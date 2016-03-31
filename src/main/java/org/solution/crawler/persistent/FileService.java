package org.solution.crawler.persistent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.solution.crawler.log.LogService;

public class FileService {

	private static Path location = Paths.get(System.getProperty("user.dir"), "data");
	public static final String DEAULT_FILE_NAME = "DEFAULT.html";
	// public static void setLocation(String newLocation) {
	// location = Paths.get(newLocation);
	// }

	/**
	 * Save fetched web content as local file
	 * @param url_string
	 * @param content
	 */
	public static void save(String url_string, Object content) {
		try {
			URL url = new URL(url_string);
			if (content instanceof String) {
				Path path = getFilePathFromURL(url);
				Files.createDirectories(path.getParent());
				FileWriter writer = new FileWriter(path.toFile());
				writer.write((String) content);
				writer.close();
			}
		} catch (MalformedURLException e) {
			LogService.logException(e);
		} catch (IOException e) {
			LogService.logException(e);
		}
	}

	/**
	 * encode characters in url path which are not valid in file path
	 * @param url_path
	 * @return
	 */
	public static String encode(String url_path){
		try {
			url_path = URLEncoder.encode(url_path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LogService.logException(e);
		}
		url_path = url_path.replaceAll("\\*", "%2a");
		return url_path;
	}
	
	/**
	 * Return local file path from url
	 * @param url
	 * @return
	 */
	public static Path getFilePathFromURL(URL url) {
		Path path = location.resolve(url.getHost());
		
		/*
		 * transform url path by removing leading slash and ending slash, 
		 * and encode characters in url path which are not valid in file path
		 */
		String url_path = url.getPath();
		if (url_path.startsWith("/"))
			url_path = url_path.substring(1);
		if (url_path.endsWith("/"))
			url_path = url_path.substring(0, url_path.length() - 1);
		if (!url_path.isEmpty()) {
			String[] parts = url_path.split("/");
			for (int i = 0; i < parts.length; i++) {
				parts[i] = encode(parts[i]);
			}
			url_path = String.join("/", parts);
			path = path.resolve(url_path);
		} else
			path = path.resolve(DEAULT_FILE_NAME);
		
		/*
		 * Compose filename by adding filename and encoded query
		 * truncate the filename if it is too long (> 255)
		 */
		String fileName = path.getFileName().toString();
		if (fileName.indexOf('.') != -1) {
			path = path.getParent();
		} else
			fileName = DEAULT_FILE_NAME;
		String query = url.getQuery();
		if (query != null && !query.isEmpty()) {
			fileName += encode("?" + query);
		}
		if (fileName.length() > 255)
			fileName = fileName.substring(0, 255);
		path = path.resolve(fileName);
		return path;
	}
}
