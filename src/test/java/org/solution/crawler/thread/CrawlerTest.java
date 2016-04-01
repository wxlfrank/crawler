package org.solution.crawler.thread;

import java.sql.SQLException;
import java.util.Set;

import org.solution.crawler.URLStore;
import org.solution.crawler.http.HttpService;
import org.solution.crawler.persistent.FileService;
import org.solution.crawler.sql.SQLiteService;
import org.solution.crawler.threads.Crawler;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CrawlerTest extends TestCase {

	public void testCheckMultiThread() {
		Crawler crawler = new Crawler();
		URLStore store = crawler.getURLStore();
		SQLiteService.loadStore(store);
		String url = "www.telenor.no";
		if (!store.getHosts().get(0).contains(url) && !store.getHosts().get(1).contains(url))
			store.addUrl(url);
		crawler.createChildThreads();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		crawler.stopChildren();
		System.out.println("chilren threads are closed");
		SQLiteService.saveStore(store);
		URLStore new_store = new URLStore();
		SQLiteService.loadStore(new_store);
		Assert.assertTrue(new_store.equals(store));
//		for(String iter : store.getHosts().get(0)){
//			Assert.assertFalse(store.getURLs().get(iter).get(0).isEmpty());
//		}
//		for(String iter : store.getHosts().get(1)){
//			Assert.assertTrue(store.getURLs().get(iter).get(0).isEmpty());
//		}
//		System.out.println(URLStore.suffix);
	}

	public void testCheckSingleThread() throws ClassNotFoundException, SQLException {
		URLStore store = new URLStore();
		SQLiteService.loadStore(store);
		String url = "www.telenor.no";
		if (!store.getHosts().get(0).contains(url) && !store.getHosts().get(1).contains(url))
			store.addUrl(url);
		long start = System.currentTimeMillis();
		while (true) {
			url = store.getNextURL();
			if (url.isEmpty()) {
				SQLiteService.saveStore(store);
				break;
			}
			Object content = HttpService.fetchContent(url);
			if (content instanceof String) {
				FileService.save(url, content);
				Set<String> extracted = HttpService.extractUrls((String) content);
				HttpService.addExtractedUrls(store, url, extracted);
			}
			if (System.currentTimeMillis() - start > 2000) {
				SQLiteService.saveStore(store);
				URLStore new_store = new URLStore();
				SQLiteService.loadStore(new_store);
				Assert.assertTrue(new_store.equals(store));
				break;
			}
		}
	}

}
