package org.solution.crawler.threads;

import java.util.concurrent.BlockingQueue;

import org.solution.crawler.URLStore;

/**
 * The thread to fetch urls from URLStore
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 */
public class URLFetcher extends AbstractThread {

	BlockingQueue<String> urlQueue;
	URLStore store;
	ParentThread[] dependents = null;

	public URLFetcher(URLStore h, BlockingQueue<String> q, ParentThread[] threads) {
		store = h;
		urlQueue = q;
		dependents = threads;
		this.setName("URL Fetcher");
	}

	public void run() {

		threadMessage("started");
		String url = "";
		while (true) {
			/*
			 * Get a next unvisited url from url store.
			 */
			do {
				url = store.getNextURL();
//				System.out.println(url);
				if (!url.isEmpty())
					break;
				synchronized (urlQueue) {
					try {
						/**
						 * If the next unvisited url is empty, two situations
						 * should be considered: 1. If all the other dependent
						 * threads are waiting, it means that no unvisited url
						 * can be found. In this case, the crawling can be
						 * stopped. 2. Otherwise, it means that other threads
						 * MAYBE find unvisited urls. In this case, the url
						 * fetcher thread should wait some time. Note that, we
						 * cannot wait infinitely. The reason is that other
						 * threads MAYBE not find any unvisited url. If this
						 * happens when we wait infinitely, the thread may not
						 * stop.
						 */
						if (urlQueue.isEmpty() && allDependentThreadWaiting(dependents)) {
							threadMessage("End of crawler");
							break;
						} else
							urlQueue.wait(WAIT_TIME);
					} catch (InterruptedException e) {
						interrupted = true;
						System.out.println("interrupted");
						break;
					}
				}
			} while (true);

			monitor();

			/**
			 * put fetched url into urlQueue for further processing.
			 */
			putToQueueUntilSuccess(urlQueue, url);

			/**
			 * If the thread is interrupted, we clear the url queue and put an
			 * empty string into url queue. The empty string in url queue is a
			 * signal to end the crawling
			 */
			if (interrupted) {
				threadMessage("is going to stop!");
				synchronized (urlQueue) {
					for (String left : urlQueue) {
						if (!left.isEmpty())
							store.setUnvisited(left);
					}
					urlQueue.clear();
				}
				url = "";
				putToQueueUntilSuccess(urlQueue, url);
			}
			if (url.isEmpty()) {
				break;
			}
		}
		threadMessage("stopped");
	}

	/**
	 * Return whether all dependent threads (content fetching, url extracting)
	 * are waiting, i.e., they are waiting and their children threads are also
	 * waiting
	 * 
	 * @param dependent
	 *            threads
	 * @return true if all dependent threads are waiting, false otherwise
	 */
	private boolean allDependentThreadWaiting(ParentThread[] threads) {
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getState() != Thread.State.WAITING || threads[i].getChildren().size() != 0) {
				return false;
			}
		}
		return true;
	}
}
