package org.solution.crawler.threads;

import java.util.ArrayList;
import java.util.List;

public abstract class ParentThread extends AbstractThread {

	List<Thread> unfinished = new ArrayList<Thread>();

	public void waitChildrenFinish() {
		synchronized (unfinished) {
			while (!unfinished.isEmpty()) {
				try {
					threadMessage("has " + unfinished.size() + " children threads to finish!");
					unfinished.wait(WAIT_TIME);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void waitForLessChildren() {
		synchronized (unfinished) {
			while (unfinished.size() > QUEUE_SIZE) {
				try {
					unfinished.wait(WAIT_TIME);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public List<Thread> getChildren() {
		return unfinished;
	}
}
