package com.github.wheredidmydivgo.routinize.routinize;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class RoutinizeManager {

	public static final RoutinizeManager INSTANCE = new RoutinizeManager();

	private final List<RoutinizeSlot> slots = new ArrayList<>();
	private final Map<RoutinizeSlot, Boolean> prevToggleDown = new IdentityHashMap<>();
	private final Map<RoutinizeSlot, Boolean> prevPauseDown = new IdentityHashMap<>();
	private boolean wasScreenOpen = false;

	private RoutinizeManager() {
	}

	public RoutinizeSlot createProfile(String name) {
		if (findProfile(name) != null) {
			throw new IllegalArgumentException("profile already exists: " + name);
		}
		RoutinizeSlot slot = new RoutinizeSlot(name);
		slots.add(slot);
		return slot;
	}

	public RoutinizeSlot createProfile(String name, int toggleKeyCode, int pauseKeyCode) {
		RoutinizeSlot slot = createProfile(name);
		slot.setToggleKeyCode(toggleKeyCode);
		slot.setPauseKeyCode(pauseKeyCode);
		return slot;
	}

	public void deleteProfile(RoutinizeSlot slot) {
		slot.runner.stop();
		slots.remove(slot);
		prevToggleDown.remove(slot);
		prevPauseDown.remove(slot);
	}

	public void reset() {
		for (RoutinizeSlot slot : slots) {
			slot.runner.stop();
		}
		slots.clear();
		prevToggleDown.clear();
		prevPauseDown.clear();
	}

	public RoutinizeSlot findProfile(String name) {
		if (name == null) return null;
		for (RoutinizeSlot slot : slots) {
			if (slot.name().equalsIgnoreCase(name.strip())) {
				return slot;
			}
		}
		return null;
	}

	public List<RoutinizeSlot> profiles() {
		return List.copyOf(slots);
	}

	public void tick(RoutinizeState routinizeState) {
		Minecraft mc = Minecraft.getInstance();
		long window = mc.getWindow().handle();

		boolean screenOpenNow = routinizeState.anyScreenOpen();
		boolean screenJustOpened = screenOpenNow && !wasScreenOpen;
		wasScreenOpen = screenOpenNow;

		for (RoutinizeSlot slot : slots) {
			int toggleKey = slot.toggleKeyCode();
			boolean toggleDown = toggleKey != -1 && GLFW.glfwGetKey(window, toggleKey) == GLFW.GLFW_PRESS;
			boolean prevToggle = prevToggleDown.getOrDefault(slot, false);
			if (toggleDown && !prevToggle) {
				if (slot.isRunning()) {
					slot.toggle();
					routinizeState.sendFeedback("Stopped '" + slot.name() + "'");
				} else if (!screenOpenNow) {
					slot.toggle();
					routinizeState.sendFeedback("Started '" + slot.name() + "'");
				}
			}
			prevToggleDown.put(slot, toggleDown);

			int pauseKey = slot.pauseKeyCode();
			boolean pauseDown = pauseKey != -1 && GLFW.glfwGetKey(window, pauseKey) == GLFW.GLFW_PRESS;
			boolean prevPause = prevPauseDown.getOrDefault(slot, false);
			if (pauseDown && !prevPause && slot.isRunning()) {
				boolean wasPaused = slot.isPaused();
				slot.togglePause();
				routinizeState.sendFeedback((wasPaused ? "Resumed '" : "Paused '") + slot.name() + "'");
			}
			prevPauseDown.put(slot, pauseDown);

			if (screenJustOpened && slot.isRunning() && !slot.isPaused() && slot.runner.hasHeldKeys()) {
				if (slot.hasPauseKeyBinding()) {
					slot.runner.pause();
					routinizeState.sendFeedback("Paused '" + slot.name() + "': gui opened");
				} else {
					slot.runner.stop();
					routinizeState.sendFeedback("Stopped '" + slot.name() + "': gui opened (no pause key set)");
				}
			}

			boolean wasRunning = slot.isRunning();
			slot.runner.tick(routinizeState);
			if (wasRunning && !slot.isRunning()) {
				String reason = slot.runner.consumeStopReason();
				routinizeState.sendFeedback("Stopped '" + slot.name() + "'" + (reason == null ? "" : ": " + reason));
			}
		}
	}
}