package org.solution.crawler.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TextDownloaderTest extends TestCase {

	public void testDownload() {
		TextDownloader downloader = new TextDownloader();
		String result = null;
		try {
			result = downloader.download(null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Assert.assertNull(result);

		try {
			result = downloader.download(new URL("https://www.telenor.no").openConnection().getInputStream());
			Assert.assertTrue(result instanceof String);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			result = downloader.download(new URL("https://www.a.b.no").openConnection().getInputStream());
			Assert.assertTrue(result instanceof String);
			Assert.assertTrue(false);
		} catch (MalformedURLException e) {
			Assert.assertTrue(true);
		} catch (IOException e) {
			Assert.assertTrue(true);
		}
	}

}
