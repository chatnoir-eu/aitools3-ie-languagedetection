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
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * This class provides the main interface to the language detection package.
 * 
 * TODO: The interface (getLanguage() method) could as well be static? But in
 * future one could want to get more information about for example the second
 * most probable language or the distance from the most probable language to the
 * next one...
 * 
 * TODO: fabian loose: fix polish model
 * 
 * @author bege5932
 * 
 */
public class LanguageDetection {

	/**
	 * An inverted index from a given trigram of letters to a list of
	 * frequencies in either language ([trigram] => [language1|frequency1],
	 * [language2|frequency2], ..., [languageN|frequencyN]).
	 */
	private static LinkedHashMap<String, LinkedHashMap<Locale, Double>> languageModelIndex;

	/**
	 * As not to build the language model index at every execution, the index is
	 * stored on the hard disk in a serialized form. If you create the library
	 * as JAR using the build.xml file, then a fresh serialization is created.
	 */
	public static final String SERIALIZATION_NAME = "LanguageModels.serialized";
	static {
		InputStream is = LanguageModel.class
				.getResourceAsStream(SERIALIZATION_NAME);
		if (is != null) {
			load(is);
		} else {
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
		LinkedHashMap<String, Double> trigrams = TrigramStatistic
				.getTrigrams(text);

		LinkedHashMap<Locale, Double> result = new LinkedHashMap<Locale, Double>();
		LinkedHashMap<Locale, Double> postlist = null;
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
	 * in the models directory.
	 */
	private static void createLanguageModelIndex() {
		System.out.println("Creating languagemodels ...");
		System.out.println("This has to be done only once.\n");
		languageModelIndex = new LinkedHashMap<String, LinkedHashMap<Locale, Double>>();
		int count = 0;
		for (String language : LanguageModel.LanguageModelDir.list()) {
			try {
				if (language.endsWith("model")) {
					System.out.print("Creating model: "
							+ language.substring(0, 2) + " ... ");
					LanguageModel model = LanguageModel.load(new Locale(
							language.substring(0, 2)));
					for (String trigram : model.getTrigramIndex().keySet()) {
						LinkedHashMap<Locale, Double> postlist = languageModelIndex
								.get(trigram);
						if (postlist == null) {
							postlist = new LinkedHashMap<Locale, Double>();
						}
						if (postlist.containsKey(model.getLocale())) {
							postlist.put(model.getLocale(), postlist.get(model
									.getLocale())
									* model.getTrigramIndex().get(trigram));
						} else {
							postlist.put(model.getLocale(), model
									.getTrigramIndex().get(trigram));
						}
						languageModelIndex.put(trigram, postlist);
					}
					++count;
					System.out.println("done.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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
				languageModelIndex = (LinkedHashMap<String, LinkedHashMap<Locale, Double>>) objIn
						.readObject();
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
			File serializazion = new File(LanguageModel.LanguageModelDir
					.getParentFile(), SERIALIZATION_NAME);
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
	 * The main function is needed to build the serialization for the jar with
	 * ant.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

	}
}
