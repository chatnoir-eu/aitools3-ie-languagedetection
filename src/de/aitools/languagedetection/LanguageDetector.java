// Copyright (C) 2009 webis.de. All rights reserved.
package de.aitools.languagedetection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class is the main interface to the language detection package.
 * 
 * TODO: The interface (getLanguage() method) could as well be static? But in
 * future one could want to get more information about for example the second
 * most probable language or the distance from the most probable language to the
 * next one...
 * 
 * TODO: fabian loose: fix models: pl,lt -- these (and probably a few other)
 * models are the best guess when the text contains many white spaces, special
 * character etc. ... so the language (wiki) corpus still seems to have
 * problems. -- good test with vertical search results, as these texts somehow
 * randomly come from the web
 * 
 * @author fabian.loose@uni-weimar.de
 */
public class LanguageDetector {

	/**
	 * Inverted index which maps a character trigram onto a list of occurence 
	 * frequencies in all supported languages, i.e.,
	 * ([trigram] => [language1|frequency1], ..., [languageN|frequencyN]).
	 */
	private static Map<String, Map<Locale, Double>> languageModelIndex;

	/**
	 * In order not to build the language model index at each time when the
	 * language detector is used, the index is stored on the hard disk as
	 * serialized object. If you create the library as JAR using the Ant build
	 * file, the index is refreshed.
	 */
	private static final String PACKAGE_PATH;
	private static final String SERIALIZATION_NAME;
	static {
		String thisPackage = LanguageDetector.class.getPackage().getName();
		PACKAGE_PATH = "/" + thisPackage.replace('.', '/');
		SERIALIZATION_NAME = "language-models.obj";
		String resource = PACKAGE_PATH + "/" + SERIALIZATION_NAME;
		InputStream is = LanguageModel.class.getResourceAsStream(resource);
		if (is != null) { load(is); }
		else {
			createLanguageModelIndex();
			write();
		}
	}

	/**
	 * Public part:
	 */

	/**
	 * Returns the language of a given string by analyzing the distribution of
	 * trigram frequencies.
	 * 
	 * @param text
	 * @return
	 */
	public Locale getLanguage(String text) {
		LinkedHashMap<String, Double> trigrams =
			TrigramStatistic.getTrigrams(text);

		Map<Locale, Double> result = new LinkedHashMap<Locale, Double>();
		Map<Locale, Double> postlist = null;
		for (String trigram : trigrams.keySet()) {
			postlist = languageModelIndex.get(trigram);
			if (postlist == null) {
				continue;
			}
			for (Locale locale : postlist.keySet()) {
				double product = postlist.get(locale) * trigrams.get(trigram);
				Double d = result.get(locale);
				if (d == null) {
					d = new Double(0.);
				}
				result.put(locale, product + d);
			}
		}

		double highestScalarproduct = 0;
		Locale bestLocale = Locale.ENGLISH;
		for (Locale locale : result.keySet()) {

			double d = result.get(locale);
			if (d > highestScalarproduct) {
				highestScalarproduct = d;
				bestLocale = locale;
			}
		}
		return bestLocale;
	}

	/**
	 * Private part:
	 */

	/**
	 * This method creates the language model index from all the *.model files
	 * in the models package.
	 */
	private static void createLanguageModelIndex() {
		System.out.println("Creating languagemodels ...");
		System.out.println("This has to be done only once.\n");
		languageModelIndex = new LinkedHashMap<String, Map<Locale, Double>>();
		int count = 0;
		for (File modelFile : LanguageModel.modelDir.listFiles()) {
			String name = modelFile.getName();
			if (!name.endsWith("model")) { continue; }
			Locale language = new Locale(name.substring(0, 2));
			System.out.print("Creating: " + language + " model ... ");
			LanguageModel model = null;
			try { model = LanguageModel.load(language); }
			catch (FileNotFoundException e) { e.printStackTrace(); }
			finally { if(model == null) { throw new RuntimeException(); } }
			Map<String, Double> trigramIndex = model.getTrigramIndex(); 
			for (String trigram : trigramIndex.keySet()) {
				Map<Locale, Double> postlist = languageModelIndex.get(trigram);
				if (postlist == null) {
					postlist = new LinkedHashMap<Locale, Double>();
				}
				Double value = postlist.get(language);
				if (value != null) { value *= trigramIndex.get(trigram); }
				postlist.put(language, value);
				languageModelIndex.put(trigram, postlist);
			}
			++count;
			System.out.println("done.");
		}
		System.out.println("+++ " + count + " languagemodels loaded.");
	}

	/**
	 * Load the serialized language model index from the given input stream.
	 * 
	 * @param is
	 */
	@SuppressWarnings("unchecked")
	private static void load(InputStream is) {
		try {
			ObjectInputStream objIn = new ObjectInputStream(
					new BufferedInputStream(is));
			try {
				languageModelIndex = (Map<String, Map<Locale, Double>>) objIn.readObject();
				objIn.close();
			} catch (ClassNotFoundException e) {
				objIn.close();
				throw new IOException(e);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Serialize the language model index.
	 */
	private static void write() {
		try {
			String directory = LanguageModel.modelDir.getParentFile().getAbsolutePath().replaceAll(PACKAGE_PATH, "");
			File serializazion = new File(directory, SERIALIZATION_NAME);
			FileOutputStream fos = new FileOutputStream(serializazion);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream objOut = new ObjectOutputStream(bos);
			objOut.writeObject(languageModelIndex);
			objOut.close();
			// Saving the serialized file in both, the src and bin directory.
			// Either single location for the serialized file could cause
			// unexpected behavior of this method, so we put it in both.
			copyFile(serializazion, new File(serializazion.getAbsolutePath()
					.replace("src", "bin")));
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Simple file copy method.
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	private static void copyFile(File in, File out) throws IOException {
		if (in.equals(out)) {
			return;
		}
		FileInputStream fis = new FileInputStream(in);
		FileOutputStream fos = new FileOutputStream(out);
		byte[] buf = new byte[1024];
		int i = 0;
		while ((i = fis.read(buf)) != -1) {
			fos.write(buf, 0, i);
		}
		fis.close();
		fos.close();
	}

	/**
	 * The main method is required to build the serialization for the jar with
	 * ant.
	 */
	public static void main(String[] args) {}
}
