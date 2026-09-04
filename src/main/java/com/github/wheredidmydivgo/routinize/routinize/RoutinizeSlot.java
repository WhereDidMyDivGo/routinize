package com.github.wheredidmydivgo.routinize.routinize;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RoutinizeSlot {

	private String name;
	private int toggleKeyCode = -1;
	private int pauseKeyCode = -1;
	public final RoutinizeRunner runner = new RoutinizeRunner();
	private String sourceText = "";
	private List<RoutinizeStep> program = List.of();
	private boolean usesWorldActions = false;

	RoutinizeSlot(String name) {
		this.name = name == null || name.isBlank() ? "Unnamed" : name;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null || name.isBlank() ? "Unnamed" : name;
	}

	public int toggleKeyCode() {
		return toggleKeyCode;
	}

	public void setToggleKeyCode(int toggleKeyCode) {
		this.toggleKeyCode = toggleKeyCode;
	}

	public int pauseKeyCode() {
		return pauseKeyCode;
	}

	public void setPauseKeyCode(int pauseKeyCode) {
		this.pauseKeyCode = pauseKeyCode;
	}

	public String toggleKeyName() {
		return keyName(toggleKeyCode);
	}

	public String pauseKeyName() {
		return keyName(pauseKeyCode);
	}

	public static String keyName(int keyCode) {
		if (keyCode == -1) {
			return "unbound";
		}
		String label = GLFW.glfwGetKeyName(keyCode, 0);
		if (label != null && !label.isBlank()) {
			return label.toUpperCase();
		}
		return switch (keyCode) {
			case GLFW.GLFW_KEY_F1 ->  "F1";
			case GLFW.GLFW_KEY_F2 ->  "F2";
			case GLFW.GLFW_KEY_F3 ->  "F3";
			case GLFW.GLFW_KEY_F4 ->  "F4";
			case GLFW.GLFW_KEY_F5 ->  "F5";
			case GLFW.GLFW_KEY_F6 ->  "F6";
			case GLFW.GLFW_KEY_F7 ->  "F7";
			case GLFW.GLFW_KEY_F8 ->  "F8";
			case GLFW.GLFW_KEY_F9 ->  "F9";
			case GLFW.GLFW_KEY_F10 ->  "F10";
			case GLFW.GLFW_KEY_F11 ->  "F11";
			case GLFW.GLFW_KEY_F12 ->  "F12";
			case GLFW.GLFW_KEY_LEFT ->  "LEFT";
			case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
			case GLFW.GLFW_KEY_UP ->  "UP";
			case GLFW.GLFW_KEY_DOWN ->  "DOWN";
			case GLFW.GLFW_KEY_SPACE ->  "SPACE";
			case GLFW.GLFW_KEY_TAB ->  "TAB";
			case GLFW.GLFW_KEY_ESCAPE ->  "ESC";
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER ->  "ENTER";
			case GLFW.GLFW_KEY_BACKSPACE ->  "BACKSPACE";
			case GLFW.GLFW_KEY_DELETE ->  "DELETE";
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT ->  "SHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL ->  "CTRL";
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT ->  "ALT";
			case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER ->  "SUPER";
			default -> "Key " + keyCode;
		};
	}

	public boolean hasPauseKeyBinding() {
		return pauseKeyCode != -1;
	}

	public String sourceText() {
		return sourceText;
	}

	public boolean usesWorldActions() {
		return usesWorldActions;
	}

	public void applySource(String text) {
		List<RoutinizeStep> parsed = RoutinizeParser.parse(text);
		this.sourceText = text;
		this.program = parsed;
		this.usesWorldActions = containsWorldAction(parsed);
	}

	private static boolean containsWorldAction(List<RoutinizeStep> steps) {
		for (RoutinizeStep step : steps) {
			if (step instanceof RoutinizeStep.Action action) {
				for (RoutinizeStep.ActionToken token : action.tokens()) {
					if (token instanceof RoutinizeStep.KeyToggle) return true;
				}
			} else if (step instanceof RoutinizeStep.IfPresent ifStep) {
				if (containsWorldAction(ifStep.thenSteps()) || containsWorldAction(ifStep.elseSteps())) return true;
			} else if (step instanceof RoutinizeStep.While whileStep) {
				if (containsWorldAction(whileStep.body())) return true;
			} else if (step instanceof RoutinizeStep.Loop loop) {
				if (containsWorldAction(loop.body())) return true;
			}
		}
		return false;
	}

	public void toggle() {
		runner.toggle(program);
	}

	public void togglePause() {
		runner.togglePause();
	}

	public boolean isRunning() {
		return runner.isRunning();
	}

	public boolean isPaused() {
		return runner.isPaused();
	}
}