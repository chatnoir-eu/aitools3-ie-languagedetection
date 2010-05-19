// Copyright (C) 2009 webis.de. All rights reserved.
package de.aitools.ie.languagedetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author fabian.loose@uni-weimar.de
 * @author martin.potthast@uni-weimar.de
 */
public class TrigramStatistic {

	/** The maximum number of trigrams to be considered. */
	private static final int MAX_NUM_TRIGRAMS = 2500;

	/** The unicode upper bound for Latin characters. */
	private static final char MAX_LATIN_CHAR_CODE = 0x024f;

	/**
	 * Creates a map containing a fixed number of the most frequent trigrams in
	 * a given string. At the same time it is checked whether the amount of 
	 * non-Latin characters is higher then the amount of Latin characters. In 
	 * this case all Latin character trigrams are removed. This is necessary
	 * because English words and sentences can be found in texts of all
	 * languages, and, since the alphabet of Eastern languages like Chinese is
	 * larger than the Western alphabet, English trigrams may have a high
	 * frequencies even if there is only little English text to be found.
	 */
	public static Map<String, Double> getTrigrams(String s) {
		Map<String, Double> trigrams = new HashMap<String, Double>();
		int latinChar = 0;
		int nonLatinChar = 0;
		String trigram;
		for (int i = 0; i < s.length() - 3; ++i) {
			trigram = new String(s.substring(i, i + 3));
			Double d = trigrams.get(trigram);
			if (d == null) { d = new Double(0); }
			trigrams.put(trigram, d + 1);
			for (int j = 0; j < trigram.length(); ++j) {
				if (trigram.charAt(j) < MAX_LATIN_CHAR_CODE) { ++latinChar; }
				else { ++nonLatinChar; }
			}
		}
		if (nonLatinChar > latinChar) { removeLatin(trigrams); }
		trigrams = trim(trigrams);
		normalize(trigrams);
		return trigrams;
	}

	/** Removes all trigrams that contain at least one Latin character. */
	private static void removeLatin(Map<String, Double> trigrams) {
		List<String> keys = new ArrayList<String>(trigrams.keySet());
		for (String trigram : keys) {
			boolean retain = true;
			for (int j = 0; j < trigram.length(); ++j) {
				if (trigram.charAt(j) < MAX_LATIN_CHAR_CODE) { retain = false; }
			}
			if (!retain) { trigrams.remove(trigram); }
		}
	}

	/** Retains the MAX_NUM_TRIGRAMS most frequent trigrams. */
	private static Map<String, Double> trim(Map<String, Double> trigrams) {
		Map<Double, List<String>> frequencyTrigramsMap = invert(trigrams);
		List<Double> frequencies = 
			new ArrayList<Double>(frequencyTrigramsMap.keySet());
		Collections.sort(frequencies, Collections.reverseOrder());
		Map<String, Double> newTrigrams = new HashMap<String, Double>();
		int count = 0;
		for (Double frequency : frequencies) {
			for (String trigram : frequencyTrigramsMap.get(frequency)) {
				if (++count > MAX_NUM_TRIGRAMS) { break; }
				newTrigrams.put(trigram, frequency);
			}
		}
		return newTrigrams;
	}
	
	/** Inverts the mapping of trigrams to frequencies. */
	private static Map<Double, List<String>> invert(
		Map<String, Double> trigramFrequencyMap
	) {
		Map<Double, List<String>> frequencyTrigramsMap = 
			new HashMap<Double, List<String>>();
		for (String trigram : trigramFrequencyMap.keySet()) {
			Double frequency = trigramFrequencyMap.get(trigram);
			List<String> trigramList = frequencyTrigramsMap.get(frequency);
			if (trigramList == null) {
				trigramList = new LinkedList<String>();
				frequencyTrigramsMap.put(frequency, trigramList);
			}
			trigramList.add(trigram);
		}
		return frequencyTrigramsMap;
	}
	
	/** Normalizes the vector of trigrams. */
	private static void normalize(Map<String, Double> trigrams) {
		double norm = 0;
		for (Double d : trigrams.values()) { norm += d * d; }
		norm = Math.sqrt(norm);
		List<String> keys = new ArrayList<String>(trigrams.keySet());
		for (String trigram : keys) {
			trigrams.put(trigram, trigrams.get(trigram) / norm);
		}
	}
}
