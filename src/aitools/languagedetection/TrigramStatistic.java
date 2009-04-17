package aitools.languagedetection;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author bege5932
 * 
 */
public class TrigramStatistic {

	/**
	 * The amount of trigrams to be used.
	 */
	private static final int threshold = 2500;

	/**
	 * The unicode value which presents the upper border of Latin characters.
	 */
	private static final char latinCharThreashold = 0x024f;

	/**
	 * Create a map of a fixed number of the most frequent trigrams from a given
	 * string. At the same step we check if the amount of none Latin characters
	 * is higher then the amount of Latin characters. If this is the case, all
	 * Latin characters are removed. This is done due to the fact that English
	 * words and sentences can be found in texts of any language. In languages
	 * like Chinese, though, the 'wrong' characters badly disturb the overall
	 * frequencies, because the Chinese language has far more characters and so
	 * English trigrams tend to have a high probability even if the English part
	 * of the text is by far smaller than the Chinese one.
	 * 
	 * @param text
	 * @return
	 */
	public static LinkedHashMap<String, Double> getTrigrams(String text) {
		LinkedHashMap<String, Double> trigrams = new LinkedHashMap<String, Double>();
		int latinChar = 0;
		int nonLatinChar = 0;

		String trigram;
		for (int i = 0; i < text.length() - 3; ++i) {
			trigram = text.substring(i, i + 3);
			Double d = trigrams.get(trigram);
			if (d == null) {
				trigrams.put(trigram, 1.);
			} else {
				trigrams.put(trigram, d + 1.);
			}
			for (int j = 0; j < 3; ++j) {
				if (trigram.charAt(j) < latinCharThreashold) {
					++latinChar;
				} else {
					++nonLatinChar;
				}
			}
		}
		if (nonLatinChar > latinChar) {
			trigrams = removeLatinChar(trigrams);
		}

		trigrams = trim(invert(trigrams));
		trigrams = normalize(trigrams);

		return trigrams;
	}

	/**
	 * Removes all Latin characters; That is: characters below unicode 0x024f.
	 * 
	 * @param trigrams
	 * @return
	 */
	private static LinkedHashMap<String, Double> removeLatinChar(
			LinkedHashMap<String, Double> trigrams) {
		LinkedHashMap<String, Double> newMap = new LinkedHashMap<String, Double>();
		for (String trigram : trigrams.keySet()) {
			boolean add = true;
			for (int j = 0; j < 3; ++j) {
				if (trigram.charAt(j) < latinCharThreashold) {
					add = false;
				}
			}
			if (add) {
				newMap.put(trigram, trigrams.get(trigram));
			}
		}
		return newMap;
	}

	/**
	 * Normalize the 'vector' of trigrams.
	 * 
	 * @param trigrams
	 * @return
	 */
	private static LinkedHashMap<String, Double> normalize(
			LinkedHashMap<String, Double> trigrams) {
		LinkedHashMap<String, Double> newMap = new LinkedHashMap<String, Double>();
		double norm = 0;
		for (Double d : trigrams.values()) {
			norm += d * d;
		}

		norm = Math.sqrt(norm);

		for (String trigram : trigrams.keySet()) {
			newMap.put(trigram, trigrams.get(trigram) / norm);
		}
		return newMap;

	}

	/**
	 * Invert the map of trigrams and frequencies in order to sort the trigrams
	 * by frequency.
	 * 
	 * @param trigramToFrequency
	 * @return
	 */
	private static LinkedHashMap<Double, List<String>> invert(
			LinkedHashMap<String, Double> trigramToFrequency) {
		LinkedHashMap<Double, List<String>> frequencyToTrigram = new LinkedHashMap<Double, List<String>>();

		for (String trigram : trigramToFrequency.keySet()) {
			Double frequency = trigramToFrequency.get(trigram);
			List<String> trigramList = frequencyToTrigram.get(frequency);
			if (trigramList == null) {
				trigramList = new LinkedList<String>();
				frequencyToTrigram.put(frequency, trigramList);
			}
			trigramList.add(trigram);
		}

		return frequencyToTrigram;
	}

	/**
	 * Sort the trigrams by frequency and keep only the fixed number of the most
	 * frequent trigrams.
	 * 
	 * @param frequencyToTrigram
	 * @return
	 */
	private static LinkedHashMap<String, Double> trim(
			LinkedHashMap<Double, List<String>> frequencyToTrigram) {
		LinkedList<Double> sortedFrequencies = new LinkedList<Double>(
				frequencyToTrigram.keySet());
		Collections.sort(sortedFrequencies, new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				return -1 * Double.compare(o1, o2);
			}

		});

		LinkedHashMap<String, Double> trigramToFrequency = new LinkedHashMap<String, Double>();
		int count = 0;
		for (Double frequency : sortedFrequencies) {

			for (String trigram : frequencyToTrigram.get(frequency)) {
				if (++count > threshold) {
					break;
				}
				trigramToFrequency.put(trigram, frequency);
			}

		}
		return trigramToFrequency;
	}
}
