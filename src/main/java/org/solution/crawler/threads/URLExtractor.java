package org.solution.crawler.threads;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.solution.crawler.URLStore;
import org.solution.crawler.http.HttpService;

/**
 * The parent thread to extract urls from content
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 *
 */
public class URLExtractor extends ParentThread {
	URLStore store = null;
	BlockingQueue<String[]> content = null;

	public URLExtractor(URLStore store, BlockingQueue<String[]> forExtractor) {
		this.store = store;
		this.content = forExtractor;
		this.setName("URL Extractor");
	}

	/**
	 * 1. take url and content from the queue 2. use a child thread to extract
	 * urls from a content and put the extracted urls into url store
	 */
	public void run() {
		threadMessage("started");
		String[] url_content = null;
		while (true) {
			url_content = takeFromQueueUntilSuccess(content);
			monitor();

			if (url_content.length == 0) {
				threadMessage("is going to stop!");
				waitChildrenFinish();
				break;
			} else {
				waitForLessChildren();
				Thread child = new ExtractorThread(store, url_content, unfinished);
				child.start();
			}
		}
		threadMessage("stopped!");
	}

	/**
	 * The child thread to extract urls from a given conent
	 * 
	 * @author wxlfr_000
	 *
	 */
	private static class ExtractorThread extends Thread {

		String[] content;
		List<Thread> unfinished;
		URLStore store;

		public ExtractorThread(URLStore store, String[] content, List<Thread> unfinished) {
			this.content = content;
			this.unfinished = unfinished;
			this.store = store;
			this.unfinished.add(ExtractorThread.this);
		}

		public void run() {
			HttpService.addExtractedUrls(store, content[0], HttpService.extractUrls(content[1]));
			synchronized (unfinished) {
				unfinished.remove(ExtractorThread.this);
				unfinished.notifyAll();
			}
		}

	}
}
