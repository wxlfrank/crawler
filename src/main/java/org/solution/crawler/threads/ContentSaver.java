package org.solution.crawler.threads;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.solution.crawler.log.LogService;
import org.solution.crawler.persistent.FileService;

/**
 * The parent thread to save contents
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 *
 */
public class ContentSaver extends ParentThread {

	BlockingQueue<String[]> content = null;

	public ContentSaver(BlockingQueue<String[]> forSavor) {
		this.content = forSavor;
		this.setName("Content Saver");
	}

	/**
	 * 1. take url and content from the queue 2. save the content with a child
	 * thread
	 */
	public void run() {
		threadMessage("started");
		String[] url_content = null;
		while (true) {
			url_content = takeFromQueueUntilSuccess(content);
			if (url_content.length == 0) {
				threadMessage("is going to stop!");
				waitChildrenFinish();
				break;
			} else {
				waitForLessChildren();
				Thread child = new ContentSaveThread(url_content, unfinished);
				child.start();
			}
		}
		threadMessage("stopped");
	}

	/**
	 * The child thread to save content as local file
	 * 
	 * @author wxlfr_000
	 *
	 */
	private static class ContentSaveThread extends Thread {

		String[] content;
		List<Thread> unfinished;

		public ContentSaveThread(String[] content, List<Thread> unfinished) {
			this.content = content;
			this.unfinished = unfinished;
			this.unfinished.add(ContentSaveThread.this);
		}

		public void run() {
			try{
				FileService.save(content[0], content[1]);
			}catch(Exception e){
				LogService.logException(e);
			}
			finally{
				synchronized (unfinished) {
					unfinished.remove(ContentSaveThread.this);
					unfinished.notifyAll();
				}
			}
		}

	}
}
