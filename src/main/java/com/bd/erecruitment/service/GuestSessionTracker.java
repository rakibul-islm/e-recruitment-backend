package com.bd.erecruitment.service;

public interface GuestSessionTracker {
	void track(String guestId);
	long activeCount();
}
