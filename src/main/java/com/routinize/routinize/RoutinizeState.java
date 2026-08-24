package com.routinize.routinize;

import java.util.List;

public interface RoutinizeState {

	boolean screenOpen();

	boolean anyScreenOpen();

	List<String> fingerprint();

	boolean matchExists(String nameContains, String loreContains);

	boolean clickSlot(String button, boolean shift, String nameContains, String loreContains);

	void closeScreen();

	void runCommand(String command);

	void setKeyState(String key, boolean down);

	void sendFeedback(String message);
}