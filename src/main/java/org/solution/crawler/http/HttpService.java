package org.solution.crawler.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.solution.crawler.URLStore;
import org.solution.crawler.log.LogService;

public class HttpService {
	/**
	 * content downloaders for different types. Now, we only considered text downloader.
	 */
	private static Map<String, Class<?>> downloaders = new HashMap<String, Class<?>>();

	static {
		downloaders.put("text", TextDownloader.class);
	}

	public static void registerDownloader(String type, Class<?> downloader) {
		downloaders.put(type, downloader);
	}
	
	public static Downloader getDownloader(String type){
		Class<?> clazz = downloaders.get(type);
		if(clazz == null) return null;
		try {
			return (Downloader) clazz.newInstance();
		} catch (InstantiationException e) {
			LogService.logException(e);
		} catch (IllegalAccessException e) {
			LogService.logException(e);
		}
		return null;
	}

	/**
	 * Given a url, fetch the web content Right now, we only collect web content
	 * which is text based.
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Object fetchContent(String url) {
		HttpURLConnection connection = null;
		Set<String> urls = new HashSet<String>();
		Downloader downloader = null;
		int retry = 0;
		String errorMessage = String.format("Error happens when downing content for %s!%n", url);
		while (true) {
			try{
				URLConnection con = new URL(url).openConnection();
				if(con instanceof HttpURLConnection){
					connection = (HttpURLConnection) con;
				}else
					return null;
			}catch(MalformedURLException e){
				LogService.logException(e);
				return null;
			}
			catch (SocketTimeoutException e){
				/*
				 * If a time out happens during creating connection, we will try to create connection at most 5 times
				 */
				LogService.logException(errorMessage, e);
				retry++;
				connection.disconnect();
				if(retry < 5){
					continue;
				}else{
					return null;
				}
			}catch (IOException e) {
				LogService.logException(errorMessage, e);
				return null;
			}
			try {
				connection.addRequestProperty("User-Agent", 
						"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
				/*
				 * get the downloader for a given content type Right now, we
				 * only consider text downloader
				 */
				String contentType = getType(connection.getContentType());
				if (downloader == null) {
					downloader = getDownloader(contentType);
				}
				if (downloader == null){
					connection.disconnect();
					return null;
				}
				int code = connection.getResponseCode();
				/*
				 * we follow redirection
				 */
				if (code > 299 && code < 400) {
					/*
					 * If cycle redirection occurs, return null;
					 */
					if (urls.contains(url)) {
						connection.disconnect();
						return null;
					} else {
						urls.add(url);
						url = connection.getHeaderField("Location");
						connection.disconnect();
						continue;
					}
				} else if (code > 399){
					/*
					 * If client error or server error happens, return null;
					 */
					connection.disconnect();
					return null;
				}
				
				/*
				 * Download content for a url
				 */
				Object obj =  downloader.download(connection.getInputStream());
				connection.disconnect();
				return obj;
			}catch (SocketException e) {
				LogService.logException(errorMessage, e);
				if(connection != null){
					connection.disconnect();
				}
				retry++;
				if(retry < 5){
					continue;
				}else{
					return null;
				}
			}catch (IOException e) {
				if(connection != null){
					connection.disconnect();
				}
				LogService.logException(errorMessage, e);
				return null;
			}
		}
	}

	/**
	 * Return type from connection type null => text text/* => text; others => null
	 */
	private static String getType(String type) {
		if (type != null) {
			int index = type.indexOf('/');
			if (index == -1)
				type = null;
			else
				type = type.substring(0, index);
		}
		if (type == null)
			return "text";
		return type;
	}

	/**
	 * extract urls from content using Jsoup
	 * 
	 * @param content
	 * @return
	 */
	public static Set<String> extractUrls(String content) {
		Document doc = Jsoup.parse(content);
		Set<String> urls = new HashSet<String>();
		for(Element iter : doc.select("a")){
			String str = iter.attr("href").trim();
			if (!str.isEmpty()) {
				urls.add(str);
			}
		}
		return urls;
	}

	/**
	 * Add the extracted urls into url store
	 * 
	 * @param store
	 * @param base
	 * @param urls
	 */
	public static void addExtractedUrls(URLStore store, String base, Set<String> urls) {
		for (String iter : urls) {
			store.addUrl(base, iter);
		}
	}

}
