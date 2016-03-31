package org.solution.crawler;

import org.solution.crawler.sql.SQLiteService;

import junit.framework.Assert;
import junit.framework.TestCase;

public class URLStoreTest extends TestCase {

	public void testDBLoad() {
		URLStore store = new URLStore();
		SQLiteService.loadStore(store);
		URLStore store1 = new URLStore();
		SQLiteService.loadStore(store1);
		Assert.assertTrue(store.equals(store1));
	}

	public void testDBStore() {
		URLStore store = new URLStore();
		SQLiteService.loadStore(store);
		store.addUrl("www.brukea.com");
		store.addUrl("www.Brukea.com");
		store.addUrl("www.brukea1.com");
		SQLiteService.saveStore(store);
		URLStore store1 = new URLStore();
		SQLiteService.loadStore(store1);
		store1.equals(store);
	}

}
