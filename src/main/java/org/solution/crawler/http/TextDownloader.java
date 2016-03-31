package org.solution.crawler.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextDownloader implements Downloader {
	public static final String NL = System.getProperty("line.separator");

	/**
	 * download text from Internet
	 */
	public String download(InputStream input) throws IOException {
		if (input == null)
			return null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(NL);
		}
		reader.close();
		if (stringBuilder.length() == 0)
			return null;
		return stringBuilder.toString();
	}

}
