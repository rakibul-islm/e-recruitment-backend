package com.bd.erecruitment.backup;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RestoreState {

	// VALIDATING checks the file without locking anyone out; only RUNNING puts the system into maintenance mode.
	public enum Phase { IDLE, VALIDATING, RUNNING, COMPLETED, FAILED }

	public record Snapshot(Phase phase, String fileName, String startedBy, Date startedOn, Date finishedOn, String message) {}

	private Snapshot current = new Snapshot(Phase.IDLE, null, null, null, null, null);

	public synchronized Snapshot get() {
		return current;
	}

	public synchronized boolean isRunning() {
		return current.phase() == Phase.RUNNING;
	}

	public synchronized boolean isBusy() {
		return current.phase() == Phase.VALIDATING || current.phase() == Phase.RUNNING;
	}

	public synchronized boolean tryBegin(String fileName, String startedBy) {
		if (isBusy()) return false;
		current = new Snapshot(Phase.VALIDATING, fileName, startedBy, new Date(), null, null);
		return true;
	}

	public synchronized void markRunning() {
		current = new Snapshot(Phase.RUNNING, current.fileName(), current.startedBy(), current.startedOn(), null, null);
	}

	public synchronized void complete(String message) {
		finish(Phase.COMPLETED, message);
	}

	public synchronized void fail(String message) {
		finish(Phase.FAILED, message);
	}

	private void finish(Phase phase, String message) {
		current = new Snapshot(phase, current.fileName(), current.startedBy(), current.startedOn(), new Date(), message);
	}
}
