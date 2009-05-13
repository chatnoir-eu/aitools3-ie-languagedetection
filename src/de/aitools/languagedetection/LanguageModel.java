package de.aitools.languagedetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * This class represents a language model as a collection of the frequencies of
 * the most frequent trigrams of a language.
 * 
 * @author bege5932
 * 
 */
public class LanguageModel implements Serializable {

	/**
	 * None static part
	 */

	private Locale locale = null;
	private Map<String, Double> trigramIndex = null;
	private boolean isNormalized = false;

	public LanguageModel(Locale locale, Map<String, Double> trigrams) {
		this.locale = locale;
		this.trigramIndex = trigrams;
	}

	public Locale getLocale() {
		return locale;
	}

	public Map<String, Double> getTrigramIndex() {
		return trigramIndex;
	}

	/**
	 * Normalize the vector of trigram frequencies.
	 */
	private void normalize() {
		if (!isNormalized) {
			double squares = 0;
			for (Double d : trigramIndex.values()) {
				squares += d * d;
			}
			squares = Math.sqrt(squares);
			for (String trigram : trigramIndex.keySet()) {
				trigramIndex.put(trigram, trigramIndex.get(trigram) / squares);
			}
			isNormalized = true;
		}
	}

	/**
	 * Static part:
	 */

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = -1451504770410028784L;

	/**
	 * This is the directory where all the .model files can be found.
	 */
	public static final File LanguageModelDir;
	static {
		URL url = LanguageModel.class.getResource("models");
		File dir = null;
		try {
			dir = new File(url.toURI().getPath().replace("/bin/", "/src/"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		LanguageModelDir = dir;
	}

	/**
	 * Load a language model from the associated .model file.
	 * 
	 * @param locale
	 * @return
	 * @throws FileNotFoundException
	 */
	public static LanguageModel load(Locale locale)
			throws FileNotFoundException {
		Map<String, Double> trigramMap = new HashMap<String, Double>();
		Scanner fileScanner = new Scanner(new File(LanguageModelDir, locale
				.getLanguage()
				+ ".model"));
		fileScanner.useDelimiter("_ENDLINE_\n");
		while (fileScanner.hasNext()) {
			Scanner lineScanner = new Scanner(fileScanner.next());
			lineScanner.useDelimiter("_DELIMITER_");
			trigramMap.put(lineScanner.next(), lineScanner.nextDouble());
		}

		LanguageModel lm = new LanguageModel(locale, trigramMap);
		lm.isNormalized = true;
		return lm;
	}

	/**
	 * Write a language model to disk.
	 * 
	 * @param languageModel
	 * @throws IOException
	 */
	public static void save(LanguageModel languageModel) throws IOException {
		File f = new File(LanguageModelDir, languageModel.locale.getLanguage()
				+ ".model");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));

		if (!languageModel.isNormalized) {
			languageModel.normalize();
		}
		for (String trigram : languageModel.trigramIndex.keySet()) {
			bw.append(trigram + "_DELIMITER_"
					+ languageModel.trigramIndex.get(trigram) + "_ENDLINE_\n");
		}
		bw.flush();
		bw.close();
	}

}
