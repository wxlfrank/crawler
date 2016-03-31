package org.solution.crawler.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface of downloading content from internet
 * @author wxlfr_000
 *
 */
public interface Downloader {
	Object download(InputStream input) throws IOException;
}
