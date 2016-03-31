package org.solution.crawler.http;

import java.net.URL;

import org.solution.crawler.URLStore;

import junit.framework.Assert;
import junit.framework.TestCase;

public class HttpServiceTest extends TestCase {

	public void testFetchContent() {
		Object content = null;
		content = HttpService.fetchContent("");
		Assert.assertNull(content);
		content = HttpService.fetchContent("www.telenor.no");
		Assert.assertNull(content);

		content = HttpService.fetchContent("https://www.t.no");
		Assert.assertNull(content);

		content = HttpService.fetchContent("http://www.telenor.no/bedrift/kundeservice");
		Assert.assertNotNull(content);

		content = HttpService.fetchContent("https://www.telenor.no/Images/telenorlogo2x_tcm52-213553.png");
		Assert.assertNull(content);

		content = HttpService.fetchContent("https://www.telenor.no");
		Assert.assertNotNull(content);
		
		content = HttpService.fetchContent("http://pipes.yahoo.com/pipes/pipe.run?_id=d70fef54f786972b1e7b256f1dbd8322&_render=rss");
		Assert.assertNull(content);
		
		content = HttpService.fetchContent("http://www.online.no/service/eksperttips-bedre-tradlost-nett.jsp");
		Assert.assertNotNull(content);
	}

	public void testToValidURL() {
		URL url = URLStore.toValidURL("http://www.abc.no", "www.symantec.com");
		Assert.assertTrue(url instanceof URL);
		Assert.assertTrue(url.toString().equals("http://www.symantec.com"));

		
		url = URLStore.toValidURL("http://www.abc.no", "tlnr.no");
		Assert.assertTrue(url instanceof URL);

		url = URLStore.toValidURL("http://www.abc.no", "/tlnr.no");
		Assert.assertNotNull(url);
		url = URLStore.toValidURL("http://www.abc.no", "tlnr/no");
		Assert.assertNotNull(url);

		url = URLStore.toValidURL("", "www.symantec.com");
		Assert.assertTrue(url instanceof URL);
		Assert.assertTrue(url.toString().equals("http://www.symantec.com"));

		url = URLStore.toValidURL("", "tlnr.no");
		Assert.assertTrue(url instanceof URL);

		url = URLStore.toValidURL("", "http://tlnr.no");
		Assert.assertTrue(url instanceof URL);

		url = URLStore.toValidURL("", "http://www.tlnr.no");
		Assert.assertTrue(url instanceof URL);

		
		url = URLStore.toValidURL("", "/tlnr.no");
		Assert.assertNull(url);
		url = URLStore.toValidURL("", "http://www.wikipedia.org");
		Assert.assertNotNull(url);
		url = URLStore.toValidURL("", "////tlnr.no");
		Assert.assertNull(url);
		url = URLStore.toValidURL("", "tlnr/no");
		Assert.assertNull(url);
		url = URLStore.toValidURL("", "gq.zip");
		Assert.assertNull(url);
		url = URLStore.toValidURL("", "mailto:jobbsmartere@telenor.no");
		Assert.assertNull(url);
		
		
	}

}
