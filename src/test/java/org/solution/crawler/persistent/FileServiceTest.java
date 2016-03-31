package org.solution.crawler.persistent;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import junit.framework.Assert;
import junit.framework.TestCase;

public class FileServiceTest extends TestCase {

	public void testGetFilePathFromURL() {
		try {
			String file_separator = System.getProperty("file.separator");
			Path path = FileService.getFilePathFromURL(new URL("https://www.telenor.no"));
			Assert.assertTrue(path.endsWith("www.telenor.no" + file_separator + "DEFAULT.html"));
			path = FileService.getFilePathFromURL(new URL("https://www.telenor.no/"));
			Assert.assertTrue(path.endsWith("www.telenor.no" + file_separator + "DEFAULT.html"));
			path = FileService.getFilePathFromURL(new URL("https://www.telenor.no/abc"));
			Assert.assertTrue(
					path.endsWith("www.telenor.no" + file_separator + "abc" + file_separator + "DEFAULT.html"));
			path = FileService.getFilePathFromURL(new URL("https://www.telenor.no/abc/"));
			Assert.assertTrue(
					path.endsWith("www.telenor.no" + file_separator + "abc" + file_separator + "DEFAULT.html"));
			path = FileService.getFilePathFromURL(new URL("https://www.telenor.no/abc/c.html"));
			Assert.assertTrue(path.endsWith("www.telenor.no" + file_separator + "abc" + file_separator + "c.html"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public void testURLEncoding() throws UnsupportedEncodingException{
		String fileName = "*";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "\\";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "/";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = ":";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "?";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "\"";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "<";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = ">";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
		fileName = "|";
		fileName = FileService.encode(fileName);
		Assert.assertTrue(fileName.matches("[^\\/:?\"<>|]+"));
	}

}
