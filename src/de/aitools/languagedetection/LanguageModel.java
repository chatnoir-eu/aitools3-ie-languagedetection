// Copyright (C) 2009 webis.de. All rights reserved.
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
 * @author fabian.loose@uni-weimar.de
 * @author martin.potthast@uni-weimar.de
 */
public class LanguageModel implements Serializable {

	private static final long serialVersionUID = -1451504770410028784L;

	/**
	 * This is the directory where all the .model files can be found.
	 */
	static final File modelDir;
	static {
		URL url = LanguageModel.class.getResource("models");
		File dir = null;
		try { dir = new File(url.toURI().getPath().replace("/bin/", "/src/")); }
		catch (URISyntaxException e) { e.printStackTrace(); }
		finally { 
			if(dir == null || !dir.exists()) { throw new RuntimeException(); }
		}
		modelDir = dir;
	}

	/**
	 * Reads a language model from the locale's .model file.
	 */
	public static LanguageModel read(Locale language) {
		if(language == null) { throw new NullPointerException(); }
		Map<String, Double> trigramMap = new HashMap<String, Double>();
		File modelFile = new File(modelDir, language.getLanguage() + ".model");
		Scanner fileScanner = null;
		try { fileScanner = new Scanner(modelFile); }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		finally { if(fileScanner == null) { throw new RuntimeException(); } }
		fileScanner.useDelimiter("_ENDLINE_\n");
		while (fileScanner.hasNext()) {
			Scanner lineScanner = new Scanner(fileScanner.next());
			lineScanner.useDelimiter("_DELIMITER_");
			trigramMap.put(lineScanner.next(), lineScanner.nextDouble());
		}
		LanguageModel lm = new LanguageModel(language, trigramMap);
		return lm;
	}

	/**
	 * Writes a language model to the models directory.
	 */
	public static void write(LanguageModel lm) throws IOException {
		if(lm == null) { throw new NullPointerException(); }
		File modelFile = 
			new File(modelDir, lm.language.getLanguage() + ".model");
		BufferedWriter bw = new BufferedWriter(new FileWriter(modelFile));
		for (String trigram : lm.trigrams.keySet()) {
			bw.append(trigram);
			bw.append("_DELIMITER_");
			bw.append(lm.trigrams.get(trigram).toString());
			bw.append("_ENDLINE_\n");
		}
		bw.close();
	}


	private Locale language;
	private Map<String, Double> trigrams;

	public LanguageModel(Locale language, Map<String, Double> trigrams) {
		if(language == null || trigrams == null) { 
			throw new NullPointerException();
		} 
		this.language = language;
		this.trigrams = trigrams;
	}

	public Locale getLanguage() { return language; }
	public Map<String, Double> getTrigrams() { return trigrams; }
}
