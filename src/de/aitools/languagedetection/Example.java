package de.aitools.languagedetection;

import java.util.Locale;

/**
 * This class simply demonstrates the usage of the language detection package.
 * 
 * @author bege5932
 * 
 */
public class Example {

	public static void main(String[] args) {

		LanguageDetection languageDetection = new LanguageDetection();
		String text = "Die abzugebenden Aufgaben sowie der Abgabetermin sind im Übungsblatt verzeichnet.";
		Locale locale = languageDetection.getLanguage(text);
		String language = locale.getLanguage();
		System.out.println("Detected language: " + language);

	}

}
