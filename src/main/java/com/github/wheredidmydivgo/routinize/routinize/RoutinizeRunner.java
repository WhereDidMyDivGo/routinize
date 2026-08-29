package com.github.wheredidmydivgo.routinize.routinize;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RoutinizeRunner {

	private final Deque<Frame> stack = new ArrayDeque<>();
	private final Set<String> heldKeys = new HashSet<>();
	private boolean running = false;
	private boolean paused = false;
	private boolean autoPaused = false;
	private long pauseStartNanos;
	private String lastStopReason;

	public boolean isRunning() {
		return running;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isAutoPaused() {
		return autoPaused;
	}

	public boolean hasHeldKeys() {
		return !heldKeys.isEmpty();
	}

	public String consumeStopReason() {
		String reason = lastStopReason;
		lastStopReason = null;
		return reason;
	}

	public void start(List<RoutinizeStep> program) {
		stack.clear();
		stack.push(new Frame(program, false));
		running = true;
		paused = false;
	}

	public void stop() {
		stopInternal(RoutinizeSettings.INSTANCE.releaseKeysOnStop());
	}

	public void forceStop() {
		stopInternal(true);
	}

	private void stopInternal(boolean release) {
		running = false;
		paused = false;
		autoPaused = false;
		stack.clear();
		if (release) {
			releaseHeldKeys();
		}
		lastStopReason = null;
	}

	private void stopWithReason(String reason) {
		stop();
		lastStopReason = reason;
	}

	public void toggle(List<RoutinizeStep> program) {
		if (running) {
			stop();
		} else {
			start(program);
		}
	}

	public void pause() {
		pauseInternal(false);
	}

	public void autoPause() {
		pauseInternal(true);
	}

	private void pauseInternal(boolean auto) {
		if (!running || paused) return;
		setHeldKeysDown(false);
		paused = true;
		autoPaused = auto;
		pauseStartNanos = System.nanoTime();
	}

	public void resume() {
		if (!running || !paused) return;
		setHeldKeysDown(true);
		paused = false;
		autoPaused = false;
		long pausedNanos = System.nanoTime() - pauseStartNanos;
		Frame top = stack.peek();
		if (top != null && top.stepStartNanos != 0) {
			top.stepStartNanos += pausedNanos;
		}
	}

	public void togglePause() {
		if (paused) {
			resume();
		} else {
			pause();
		}
	}

	public void tick(RoutinizeState routinizeState) {
		if (!running) return;
		if (paused) return;
		if (stack.isEmpty()) {
			stopWithReason("reached end of script");
			return;
		}

		Frame frame = stack.peek();
		if (frame.index >= frame.steps.size()) {
			stack.pop();
			return;
		}

		RoutinizeStep step = frame.steps.get(frame.index);

		if (step instanceof RoutinizeStep.RunCommand run) {
			routinizeState.runCommand(run.command());
			frame.advance();
		} else if (step instanceof RoutinizeStep.Wait wait) {
			if (frame.stepStartNanos == 0) {
				frame.stepStartNanos = System.nanoTime();
				frame.waitTargetMs = wait.minMs() >= wait.maxMs()
					? wait.minMs()
					: ThreadLocalRandom.current().nextInt(wait.minMs(), wait.maxMs() + 1);
			}
			if (elapsedMs(frame) >= frame.waitTargetMs) {
				frame.advance();
			}
		} else if (step instanceof RoutinizeStep.WaitForOpen wait) {
			if (frame.stepStartNanos == 0) {
				frame.stepStartNanos = System.nanoTime();
			}
			if (routinizeState.screenOpen()) {
				frame.advance();
			} else if (elapsedMs(frame) > wait.timeoutMs()) {
				stopWithReason("wait_open timed out");
			}
		} else if (step instanceof RoutinizeStep.WaitForChange wait) {
			if (frame.stepStartNanos == 0) {
				frame.stepStartNanos = System.nanoTime();
				frame.waitBaseline = routinizeState.fingerprint();
			}
			if (!Objects.equals(routinizeState.fingerprint(), frame.waitBaseline)) {
				frame.advance();
			} else if (elapsedMs(frame) > wait.timeoutMs()) {
				stopWithReason("wait_change timed out");
			}
		} else if (step instanceof RoutinizeStep.CloseScreen) {
			routinizeState.closeScreen();
			frame.advance();
		} else if (step instanceof RoutinizeStep.Stop) {
			stopWithReason("script stop");
		} else if (step instanceof RoutinizeStep.Continue) {
			while (!stack.isEmpty() && !stack.peek().isLoopBody) {
				stack.pop();
			}
			if (!stack.isEmpty()) {
				stack.pop();
			}
		} else if (step instanceof RoutinizeStep.Break) {
			while (!stack.isEmpty() && !stack.peek().isLoopBody) {
				stack.pop();
			}
			if (!stack.isEmpty()) {
				stack.pop();
			}
			if (!stack.isEmpty()) {
				stack.peek().advance();
			}
		} else if (step instanceof RoutinizeStep.Action action) {
			List<RoutinizeStep.ActionToken> tokens = action.tokens();
			while (frame.actionTokenIndex < tokens.size()) {
				RoutinizeStep.ActionToken token = tokens.get(frame.actionTokenIndex);
				switch (token) {
					case RoutinizeStep.KeyToggle toggle -> {
						routinizeState.setKeyState(toggle.key(), toggle.down());
						if (toggle.down()) {
							heldKeys.add(toggle.key());
						} else {
							heldKeys.remove(toggle.key());
						}
						frame.actionTokenIndex++;
						frame.stepStartNanos = 0;
					}
					case RoutinizeStep.InventoryClick click -> {
						if (routinizeState.clickSlot(click.button(), click.shift(), click.nameContains(), click.loreContains())) {
							frame.actionTokenIndex++;
							frame.stepStartNanos = 0;
						} else {
							if (frame.stepStartNanos == 0) {
								frame.stepStartNanos = System.nanoTime();
							} else if (elapsedMs(frame) > RoutinizeSettings.INSTANCE.clickRetryTimeoutMs()) {
								stopWithReason("click timed out (" + describeClick(click) + ")");
							}
							return;
						}
					}
				}
			}
			frame.advance();
		} else if (step instanceof RoutinizeStep.IfPresent ifStep) {
			boolean present = routinizeState.matchExists(ifStep.nameContains(), ifStep.loreContains());
			if (ifStep.negated()) present = !present;
			frame.advance();
			stack.push(new Frame(present ? ifStep.thenSteps() : ifStep.elseSteps(), false));
		} else if (step instanceof RoutinizeStep.LoopUntil loop) {
			boolean present = routinizeState.matchExists(loop.nameContains(), loop.loreContains());
			if (loop.negated()) present = !present;
			if (present) {
				frame.advance();
			} else {
				stack.push(new Frame(loop.body(), true));
			}
		} else if (step instanceof RoutinizeStep.Loop loop) {
			if (loop.count() != -1 && frame.loopIterations >= loop.count()) {
				frame.advance();
			} else {
				frame.loopIterations++;
				stack.push(new Frame(loop.body(), true));
			}
		}
	}

	private static String describeClick(RoutinizeStep.InventoryClick click) {
		StringBuilder sb = new StringBuilder(click.button());
		if (click.shift()) sb.append(" shift");
		if (click.nameContains() != null) sb.append(" name=\"").append(click.nameContains()).append('"');
		if (click.loreContains() != null) sb.append(" lore=\"").append(click.loreContains()).append('"');
		return sb.toString();
	}

	private static long elapsedMs(Frame frame) {
		return (System.nanoTime() - frame.stepStartNanos) / 1_000_000L;
	}

	private void releaseHeldKeys() {
		setHeldKeysDown(false);
		heldKeys.clear();
	}

	private void setHeldKeysDown(boolean down) {
		for (String key : heldKeys) {
			KeyActions.set(key, down);
		}
	}

	private static final class Frame {
		final List<RoutinizeStep> steps;
		final boolean isLoopBody;
		int index = 0;
		int actionTokenIndex = 0;
		long stepStartNanos = 0;
		int waitTargetMs = 0;
		List<String> waitBaseline;
		int loopIterations = 0;

		Frame(List<RoutinizeStep> steps, boolean isLoopBody) {
			this.steps = steps;
			this.isLoopBody = isLoopBody;
		}

		void advance() {
			index++;
			actionTokenIndex = 0;
			stepStartNanos = 0;
			waitTargetMs = 0;
			waitBaseline = null;
			loopIterations = 0;
		}
	}
}