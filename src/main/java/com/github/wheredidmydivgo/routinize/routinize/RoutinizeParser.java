package com.github.wheredidmydivgo.routinize.routinize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RoutinizeParser {

	private static final Pattern KV = Pattern.compile("(name|lore)=\"([^\"]*)\"");
	private static final Pattern ACTION_TOKEN = Pattern.compile("\\[([^\\]]*)\\]");
	private static final Set<String> CLICK_VERBS = Set.of("lclick", "rclick", "mclick");

	private RoutinizeParser() {}

	public static List<RoutinizeStep> parse(String source) {
		List<String> lines = new ArrayList<>();
		for (String raw : source.split("\n")) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) continue;
			lines.add(line);
		}
		Cursor cursor = new Cursor(lines);
		List<RoutinizeStep> steps = parseBlock(cursor, false, false);
		if (!cursor.atEnd()) {
			throw new IllegalArgumentException("unexpected '" + cursor.peek() + "'");
		}
		return steps;
	}

	private static List<RoutinizeStep> parseBlock(Cursor cursor, boolean insideBlock, boolean insideLoop) {
		List<RoutinizeStep> steps = new ArrayList<>();
		while (!cursor.atEnd()) {
			String line = cursor.peek();
			if (line.equals("end") || line.equals("else") || line.startsWith("elseif")) {
				if (!insideBlock) {
					throw new IllegalArgumentException("unexpected '" + line + "'");
				}
				return steps;
			}
			steps.add(parseLine(cursor, insideLoop));
		}
		if (insideBlock) {
			throw new IllegalArgumentException("missing 'end' to close a block");
		}
		return steps;
	}

	private static RoutinizeStep parseLine(Cursor cursor, boolean insideLoop) {
		String line = cursor.next();

		if (line.startsWith("command ")) {
			return new RoutinizeStep.RunCommand(line.substring("command ".length()).strip());
		}
		if (line.startsWith("wait ")) {
			String value = line.substring("wait ".length()).strip();
			int dash = value.indexOf('-');
			if (dash > 0) {
				int min = Integer.parseInt(value.substring(0, dash).strip());
				int max = Integer.parseInt(value.substring(dash + 1).strip());
				if (max < min) {
					throw new IllegalArgumentException("invalid wait range: " + value);
				}
				return new RoutinizeStep.Wait(min, max);
			}
			int ms = Integer.parseInt(value);
			return new RoutinizeStep.Wait(ms, ms);
		}
		if (line.equals("wait_open")) {
			return new RoutinizeStep.WaitForOpen(5000);
		}
		if (line.equals("wait_change")) {
			return new RoutinizeStep.WaitForChange(5000);
		}
		if (line.equals("close")) {
			return new RoutinizeStep.CloseScreen();
		}
		if (line.equals("stop")) {
			return new RoutinizeStep.Stop();
		}
		if (line.equals("continue")) {
			if (!insideLoop) {
				throw new IllegalArgumentException("'continue' outside of a loop");
			}
			return new RoutinizeStep.Continue();
		}
		if (line.equals("break")) {
			if (!insideLoop) {
				throw new IllegalArgumentException("'break' outside of a loop");
			}
			return new RoutinizeStep.Break();
		}
		if (line.startsWith("action")) {
			return parseAction(line);
		}
		if (line.startsWith("if")) {
			return parseIfBlock(line, cursor, insideLoop);
		}
		if (line.startsWith("loop_until")) {
			Condition condition = extractCondition(line, "loop_until");
			Match match = extractMatch(condition.body());
			List<RoutinizeStep> body = parseBlock(cursor, true, true);
			expectEnd(cursor);
			return new RoutinizeStep.LoopUntil(match.name(), match.lore(), condition.negated(), body);
		}
		if (line.equals("loop") || line.startsWith("loop ")) {
			int count = -1;
			if (line.startsWith("loop ")) {
				String value = line.substring("loop ".length()).strip();
				count = Integer.parseInt(value);
				if (count <= 0) {
					throw new IllegalArgumentException("loop count must be positive: " + value);
				}
			}
			List<RoutinizeStep> body = parseBlock(cursor, true, true);
			expectEnd(cursor);
			return new RoutinizeStep.Loop(body, count);
		}
		throw new IllegalArgumentException("unrecognised line: " + line);
	}

	private static RoutinizeStep parseAction(String line) {
		List<RoutinizeStep.ActionToken> tokens = new ArrayList<>();
		Matcher m = ACTION_TOKEN.matcher(line);
		while (m.find()) {
			String content = m.group(1).strip();
			int sp = content.indexOf(' ');
			String verb = (sp == -1 ? content : content.substring(0, sp)).toLowerCase();
			String rest = sp == -1 ? "" : content.substring(sp + 1).strip();

			if (rest.equals("down") || rest.equals("up")) {
				if (!KeyActions.isValid(verb)) {
					throw new IllegalArgumentException("unknown world action: " + verb);
				}
				tokens.add(new RoutinizeStep.KeyToggle(verb, rest.equals("down")));
				continue;
			}

			if (!CLICK_VERBS.contains(verb)) {
				throw new IllegalArgumentException("unknown action token: " + content);
			}
			if (!rest.equals("item") && !rest.startsWith("item ")) {
				throw new IllegalArgumentException("expected 'item' after '" + verb + "': " + content);
			}
			rest = rest.equals("item") ? "" : rest.substring("item ".length()).strip();

			boolean shift = false;
			if (rest.equals("shift") || rest.startsWith("shift ")) {
				shift = true;
				rest = rest.equals("shift") ? "" : rest.substring("shift ".length()).strip();
			}
			if (shift && verb.equals("mclick")) {
				throw new IllegalArgumentException("mclick cannot be combined with shift");
			}
			Match match = extractMatch(rest);
			tokens.add(new RoutinizeStep.InventoryClick(verb, shift, match.name(), match.lore()));
		}
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("action requires at least one token");
		}
		return new RoutinizeStep.Action(tokens);
	}

	private static RoutinizeStep parseIfBlock(String line, Cursor cursor, boolean insideLoop) {
		Condition condition = extractCondition(line, "if");
		Match match = extractMatch(condition.body());
		List<RoutinizeStep> thenSteps = parseBlock(cursor, true, insideLoop);
		List<RoutinizeStep> elseSteps = List.of();
		List<ElseIf> elseIfs = new ArrayList<>();
		while (!cursor.atEnd()) {
			String peek = cursor.peek();
			if (peek.startsWith("elseif")) {
				String elseifLine = cursor.next();
				Condition elseifCondition = extractCondition(elseifLine, "elseif");
				ElseIf elseIf = new ElseIf(extractMatch(elseifCondition.body()), elseifCondition.negated(), parseBlock(cursor, true, insideLoop));
				elseIfs.add(elseIf);
				continue;
			}
			if (peek.equals("else")) {
				cursor.next();
				elseSteps = parseBlock(cursor, true, insideLoop);
				break;
			}
			break;
		}
		expectEnd(cursor);
		for (int i = elseIfs.size() - 1; i >= 0; i--) {
			ElseIf elseIf = elseIfs.get(i);
			elseSteps = List.of(new RoutinizeStep.IfPresent(elseIf.match.name(), elseIf.match.lore(), elseIf.negated, elseIf.thenSteps, elseSteps));
		}
		return new RoutinizeStep.IfPresent(match.name(), match.lore(), condition.negated(), thenSteps, elseSteps);
	}

	private static Condition extractCondition(String line, String keyword) {
		String rest = line.substring(keyword.length()).strip();
		boolean negated = false;
		if (rest.equals("not") || rest.startsWith("not ")) {
			negated = true;
			rest = rest.equals("not") ? "" : rest.substring("not ".length()).strip();
		}
		if (!rest.startsWith("(") || !rest.endsWith(")")) {
			throw new IllegalArgumentException("expected '(' and ')' around condition: " + line);
		}
		return new Condition(rest.substring(1, rest.length() - 1).strip(), negated);
	}

	private static void expectEnd(Cursor cursor) {
		if (cursor.atEnd() || !cursor.next().equals("end")) {
			throw new IllegalArgumentException("expected 'end'");
		}
	}

	private static Match extractMatch(String line) {
		String name = null;
		String lore = null;
		Matcher m = KV.matcher(line);
		while (m.find()) {
			if (m.group(1).equals("name")) name = m.group(2);
			else lore = m.group(2);
		}
		return new Match(name, lore);
	}

	private record Match(String name, String lore) {}

	private record Condition(String body, boolean negated) {}

	private record ElseIf(Match match, boolean negated, List<RoutinizeStep> thenSteps) {}

	private static final class Cursor {
		private final List<String> lines;
		private int index = 0;

		private Cursor(List<String> lines) {
			this.lines = lines;
		}

		boolean atEnd() {
			return index >= lines.size();
		}

		String peek() {
			return lines.get(index);
		}

		String next() {
			return lines.get(index++);
		}
	}
}