// Copyright (C) 2009 webis.de. All rights reserved.
package de.aitools.ie.languagedetection;

import java.util.Locale;

/**
 * This class demonstrates the usage of the language detection package.
 * 
 * @author fabian.loose@uni-weimar.de
 * @author martin.potthast@uni-weimar.de
 */
public class Example {
	public static void main(String[] args) {
		LanguageDetector detector = new LanguageDetector();
		String text = 
			"Die abzugebenden Aufgaben sowie der Abgabetermin sind im " +
			"Übungsblatt verzeichnet.";
		Locale language = detector.detect(text);
		System.out.println("Detected language: " + language.getLanguage());
	}
}
