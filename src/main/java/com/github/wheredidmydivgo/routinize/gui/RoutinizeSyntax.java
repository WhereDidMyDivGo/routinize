package com.github.wheredidmydivgo.routinize.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class RoutinizeSyntax {

	public enum StatementType {
		COMMAND, WAIT, ACTION, CONDITIONAL, LOOP, OTHER
	}

	private static final Map<StatementType, Integer> DEFAULT_COLORS = Map.of(
		StatementType.COMMAND, 0xFF98C379,
		StatementType.WAIT, 0xFFE5C07B,
		StatementType.ACTION, 0xFF61AFEF,
		StatementType.CONDITIONAL, 0xFFC586C0,
		StatementType.LOOP, 0xFFD19A66,
		StatementType.OTHER, 0xFFD4D4D4
	);

	public record LineInfo(int indentLevel, StatementType type) {}

	public record BlockSpan(int startLine, int endLine, StatementType type) {}

	public record Analysis(List<LineInfo> lines, List<BlockSpan> blocks) {}

	private RoutinizeSyntax() {}

	public static int defaultColor(StatementType type) {
		return DEFAULT_COLORS.get(type);
	}

	public static int indentDepthAfter(List<String> lines, int lineIndex) {
		Analysis analysis = analyze(lines.subList(0, Math.min(lineIndex + 1, lines.size())));
		Deque<Integer> unused = null;
		return analysis.lines().isEmpty() ? 0 : openDepthAfterLast(lines.subList(0, Math.min(lineIndex + 1, lines.size())));
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

			int renderDepth = isCloser ? Math.max(0, stack.size() - 1) : stack.size();
			StatementType type = classify(stripped, stack);
			lines.add(new LineInfo(renderDepth, type));

			if (stripped.equals("end")) {
				if (!stack.isEmpty()) {
					OpenBlock opened = stack.pop();
					blocks.add(new BlockSpan(opened.startLine(), i, opened.type()));
				}
			} else if (isOpener(stripped)) {
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
		return stripped.startsWith("if") || stripped.equals("loop") || stripped.startsWith("loop ")
			|| stripped.startsWith("loop_until");
	}

	private static StatementType classify(String stripped, Deque<OpenBlock> stack) {
		if (stripped.startsWith("command")) return StatementType.COMMAND;
		if (stripped.startsWith("wait")) return StatementType.WAIT;
		if (stripped.startsWith("action")) return StatementType.ACTION;
		if (stripped.startsWith("if") || stripped.startsWith("elseif") || stripped.equals("else")) return StatementType.CONDITIONAL;
		if (stripped.equals("loop") || stripped.startsWith("loop ") || stripped.startsWith("loop_until")) return StatementType.LOOP;
		if (stripped.equals("end")) {
			OpenBlock top = stack.peek();
			return top == null ? StatementType.OTHER : top.type();
		}
		return StatementType.OTHER;
	}

	private record OpenBlock(int startLine, StatementType type) {}
}