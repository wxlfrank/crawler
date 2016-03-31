/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.solution.crawler.sql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.solution.crawler.URLStore;
import org.solution.crawler.VisitHash;
import org.solution.crawler.log.LogService;

/**
 *
 * @author wxlfr_000
 */
public class SQLiteService {
	private static final String JDBC_DRIVER = "org.sqlite.JDBC";
	private static final String DATABASE_URL = "jdbc:sqlite:data/crawl.db";
	private static boolean registered = false;

	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		if (!registered) {
			Class.forName(JDBC_DRIVER);
			registered = true;
			try {
				Files.createDirectories(Paths.get("data"));
			} catch (IOException e) {
				LogService.logException(e);
			}
		}
		return DriverManager.getConnection(DATABASE_URL);
	}

	/**
	 * Load urls, visited and unvisited, from database to url store
	 */
	public static void loadStore(URLStore store) {
		Connection connection = null;
		try {
			connection = getConnection();
			Statement stat = connection.createStatement();
			for (int index = 0; index < 2; ++index) {
				// create two tables: host0 (unvisited hosts) and host1 (visited hosts), if they do not exist
				String sql = "create table if not exists host" + index + " (host text primary key not null);";
				stat.executeUpdate(sql);
				// query the list of hosts from these two tables and load to thefield: hosts;
				List<String> hs = getList(stat, "select host from host" + index + ";", "host");
				store.getHosts().addAll(index, hs);
				for (String host : hs) {
					// query the lists of visited urls and unvisited urls, and load them into the field: mapHost2Urls
					VisitHash hashForHost = new VisitHash();
					for (int i = 0; i < 2; ++i) {
						List<String> urls = getList(stat, "select url from '" + toSQLTextValue(host) + i + "';", "url");
						hashForHost.addAll(i, urls);
					}
					store.getURLs().put(host, hashForHost);
				}
			}
			stat.close();
			connection.close();
		} catch (ClassNotFoundException e) {
			LogService.logException(e);
		} catch (SQLException e) {
			LogService.logException(e);
		} finally {
			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LogService.logException(e);
				}
		}
	}

	/**
	 * Get values of column from query result of sql expression
	 * 
	 * @param stat
	 * @param sql
	 * @param column
	 * @return a list of strings for a certain column
	 * @throws SQLException
	 */
	private static List<String> getList(Statement stat, String sql, String column) throws SQLException {
		ResultSet result = null;
		List<String> list = new ArrayList<String>();
		try {
			result = stat.executeQuery(sql);
		} catch (SQLException e) {
			LogService.logException(e);
			return list;
		}
		if (result != null)
			while (result.next()) {
				list.add(result.getString(column));
			}
		return list;
	}

	/**
	 * Change str to text value in sql expression
	 * @param str
	 * @return
	 */
	private static String toSQLTextValue(String str) {
		return str.replaceAll("'", "''");
	}

	/**
	 * update urls of a host in database. Used when saving a url store
	 * @param store
	 * @param stat
	 * @param sqls
	 * @param host
	 * @throws SQLException
	 */
	private static void updateURLs(URLStore store, Statement stat, List<String> sqls, String host) throws SQLException {
		String sql = null;
		VisitHash hashForHost = store.getURLs().get(host);
		@SuppressWarnings("unchecked")
		Set<String>[] new_urls = new HashSet[2];
		for (int index = 0; index < 2; ++index) {
			new_urls[index] = new HashSet<String>();
			new_urls[index].addAll(hashForHost.get(index));
		}
		for (int index = 0; index < 2; ++index) {
			sql = "select url from '" + toSQLTextValue(host) + index + "';";
			List<String> old_urls = getList(stat, sql, "url");
			for (String url : old_urls) {
				if (!new_urls[index].contains(url)) {
					sqls.add("delete from '" + toSQLTextValue(host) + index + "' where url='" + toSQLTextValue(url) + "';");
				}
				new_urls[index].remove(url);
			}
		}
		for (int index = 0; index < 2; ++index) {
			for (String url : new_urls[index]) {
				sqls.add("insert into '" + toSQLTextValue(host) + index + "' values ('" + toSQLTextValue(url) + "');");
			}
		}
	}

	/**
	 * Add urls of host to database. Used when saving a url store
	 * 
	 * @param store
	 * @param sqls
	 * @param host
	 * @throws SQLException
	 */
	private static void addURLs(URLStore store, List<String> sqls, String host) throws SQLException {
		VisitHash hashForHost = store.getURLs().get(host);
		for (int index = 0; index < 2; ++index) {
			for (String url : hashForHost.get(index)) {
				sqls.add("insert into '" + toSQLTextValue(host) + index + "' values ('" + toSQLTextValue(url) + "');");
			}
		}
	}

	/**
	 * Store urls, crawled or uncrawled, to database
	 */
	public static void saveStore(URLStore store) {
		Connection connection = null;
		try {
			connection = getConnection();
			Statement stat = connection.createStatement();
			List<String> sqls = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			Set<String>[] new_hosts = new HashSet[2];
			String sql = "";
			for (int index = 0; index < 2; ++index) {
				new_hosts[index] = new HashSet<String>();
				new_hosts[index].addAll(store.getHosts().get(index));
				sql = "create table if not exists host" + index + " (host text primary key not null);";
				sqls.add(sql);
			}
			for (int index = 0; index < 2; ++index) {
				// Query the old hosts and compare them with the current hosts
				sql = "select host from host" + index;
				List<String> old_hosts = getList(stat, sql, "host");

				for (String host : old_hosts) {
					if (new_hosts[index].contains(host)) {
						// If a host is kept, update its urls in database
						updateURLs(store, stat, sqls, host);
					} else {
						// If a host is deleted from unvisited list (visited list), it must be added into visited list (unvisited list).
						// In this case, we delete it from one, add it  into the other and update its urls in database.
						sqls.add("delete from host" + index + " where host='" + toSQLTextValue(host) + "';");
						sqls.add("insert into host" + (1 - index) + " values ('" + toSQLTextValue(host) + "');");
						new_hosts[1 - index].remove(host);
						updateURLs(store, stat, sqls, host);
					}
					new_hosts[index].remove(host);
				}
			}
			for (int index = 0; index < 2; ++index) {
				for (String host : new_hosts[index]) {
					// For each newly added host, we create a table for it if its table does not exist.
					// Then we update its urls in database
					sql = "create table if not exists '" + toSQLTextValue(host) + "0' (url text primary key not null);";
					sqls.add(sql);
					sql = "create table if not exists '" + toSQLTextValue(host) + "1' (url text primary key not null);";
					sqls.add(sql);
					sql = "insert into host" + index + " values ('" + toSQLTextValue(host) + "');";
					sqls.add(sql);
				}
			}
			for (int index = 0; index < 2; ++index) {
				for (String host : new_hosts[index]) {
					addURLs(store, sqls, host);
				}
			}
			stat.close();
			connection.close();
			connection = SQLiteService.getConnection();
			connection.setAutoCommit(false);
			stat = connection.createStatement();
			for (String iter : sqls) {
				stat.addBatch(iter);
			}
			stat.executeBatch();
			connection.commit();
			connection.close();
		} catch (ClassNotFoundException e) {
			LogService.logException(e);
		} catch (SQLException e) {
			LogService.logException(e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LogService.logException(e);
				}
			}
		}
	}
}
