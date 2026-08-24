package com.routinize.routinize;

import java.util.List;

public sealed interface RoutinizeStep {

	record RunCommand(String command) implements RoutinizeStep {}

	record Wait(int minMs, int maxMs) implements RoutinizeStep {}

	record WaitForOpen(int timeoutMs) implements RoutinizeStep {}

	record WaitForChange(int timeoutMs) implements RoutinizeStep {}

	record CloseScreen() implements RoutinizeStep {}

	record Stop() implements RoutinizeStep {}

	record Action(List<ActionToken> tokens) implements RoutinizeStep {}

	sealed interface ActionToken {}

	record KeyToggle(String key, boolean down) implements ActionToken {}

	record InventoryClick(String button, boolean shift, String nameContains, String loreContains) implements ActionToken {}

	record IfPresent(
		String nameContains,
		String loreContains,
		List<RoutinizeStep> thenSteps,
		List<RoutinizeStep> elseSteps
	) implements RoutinizeStep {}

	record LoopUntil(
		String nameContains,
		String loreContains,
		List<RoutinizeStep> body
	) implements RoutinizeStep {}

	record Loop(List<RoutinizeStep> body) implements RoutinizeStep {}
}