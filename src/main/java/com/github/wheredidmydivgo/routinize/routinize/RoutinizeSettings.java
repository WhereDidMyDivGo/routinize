package com.github.wheredidmydivgo.routinize.routinize;

public final class RoutinizeSettings {

	public static final RoutinizeSettings INSTANCE = new RoutinizeSettings();

	private int clickRetryTimeoutMs = 5000;
	private boolean chatFeedbackEnabled = true;

	private RoutinizeSettings() {}

	public int clickRetryTimeoutMs() {
		return clickRetryTimeoutMs;
	}

	public void setClickRetryTimeoutMs(int clickRetryTimeoutMs) {
		this.clickRetryTimeoutMs = Math.max(0, clickRetryTimeoutMs);
	}

	public boolean chatFeedbackEnabled() {
		return chatFeedbackEnabled;
	}

	public void setChatFeedbackEnabled(boolean chatFeedbackEnabled) {
		this.chatFeedbackEnabled = chatFeedbackEnabled;
	}
}