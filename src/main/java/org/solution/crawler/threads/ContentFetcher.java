package org.solution.crawler.threads;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.solution.crawler.http.HttpService;
import org.solution.crawler.log.LogService;

/**
 * The parent thread to fetch contents for urls
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 */

public class ContentFetcher extends ParentThread {

	BlockingQueue<String> urlQueue;
	BlockingQueue<String[]> forSavaer;
	BlockingQueue<String[]> forExtractor;

	public ContentFetcher(BlockingQueue<String> urls, BlockingQueue<String[]> forExtractor,
			BlockingQueue<String[]> forSaver) {
		this.urlQueue = urls;
		this.forSavaer = forSaver;
		this.forExtractor = forExtractor;
		this.setName("Content Fetcher");
	}

	/**
	 * 1. take a url from url queue 2. fetch content for the url by using of a
	 * child thread 3. put the url and the content to the queues for saving as
	 * local files and for extracting urls from the content
	 */
	public void run() {
		threadMessage("started");
		String url = null;
		String[] url_content;
		while (true) {
			monitor();

			url = takeFromQueueUntilSuccess(urlQueue);

			if (url.isEmpty()) {
				url_content = new String[] {};
				/**
				 * If the url feteched from url queue is empty, finish the
				 * thread
				 */

				threadMessage("is going to stop!");

				waitChildrenFinish();
				putToQueueUntilSuccess(forSavaer, url_content);
				putToQueueUntilSuccess(forExtractor, url_content);
			} else {				
				waitForLessChildren();
				Thread child = new ContentFetchThread(url, forExtractor, forSavaer, unfinished);
				child.start();
			}

			if (url.isEmpty())
				break;
		}
		threadMessage("stopped");
	}

	/**
	 * The child thread to fetch content for a given url
	 * 
	 * @author wxlfr_000
	 *
	 */
	private static class ContentFetchThread extends AbstractThread {

		String url;
		BlockingQueue<String[]> forExtractor, forSavor;
		List<Thread> unfinished;

		public ContentFetchThread(String url, BlockingQueue<String[]> forExtractor, BlockingQueue<String[]> forSavor,
				List<Thread> unfinished) {
			this.url = url;
			this.forExtractor = forExtractor;
			this.forSavor = forSavor;
			this.unfinished = unfinished;
			this.unfinished.add(ContentFetchThread.this);
		}

		public void run() {
			try{
				Object obj = HttpService.fetchContent(url);
				if (obj instanceof String) {
					String[] url_content = new String[] { url, (String) obj };
					putToQueueUntilSuccess(forSavor, url_content);
					putToQueueUntilSuccess(forExtractor, url_content);
				}
			}catch(Exception e){
				LogService.logException(e);
			}
			finally{
				synchronized (unfinished) {
					unfinished.remove(ContentFetchThread.this);
					unfinished.notifyAll();
				}
			}
		}

	}
}
