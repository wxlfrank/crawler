package org.solution.crawler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisitHash {
	/**
	 * The arrays store unvisited string (index = 0) and visited string (index = 1)
	 */
	@SuppressWarnings("unchecked")
	Set<String>[] hashs = new Set[] { new HashSet<String>(), new HashSet<String>() };

	/**
	 * Return the next unvisited String
	 * @return
	 */
	public synchronized String nextUnvisited() {
		if (hashs[0].isEmpty())
			return "";
		return hashs[0].iterator().next();
	}

	/**
	 * Set a str to be visited
	 * @param str
	 */
	public synchronized void setVisited(String str) {
		hashs[0].remove(str);
		hashs[1].add(str);
	}

	/**
	 * set a str to be unvisited
	 * @param str
	 */
	public synchronized void setUnVisited(String str) {
		hashs[1].remove(str);
		hashs[0].add(str);
	}

	/**
	 * add an unvisited str
	 * @param str
	 * @return
	 */
	public synchronized boolean addUnvisited(String str) {
		if (hashs[1].contains(str)) {
			return false;
		}
		if (!hashs[0].contains(str)) {
			hashs[0].add(str);
			return true;
		}
		return false;
	}

	/**
	 * Return whether a str is visited
	 * @param str
	 * @return
	 */
	public synchronized boolean isVisited(String str) {
		return hashs[1].contains(str);
	}

	/**
	 * Add list of strings into unvisited or visited
	 * @param index
	 * @param strs
	 */
	public synchronized void addAll(int index, List<String> strs) {
		if (index == 0 || index == 1)
			hashs[index].addAll(strs);
	}

	/**
	 * Return unvisited or visited strs
	 * @param index
	 * @return the corresponding item in the array if index is 0 or 1, null
	 *         otherwise
	 */
	public synchronized Set<String> get(int index) {
		if (index == 0 || index == 1)
			return hashs[index];
		return null;
	}

	/**
	 * Return whether two visit hashes are equal. Used for test
	 * @param other
	 * @return
	 */
	public boolean equals(VisitHash other) {
		boolean equal = true;
		for (int i = 0; i < 2; i++) {
			equal = hashs[i].equals(other.get(i));
			if (!equal) {
				return false;
			}
		}
		return true;
	}
}