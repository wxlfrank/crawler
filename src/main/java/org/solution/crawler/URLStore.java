package org.solution.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store of urls, visited or unvisited
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 */

public class URLStore {

	/**
	 * Urls are stored in a map. The keys are host names which are transformed
	 * by replacing the characters that are not alphabet, number or underline
	 * with underline, while the values are the urls. The values are divided
	 * based on whether they are visited. The keys are also stored in hosts and
	 * further divided based on whether the corresponding values contains
	 * unvisited urls.
	 */
	private Map<String, VisitHash> host2urls = new ConcurrentHashMap<String, VisitHash>();
	private VisitHash hosts = new VisitHash();
	private String currentHost = "";
	private VisitHash currentVisitHash = null;
	//	public static  Set<String> suffix = new HashSet<String>();
	public static  Set<String> FILE_SUFFIX = new HashSet<String>();

	static{
		FILE_SUFFIX.add(".html");
		FILE_SUFFIX.add(".htm");
		FILE_SUFFIX.add(".xml");
		FILE_SUFFIX.add(".pdf");
		FILE_SUFFIX.add(".txt");
		FILE_SUFFIX.add(".zip");
		FILE_SUFFIX.add(".php");
		FILE_SUFFIX.add(".aspx");
		FILE_SUFFIX.add(".asp");
	}
	/**
	 * Add a url to store
	 * 
	 * @param url
	 */
	public synchronized void addUrl(URL url) {
		String host = url.getHost();
		if(host.startsWith("www"))
			host = host.substring(host.indexOf('.') + 1);
		VisitHash urls = host2urls.get(host);
		if (urls == null) {
			host = url.getHost();
			urls = new VisitHash();
			host2urls.put(host, urls);
			hosts.addUnvisited(host);
		}
		boolean added = urls.addUnvisited(url.toString());
		if (added) {
			if (hosts.isVisited(host))
				hosts.setUnVisited(host);
			notifyAll();
		}
	}

	/**
	 * Return a next unvisited url in the store
	 * 
	 * @return the next unvisited url in the store, or an empty string if no
	 *         unvisited url is found in the store
	 */
	public synchronized String getNextURL() {
		String url = "";
		String host = "";
		while (url.isEmpty()) {
			if (currentHost.isEmpty()) {
				host = hosts.nextUnvisited();
				if (host.isEmpty())
					return "";
				currentHost = host;
				currentVisitHash = host2urls.get(host);
			}
			url = currentVisitHash.nextUnvisited();
			if (url.isEmpty()) {
				hosts.setVisited(currentHost);
				currentHost = "";
				currentVisitHash = null;
			}
		}
		currentVisitHash.setVisited(url);
		return url;
	}

	public void addUrl(String url) {
		addUrl("", url);
	}

	public void addUrl(String base, String url) {
		URL obj = toValidURL(base, url);
		if (obj instanceof URL)
			addUrl((URL) obj);
	}

	/**
	 * Set a url as unvisited
	 * @param url_string
	 */
	public void setUnvisited(String url_string) {
		try {
			URL url = new URL(url_string);
			String host = url.getHost();
			if(host.startsWith("www"))
				host = host.substring(host.indexOf('.') + 1);
			VisitHash urls = host2urls.get(host);
			urls.setUnVisited(url_string);
			if (hosts.isVisited(host))
				hosts.setUnVisited(host);
		} catch (MalformedURLException e) {
		}
	}

	/**
	 * Return a valid url from the base url and the relative url
	 * 
	 * @param base
	 *            base url. It can be empty. In this case, the relative url
	 *            should be a absolute url
	 * @param relative
	 *            relative url.
	 * @return a valid url if url is a http or https url, null otherwise
	 */
	public static URL toValidURL(String base, String relative) {
		/*
		 * Remove the fragment
		 */
		int index = relative.indexOf("#");
		if (index != -1)
			relative = relative.substring(0, index);
		/*
		 * Remove the starting slash
		 */
		boolean plausibleRelative = relative.startsWith("/");
		relative = relative.replaceFirst("^/+", "");
		/*
		 * Remove the ending slash
		 */
		if (relative.endsWith("/"))
			relative = relative.substring(0, relative.length() - 1);

		/*
		 * If relative is empty, return null
		 */
		if (relative.isEmpty())
			return null;

		URL url = null;
		/*
		 * Check if relative can be a url
		 */
		try {
			url = new URL(relative);
		} catch (MalformedURLException e) {
			/*
			 * Check if relative miss protocol part. 
			 * If so, guess the host name according to Internet standard (RFC 952)
			 * Then derive a url by adding http to relative.
			 */
			String host = relative;
			index = host.indexOf('/');
			if (index != -1)
				host = host.substring(0, index);
			if (!host.isEmpty() && host.matches("[\\w\\-]{1,63}(\\.[\\w\\-]{1,63})+") && host.length() < 254) {
				String suffix = host.substring(host.lastIndexOf('.'));
				if(index != -1 || (!FILE_SUFFIX.contains(suffix) && !plausibleRelative)){
					//URLStore.suffix.add(suffix);
					try {
						url = new URL("http://" + relative);
					} catch (MalformedURLException e1) { }
				}
			}
		}

		/*
		 * If relative cannot be a url and the base is not null and not empty, we 
		 * derive a new url from the base and the relative 
		 */
		try {
			if (url == null && (base != null && !base.isEmpty()))
				url = new URL(new URL(base), relative);
		} catch (MalformedURLException e) { }

		/*
		 * If url is still null, return null
		 */
		if (url == null)
			return null;

		/*
		 * If url is a http or https url, we standardize url by changing hostname to lowercase and return the standard url.
		 * Otherwise, return null;
		 */
		String protocol = url.getProtocol().toLowerCase();
		if (protocol.equals("http") || protocol.equals("https")) {
			String host_lowcase = url.getHost().toLowerCase();
			try {
				url = new URL(protocol, host_lowcase, url.getPort(), url.getFile());
			} catch (MalformedURLException e) {
				url = null;
			}
			return url;
		}
		return null;
	}

	public VisitHash getHosts() {
		return hosts;
	}

	public Map<String, VisitHash> getURLs() {
		return host2urls;
	}

	/**
	 * Return whether two stores are equal. Used in test
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(URLStore other) {
		boolean equal = hosts.equals(other.getHosts());
		if (!equal) {
			return false;
		}
		for (Entry<String, VisitHash> iter : host2urls.entrySet()) {
			String host = iter.getKey();
			equal = iter.getValue().equals(other.getURLs().get(host));
			if (!equal) {
				System.out.println(iter.getValue());
				System.out.println(other.getURLs().get(host));
				return false;
			}
		}
		return true;
	}

}
