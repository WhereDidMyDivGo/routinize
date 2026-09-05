package com.github.wheredidmydivgo.routinize.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class RoutinizeSyntax {

	public enum StatementType {
		COMMAND, WAIT, ACTION, CONDITIONAL, LOOP, FLOW, OTHER
	}

	private static final int IMPERATIVE_COLOR = 0xFF98C379;
	private static final int ARGUMENT_COLOR = 0xFF56B6C2;

	private static final Map<StatementType, Integer> DEFAULT_COLORS = Map.of(
		StatementType.COMMAND, IMPERATIVE_COLOR,
		StatementType.WAIT, 0xFFE5C07B,
		StatementType.ACTION, IMPERATIVE_COLOR,
		StatementType.CONDITIONAL, 0xFFC586C0,
		StatementType.LOOP, 0xFFD19A66,
		StatementType.FLOW, 0xFFE06C75,
		StatementType.OTHER, 0xFFD4D4D4
	);

	public record LineInfo(int indentLevel, StatementType type, int argumentOffset, boolean isBlockBoundary) {}

	public record BlockSpan(int startLine, int endLine, StatementType type) {}

	public record Analysis(List<LineInfo> lines, List<BlockSpan> blocks) {}

	private RoutinizeSyntax() {}

	public static int defaultColor(StatementType type) {
		return DEFAULT_COLORS.get(type);
	}

	public static int argumentColor() {
		return ARGUMENT_COLOR;
	}

	public static int indentDepthAfter(List<String> lines, int lineIndex) {
		return openDepthAfterLast(lines.subList(0, Math.min(lineIndex + 1, lines.size())));
	}

	public static boolean opensBlockAlreadyClosed(List<String> lines, int openerLineIndex) {
		Deque<Integer> stack = new ArrayDeque<>();
		for (int i = 0; i <= openerLineIndex && i < lines.size(); i++) {
			String stripped = lines.get(i).strip();
			if (stripped.equals("end")) {
				if (!stack.isEmpty()) stack.pop();
			} else if (isOpener(stripped)) {
				stack.push(i);
			}
		}
		if (stack.isEmpty()) return false;
		for (int i = openerLineIndex + 1; i < lines.size(); i++) {
			String stripped = lines.get(i).strip();
			if (stripped.equals("end")) {
				if (!stack.isEmpty()) stack.pop();
				if (stack.isEmpty()) return true;
			} else if (isOpener(stripped)) {
				stack.push(i);
			}
		}
		return false;
	}

	public static BlockSpan innermostBlockContaining(Analysis analysis, int lineIndex) {
		BlockSpan best = null;
		for (BlockSpan span : analysis.blocks()) {
			if (lineIndex < span.startLine() || lineIndex > span.endLine()) continue;
			if (best == null || (span.endLine() - span.startLine()) < (best.endLine() - best.startLine())) {
				best = span;
			}
		}
		return best;
	}

	public static Analysis analyze(List<String> rawLines) {
		List<LineInfo> lines = new ArrayList<>();
		List<BlockSpan> blocks = new ArrayList<>();
		Deque<OpenBlock> stack = new ArrayDeque<>();

		for (int i = 0; i < rawLines.size(); i++) {
			String stripped = rawLines.get(i).strip();
			boolean isCloser = stripped.equals("end") || stripped.equals("else") || stripped.startsWith("elseif");
			boolean isOpener = isOpener(stripped);

			int renderDepth = isCloser ? Math.max(0, stack.size() - 1) : stack.size();
			StatementType type = classify(stripped, stack);
			int argumentOffset = argumentStartIndex(stripped);
			lines.add(new LineInfo(renderDepth, type, argumentOffset, isCloser || isOpener));

			if (stripped.equals("end")) {
				if (!stack.isEmpty()) {
					OpenBlock opened = stack.pop();
					blocks.add(new BlockSpan(opened.startLine(), i, opened.type()));
				}
			} else if (isOpener) {
				stack.push(new OpenBlock(i, type));
			}
		}

		return new Analysis(lines, blocks);
	}

	private static int openDepthAfterLast(List<String> rawLines) {
		Deque<OpenBlock> stack = new ArrayDeque<>();
		for (String raw : rawLines) {
			String stripped = raw.strip();
			if (stripped.equals("end")) {
				if (!stack.isEmpty()) stack.pop();
			} else if (isOpener(stripped)) {
				stack.push(new OpenBlock(0, classify(stripped, stack)));
			}
		}
		return stack.size();
	}

	private static boolean isOpener(String stripped) {
		return stripped.startsWith("if") || stripped.startsWith("while") || isLoop(stripped);
	}

	private static boolean isLoop(String stripped) {
		return stripped.equals("loop") || (stripped.startsWith("loop") && stripped.substring(4).strip().startsWith("("));
	}

	private static int argumentStartIndex(String stripped) {
		int paren = stripped.indexOf('(');
		int bracket = stripped.indexOf('[');
		if (paren < 0) return bracket;
		if (bracket < 0) return paren;
		return Math.min(paren, bracket);
	}

	private static StatementType classify(String stripped, Deque<OpenBlock> stack) {
		if (stripped.startsWith("command")) return StatementType.COMMAND;
		if (stripped.startsWith("wait")) return StatementType.WAIT;
		if (stripped.startsWith("action")) return StatementType.ACTION;
		if (stripped.startsWith("if") || stripped.startsWith("elseif") || stripped.equals("else")) return StatementType.CONDITIONAL;
		if (stripped.startsWith("while") || isLoop(stripped)) return StatementType.LOOP;
		if (stripped.equals("stop") || stripped.equals("continue") || stripped.equals("break") || stripped.equals("close")) return StatementType.FLOW;
		if (stripped.equals("end")) {
			OpenBlock top = stack.peek();
			return top == null ? StatementType.OTHER : top.type();
		}
		return StatementType.OTHER;
	}

	private record OpenBlock(int startLine, StatementType type) {}
}