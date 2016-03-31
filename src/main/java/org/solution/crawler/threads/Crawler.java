package org.solution.crawler.threads;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.solution.crawler.URLStore;
import org.solution.crawler.log.LogService;
import org.solution.crawler.sql.SQLiteService;

/**
 * The workflow of the crawl consists of the following steps in sequence:
 * <ul>
 * <li>preconfigure simply the crawl, i.e., creating a directory named data for containing database and crawled data, if such a directory does not exist</li>
 * <li>load urls from database into {@link URLStore}</li>
 * <li>create and start four child threads which are responsible for different tasks
 * <ul>
 * <li><strong>urlFetcher</strong> : {@link URLFetcher} fetches urls from url store {@link URLStore} and puts fetched urls into a url queue</li>
 * <li><strong>contentFetcher</strong> : {@link ContentFetcher} takes urls from the url queue, fetches content for the urls and puts the fetched content into two content queues</li>
 * <li><strong>urlExtractor</strong> : {@link URLExtractor} takes contents from one content queue, extracts urls from the contents and puts extracted urls into url store {@link URLStore}</li>
 * <li><strong>contentSaver</strong> : {@link ContentSaver} takes contents from the other content queue and saves the contents as local files</li>
 * </ul>
 * <span><strong>the last three threads contain also child threads</strong><span>
 * </li>
 * <li>Register handler to handle shutdown signal. When a shut down signal is issued, the following steps are executed:
 * <ul>
 * <li>Interrupt the thread <strong>urlFetcher</strong> </li>
 * <li>When the thread <strong>urlFetcher</strong> receives interruption, it clears the url queue, puts an empty url into the url queue and stops itself</li>
 * <li>When the thread <strong>contentFetcher</strong> takes the empty url from the url queue, it puts an empty content into the two content queues.
 * Then it will stop itself after all its child threads finish</li>
 * <li>When the thread <strong>urlExtractor</strong> and <strong>contentSaver</strong> take the empty contents from the content queues, they will stop themselves after all their child threads finish</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Xiaoliang Wang
 * @version 1.0
 * @since 2016-03-23
 */
public class Crawler extends AbstractThread {

	/**
	 * fetch urls from url store
	 */
	Thread urlFetcher;

	/**
	 * fetch content for urls from internet
	 */
	ParentThread contentFetcher;

	/**
	 * extract urls from fetched content
	 */
	ParentThread urlExtractor;

	/**
	 * save fetched content on disk
	 */
	ParentThread contentSaver;

	/**
	 * all th urls, crawled or to be crawled
	 */
	URLStore store = new URLStore();

	/**
	 * urls to be crawled
	 */
	BlockingQueue<String> urls = new ArrayBlockingQueue<String>(QUEUE_SIZE);

	/**
	 * urls and their corresponding contents, used to extract urls from contents
	 */
	BlockingQueue<String[]> forExtractor = new ArrayBlockingQueue<String[]>(QUEUE_SIZE);

	/**
	 * urls and their corresponding contents, used to save content
	 */
	BlockingQueue<String[]> forSavor = new ArrayBlockingQueue<String[]>(QUEUE_SIZE);

	/**
	 * preconfigure the crawler by creating a data directory
	 */
	private boolean preconfigure() {
		try {
			Path path = Paths.get("data");
			boolean needCreate = false;
			if (Files.exists(path)) {
				if (!Files.isDirectory(path)) {
					Scanner scanner = new Scanner(System.in);
					threadMessage("A file named \"data\" exists in the current directory. ");
					threadMessage("We are going to delete it. Do you agree? (Y)/N");

					String confirm = scanner.nextLine();
					scanner.close();
					if (confirm.isEmpty() || confirm.toUpperCase().equals("Y")) {
						threadMessage("Deleting the file \"data\"");
						Files.delete(path);
						needCreate = true;
					} else {
						return false;
					}
				}
			} else
				needCreate = true;
			if (needCreate) {
				threadMessage("Creating the directory \"data\"");
				Files.createDirectories(path);
			}
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public void createChildThreads() {
		threadMessage("crawling begins");
		contentFetcher = new ContentFetcher(urls, forExtractor, forSavor);
		urlExtractor = new URLExtractor(store, forExtractor);
		contentSaver = new ContentSaver(forSavor);
		urlFetcher = new URLFetcher(store, urls, new ParentThread[] { contentFetcher, urlExtractor });
		contentFetcher.start();
		urlExtractor.start();
		contentSaver.start();
		urlFetcher.start();
	}

	/**
	 * run the crawler 1. configure the crawler 2. load urls from database 3.
	 * create the four threads 4. start the created threads 5. register thread
	 * to perform the following steps when the application shuts down a. close
	 * all the running thread b. save urls to database
	 */
	public void run() {
		threadMessage("preconfigure begins");
		if (preconfigure()) {
			threadMessage("preconfigure succeed!");
		} else {
			threadMessage("preconfigure failed!");
			return;
		}
		threadMessage("loading database");
		SQLiteService.loadStore(store);
		threadMessage("loading database finished");

		createChildThreads();

		registerShutDownHandler();
		threadMessage("finished");
	}

	public void registerShutDownHandler() {
		Thread shutdown = new Thread("ShutDown") {
			public void run() {
				threadMessage("Preparing shutting!");
				Crawler.this.stopChildren();
				threadMessage("Save to database");
				SQLiteService.saveStore(store);
				threadMessage("Save to database finished");
				threadMessage("Now, the application can be safely shut down!");
			}
		};

		Runtime.getRuntime().addShutdownHook(shutdown);
		try {
			shutdown.join();
		} catch (InterruptedException e) {
			LogService.logException(e);
		}
	}

	/**
	 * add a url to url store
	 */
	public void addUrl(String url) {
		store.addUrl("", url);
	}

	/**
	 * add urls from a file
	 */
	public void addUrlsFromFile(String fileName) {
		Path path = Paths.get(fileName);
		if (!Files.exists(path)) {
			threadMessage("Cannot find file: " + fileName);
			return;
		}
		if (Files.isDirectory(path)) {
			threadMessage(fileName + " is a directory!");
			return;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = reader.readLine()) != null) {
				addUrl(line);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			LogService.logException(e);
		} catch (IOException e) {
			LogService.logException(e);
		}
	}

	public static void main(String[] args) {
		Crawler crawler = new Crawler();
		System.out.println("Usage: crawler [--url url_strings+]  [--file files+]");
		System.out.println("     : --url specify the initial urls to be crawled");
		System.out.println("     : --file specify the files containing the initial urls to be crawled");
		System.out.println("     : Both --url and --file parameters can be omitted except that the application is run for the first time.");
		List<String> parameters = new ArrayList<String>();
		int paraFlag = 0;
		for (int i = 0; i < args.length; i++) {
			String p = args[i];
			if (p.equals("--url")) {
				paraFlag = 1;
			} else if (p.equals("--file")) {
				paraFlag = 2;
			} else if (paraFlag > 0) {
				if (p.startsWith("-"))
					break;
				else
					parameters.add(p);
			}

		}
		for (String iter : parameters) {
			if (paraFlag == 1)
				crawler.addUrl(iter);
			else
				crawler.addUrlsFromFile(iter);
		}
		new Thread(crawler, "Crawler").start();
	}

	public void stopChildren() {
		urlFetcher.interrupt();
		waitThreadFinish(urlFetcher);
		waitThreadFinish(contentFetcher);
		waitThreadFinish(urlExtractor);
		waitThreadFinish(contentSaver);
	}

	public URLStore getURLStore() {
		return store;
	}
}
