// Copyright (C) 2009 webis.de. All rights reserved.
package de.aitools.languagedetection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class is the main interface to the language detection package.
 * 
 * TODO(loose): The interface (getLanguage() method) could as well be static?
 * But in future one could want to get more information about for example the
 * second most probable language or the distance from the most probable language
 * to the next one...
 * 
 * TODO(loose): fix models: pl,lt -- these (and probably a few other) models are
 * the best guess when the text contains many white spaces, special character
 * etc. ... so the language (wiki) corpus still seems to have problems. -- good
 * test with vertical search results, as these texts somehow randomly come from
 * the web
 * 
 * @author fabian.loose@uni-weimar.de
 * @author martin.potthast@uni-weimar.de
 */
public class LanguageDetector {

	/**
	 * Inverted index which maps a character trigram onto a list of occurence 
	 * frequencies in all supported languages, i.e.,
	 * ([trigram] => [language1|frequency1], ..., [languageN|frequencyN]).
	 */
	private static Map<String, Map<Locale, Double>> languageModelIndex = null;

	/**
	 * In order not to build the language model index each time when the
	 * language detector is used, the index is stored on the hard disk as
	 * serialized object. If you create the JAR library using the Ant build
	 * file, the index will be refreshed.
	 */
	private static final String PACKAGE_PATH;
	private static final String SERIALIZATION_NAME = "language-model-index.obj";
	static {
		String thisPackage = LanguageDetector.class.getPackage().getName();
		PACKAGE_PATH = "/" + thisPackage.replace('.', '/');
		String resource = PACKAGE_PATH + "/" + SERIALIZATION_NAME;
		InputStream is = LanguageModel.class.getResourceAsStream(resource);
		if (is != null) { read(is); }
		else {
			buildLanguageModelIndex();
			write();
		}
	}
	
	/** Builds the language model index from the model files. */
	private static void buildLanguageModelIndex() {
		System.out.println("Creating language models ...");
		System.out.println("This needs to be done only once.\n");
		languageModelIndex = new HashMap<String, Map<Locale, Double>>();
		int count = 0;
		for (File modelFile : LanguageModel.modelDir.listFiles()) {
			String name = modelFile.getName();
			if (!name.endsWith("model")) { continue; }
			Locale language = new Locale(name.substring(0, 2));
			System.out.print("Creating " + language + " model ... ");
			LanguageModel model = LanguageModel.read(language);
			Map<String, Double> trigramIndex = model.getTrigrams(); 
			for (String trigram : trigramIndex.keySet()) {
				Map<Locale, Double> postlist = languageModelIndex.get(trigram);
				if (postlist == null) {
					postlist = new HashMap<Locale, Double>();
				}
				Double value = trigramIndex.get(trigram);
				if (postlist.containsKey(language)) {
					value *= postlist.get(language);  // This is never called!
				}
				postlist.put(language, value);
				languageModelIndex.put(trigram, postlist);
			}
			++count;
			System.out.println("done.");
		}
		System.out.println("+++ " + count + " language models created.");
	}
	
	/** Reads a language model index object from an input stream. */
	@SuppressWarnings({ "unchecked" })
	private static void read(InputStream is) {
		if(is == null) { throw new NullPointerException(); }
		ObjectInputStream objIn = null;
		try {
			objIn = new ObjectInputStream(new BufferedInputStream(is));
			languageModelIndex = 
				(Map<String, Map<Locale, Double>>)objIn.readObject();
		}
		catch (IOException e) { e.printStackTrace(); }
		catch (ClassNotFoundException e) { e.printStackTrace(); }
		finally {
			if(languageModelIndex == null) { throw new RuntimeException(); }
		}
		try { objIn.close(); }
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	/** Writes the language model index to a within this package hierarchy. */
	private static void write() {
		File directory = LanguageModel.modelDir.getParentFile();
		if(!directory.exists()) { return; }
		File objFile = new File(directory, SERIALIZATION_NAME);
		try {
			OutputStream out = new FileOutputStream(objFile);
			out = new BufferedOutputStream(out);
			ObjectOutputStream objOut = new ObjectOutputStream(out);
			objOut.writeObject(languageModelIndex);
			objOut.close();
			// The serialized object is saved copied to the bin directory since
			// only one of the two locations could cause unexpected behavior.
			copyFile(objFile, new File(
				objFile.getAbsolutePath().replace("src", "bin")
			));
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	/** Copies a file. */
	private static void copyFile(File from, File to) throws IOException {
		if (from == null || to == null) { throw new NullPointerException(); }
		if (from.equals(to)) { return; }
		FileInputStream fis = new FileInputStream(from);
		FileOutputStream fos = new FileOutputStream(to);
		byte[] buf = new byte[1024];
		int i = 0;
		while ((i = fis.read(buf)) != -1) { fos.write(buf, 0, i); }
		fis.close();
		fos.close();
	}

	/** Detects the language of a string based on its character trigrams. */
	public Locale detect(String s) {
		if(s == null) { throw new NullPointerException(); }
		Map<String, Double> trigrams = TrigramStatistic.getTrigrams(s);
		Map<Locale, Double> result = new HashMap<Locale, Double>();
		for (String trigram : trigrams.keySet()) {
			Map<Locale, Double> postlist = languageModelIndex.get(trigram);
			if (postlist == null) { continue; }
			for (Locale language : postlist.keySet()) {
				double product = postlist.get(language) * trigrams.get(trigram);
				Double d = result.get(language);
				if (d == null) { d = new Double(0); }
				result.put(language, product + d);
			}
		}

		double maxScalarProduct = 0;
		Locale detected = Locale.ENGLISH;
		for (Locale language : result.keySet()) {
			double d = result.get(language);
			if (d > maxScalarProduct) {
				maxScalarProduct = d;
				detected = language;
			}
		}
		return detected;
	}

	/** Required so that the language model index can be initialized by Ant. */
	public static void main(String[] args) {}
}
