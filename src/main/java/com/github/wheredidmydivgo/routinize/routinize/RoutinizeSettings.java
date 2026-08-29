package com.github.wheredidmydivgo.routinize.routinize;

public final class RoutinizeSettings {

	public static final RoutinizeSettings INSTANCE = new RoutinizeSettings();

	private int clickRetryTimeoutMs = 5000;
	private boolean chatFeedbackEnabled = true;
	private boolean releaseKeysOnStop = true;
	private boolean autoResumeEnabled = false;

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

	public boolean releaseKeysOnStop() {
		return releaseKeysOnStop;
	}

	public void setReleaseKeysOnStop(boolean releaseKeysOnStop) {
		this.releaseKeysOnStop = releaseKeysOnStop;
	}

	public boolean autoResumeEnabled() {
		return autoResumeEnabled;
	}

	public void setAutoResumeEnabled(boolean autoResumeEnabled) {
		this.autoResumeEnabled = autoResumeEnabled;
	}
}