// Copyright (C) 2009 webis.de. All rights reserved.
package de.aitools.ie.languagedetection;

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

  private static String[] MODELS = {
    "ar", "bg", "ca", "cs", "da", "de", "el", "en", "eo", "es", "fa", "fi",
    "fr", "hr", "hu", "in", "it", "iw", "ja", "ko", "lt", "nl", "no", "pt",
    "ro", "ru", "sk", "sl", "sr", "th", "tr", "uk", "vi", "zh"
  };

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
	public static final String SERIALIZATION_NAME = "language-model-index.obj";
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
    if (LanguageModel.modelDir != null && LanguageModel.modelDir.exists())
    {
  		for (File modelFile : LanguageModel.modelDir.listFiles()) {
  			String name = modelFile.getName();
  			if (!name.endsWith("model")) { continue; }
  			Locale language = new Locale(name.substring(0, 2));
  			addToLanguageModelIndex(language);
  	    ++count;
  		}
    }
    else
    {
      for (String languageTag : MODELS)
      {
        Locale language = new Locale(languageTag);
        addToLanguageModelIndex(language);
        ++count;
      }
    }
		System.out.println("+++ " + count + " language models created.");
	}

  private static void addToLanguageModelIndex(Locale language) {
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
        value *= postlist.get(language);  // This is never reached!
      }
      postlist.put(language, value);
      languageModelIndex.put(trigram, postlist);
    }
    System.out.println("done.");
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
	  if (LanguageModel.modelDir != null && LanguageModel.modelDir.exists())
	  {
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
		return setDefaultCountry(detected);
	}
	
	/**
	 * The language detection does not support {@link Locale#getCountry()}.
	 * However, other components need that information, so this function is a
	 * workaround to set a default country for given language. (trenkman)
	 */
	private static final Locale setDefaultCountry(Locale locale) {
		// Set most common countries, e.g. "de" -> "de_DE" NOT "de_CH"
		// These mappings are from Locale.getAvailableLocales()
		if (locale.getLanguage().equals("de")) return new Locale("de", "DE");
		if (locale.getLanguage().equals("en")) return new Locale("en", "US");
		if (locale.getLanguage().equals("fr")) return new Locale("fr", "FR");
		if (locale.getLanguage().equals("it")) return new Locale("it", "IT");
		if (locale.getLanguage().equals("es")) return new Locale("es", "ES");
		if (locale.getLanguage().equals("pl")) return new Locale("pl", "PL");
		if (locale.getLanguage().equals("lt")) return new Locale("lt", "LT");
		if (locale.getLanguage().equals("mk")) return new Locale("mk", "MK");
		if (locale.getLanguage().equals("pt")) return new Locale("pt", "PT");
		if (locale.getLanguage().equals("no")) return new Locale("no", "NO");
		if (locale.getLanguage().equals("ar")) return new Locale("ar", "AE");
		if (locale.getLanguage().equals("fi")) return new Locale("fi", "FI");
		if (locale.getLanguage().equals("sk")) return new Locale("sk", "SK");
		if (locale.getLanguage().equals("tr")) return new Locale("tr", "TR");
		if (locale.getLanguage().equals("lv")) return new Locale("lv", "LV");
		if (locale.getLanguage().equals("nl")) return new Locale("nl", "NL");
		if (locale.getLanguage().equals("is")) return new Locale("is", "IS");
		if (locale.getLanguage().equals("th")) return new Locale("th", "TH");
		if (locale.getLanguage().equals("hu")) return new Locale("hu", "HU");
		if (locale.getLanguage().equals("ru")) return new Locale("ru", "RU");
		if (locale.getLanguage().equals("bg")) return new Locale("bg", "BG");
		if (locale.getLanguage().equals("mt")) return new Locale("mt", "MT");
		if (locale.getLanguage().equals("ro")) return new Locale("ro", "RO");
		if (locale.getLanguage().equals("hr")) return new Locale("hr", "HR");
		if (locale.getLanguage().equals("jp")) return new Locale("jp", "JP");
		return locale;
	}

	/** Required so that the language model index can be initialized by Ant. */
	public static void main(String[] args) {}
}
