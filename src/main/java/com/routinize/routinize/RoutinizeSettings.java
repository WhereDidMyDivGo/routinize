package com.routinize.routinize;

public final class RoutinizeSettings {

	public static final RoutinizeSettings INSTANCE = new RoutinizeSettings();

	private int clickRetryTimeoutMs = 5000;

	private RoutinizeSettings() {}

	public int clickRetryTimeoutMs() {
		return clickRetryTimeoutMs;
	}

	public void setClickRetryTimeoutMs(int clickRetryTimeoutMs) {
		this.clickRetryTimeoutMs = Math.max(0, clickRetryTimeoutMs);
	}
}