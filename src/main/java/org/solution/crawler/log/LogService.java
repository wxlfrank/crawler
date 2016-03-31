package org.solution.crawler.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogService {

	public static Logger log = getLogger();

	private static Logger getLogger() {
		Logger log = Logger.getLogger("crawler");
		log.setLevel(Level.ALL);
		log.setUseParentHandlers(false);
		try {
			Path path = Paths.get("data");
			Files.createDirectories(path);
			path = path.resolve("log.txt");
			FileHandler handle = new FileHandler(path.toString());
			handle.setFormatter(new SimpleFormatter());
			log.addHandler(handle);
		} catch (SecurityException e) {
			logException(e);
		} catch (IOException e) {
			logException(e);
			throw new RuntimeException("Problems with creating the log files");
		}
		return log;
	}

	public static void logException(Exception e){
		log.log(Level.SEVERE, e.getMessage(), e);
	}

	public static void logException(String errorMessage, Exception e) {
		log.log(Level.SEVERE, errorMessage + e.getMessage(), e);
	}

}
