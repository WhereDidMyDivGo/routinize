package com.github.wheredidmydivgo.routinize.gui;

import com.github.wheredidmydivgo.routinize.routinize.MinecraftRoutinizeState;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeConfig;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeManager;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RoutinizeEditorScreen extends Screen {

	private static final int LINE_HEIGHT = 9;

	private enum Capture { NONE, TOGGLE, PAUSE }

	private final RoutinizeSlot slot;
	private final boolean isNew;
	private final String initialName;
	private int toggleKeyCode;
	private int pauseKeyCode;
	private Capture capturing = Capture.NONE;
	private EditBox nameField;
	private Button toggleBindButton;
	private Button pauseBindButton;
	private MultiLineEditBox editor;
	private String lastEditorValue = "";

	public RoutinizeEditorScreen(RoutinizeSlot slot, boolean isNew) {
		super(Component.literal(slot == null ? "Routinize - New Routine" : "Routinize - " + slot.name()));
		this.slot = slot;
		this.isNew = isNew;
		this.initialName = slot == null ? "" : slot.name();
		this.toggleKeyCode = slot == null ? -1 : slot.toggleKeyCode();
		this.pauseKeyCode = slot == null ? -1 : slot.pauseKeyCode();
	}

	@Override
	protected void init() {
		nameField = new EditBox(font, width / 2 - 150, 20, 300, 20, Component.literal(""));
		nameField.setMaxLength(64);
		nameField.setValue(initialName);
		nameField.setTextColor(0xFFFFFFFF);
		nameField.setTextColorUneditable(0xFF888888);
		nameField.setTextShadow(true);
		nameField.setHint(Component.literal("Enter routine title"));
		addRenderableWidget(nameField);

		toggleBindButton = Button.builder(Component.literal("Run key: " + keyLabel(toggleKeyCode)), b -> {
			capturing = Capture.TOGGLE;
			b.setMessage(Component.literal("Press a key..."));
		}).bounds(width / 2 - 150, 50, 140, 20).build();
		addRenderableWidget(toggleBindButton);

		addRenderableWidget(Button.builder(Component.literal("Unbind"), b -> {
			toggleKeyCode = -1;
			toggleBindButton.setMessage(Component.literal("Run key: " + keyLabel(toggleKeyCode)));
		}).bounds(width / 2 + 10, 50, 140, 20).build());

		pauseBindButton = Button.builder(Component.literal("Pause key: " + keyLabel(pauseKeyCode)), b -> {
			capturing = Capture.PAUSE;
			b.setMessage(Component.literal("Press a key..."));
		}).bounds(width / 2 - 150, 75, 140, 20).build();
		addRenderableWidget(pauseBindButton);

		addRenderableWidget(Button.builder(Component.literal("Unbind"), b -> {
			pauseKeyCode = -1;
			pauseBindButton.setMessage(Component.literal("Pause key: " + keyLabel(pauseKeyCode)));
		}).bounds(width / 2 + 10, 75, 140, 20).build());

		editor = MultiLineEditBox.builder()
			.setX(width / 2 - 150)
			.setY(105)
			.setPlaceholder(Component.literal("Enter routine script"))
			.setTextColor(0xFFFFFFFF)
			.setTextShadow(true)
			.setCursorColor(0xFFFFFFFF)
			.setShowBackground(true)
			.setShowDecorations(true)
			.build(font, 300, 135, Component.literal("Routine lines"));
		editor.setCharacterLimit(8192);
		editor.setValue(slot == null ? "" : slot.sourceText());
		lastEditorValue = editor.getValue();
		editor.setValueListener(this::onEditorValueChanged);
		addRenderableWidget(editor);

		if (isNew) {
			setInitialFocus(nameField);
			nameField.setFocused(true);
			nameField.setCanLoseFocus(true);
		} else {
			setInitialFocus(editor);
			editor.setFocused(true);
		}

		addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
			.bounds(width / 2 - 150, height - 45, 140, 20)
			.build());

		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> Minecraft.getInstance().setScreen(new RoutinizeManagerScreen()))
			.bounds(width / 2 + 10, height - 45, 140, 20)
			.build());
	}

	private void onEditorValueChanged(String newValue) {
		String previous = lastEditorValue;
		lastEditorValue = newValue;

		MultilineTextField textField = MultiLineEditBoxAccess.textField(editor);
		int cursor = textField.cursor();

		boolean isNewlineInsertion = newValue.length() == previous.length() + 1
			&& cursor > 0 && cursor <= newValue.length() && newValue.charAt(cursor - 1) == '\n';
		if (!isNewlineInsertion) return;

		List<String> lines = List.of(newValue.split("\n", -1));
		int newLineIndex = countNewlinesBefore(newValue, cursor);
		if (newLineIndex <= 0 || newLineIndex > lines.size()) return;
		int completedLineIndex = newLineIndex - 1;

		int depth = RoutinizeSyntax.indentDepthAfter(lines, completedLineIndex);
		String indent = "    ".repeat(Math.max(0, depth));

		String completedLine = lines.get(completedLineIndex).strip();
		boolean opensBlock = completedLine.startsWith("if") || completedLine.equals("loop")
			|| completedLine.startsWith("loop ") || completedLine.startsWith("loop_until");

		if (!indent.isEmpty()) {
			textField.insertText(indent);
		}

		if (opensBlock) {
			int blankLinePosition = textField.cursor();
			String closerIndent = "    ".repeat(Math.max(0, depth - 1));
			textField.insertText("\n" + closerIndent + "end");
			textField.seekCursor(Whence.ABSOLUTE, blankLinePosition);
		}

		lastEditorValue = editor.getValue();
	}

	private static int countNewlinesBefore(String value, int index) {
		int count = 0;
		for (int i = 0; i < index && i < value.length(); i++) {
			if (value.charAt(i) == '\n') count++;
		}
		return count;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		renderSyntaxOverlay(graphics);
	}

	private void renderSyntaxOverlay(GuiGraphicsExtractor graphics) {
		List<String> lines = List.of(editor.getValue().split("\n", -1));
		if (editor.getInnerHeight() / LINE_HEIGHT != lines.size()) return;

		RoutinizeSyntax.Analysis analysis = RoutinizeSyntax.analyze(lines);
		int left = MultiLineEditBoxAccess.innerLeft(editor);
		int top = MultiLineEditBoxAccess.innerTop(editor) - (int) editor.scrollAmount();
		int visibleTop = editor.getY();
		int visibleBottom = editor.getY() + editor.getHeight();

		for (RoutinizeSyntax.BlockSpan span : analysis.blocks()) {
			int depth = analysis.lines().get(span.startLine()).indentLevel();
			int connectorX = left + font.width(" ".repeat(depth * 4));
			for (int i = span.startLine(); i <= span.endLine(); i++) {
				RoutinizeSyntax.LineInfo lineInfo = analysis.lines().get(i);
				boolean skip = i == span.startLine() || i == span.endLine()
					|| (lineInfo.indentLevel() == depth && lineInfo.isBlockBoundary());
				if (skip) continue;
				int rowTop = top + i * LINE_HEIGHT;
				int rowBottom = rowTop + LINE_HEIGHT;
				if (rowBottom < visibleTop || rowTop > visibleBottom) continue;
				graphics.fill(connectorX, Math.max(rowTop, visibleTop), connectorX + 1, Math.min(rowBottom, visibleBottom), 0xFF555555);
			}
		}

		for (int i = 0; i < lines.size(); i++) {
			int y = top + i * LINE_HEIGHT;
			if (y + LINE_HEIGHT < visibleTop || y > visibleBottom) continue;
			String raw = lines.get(i);
			RoutinizeSyntax.LineInfo lineInfo = analysis.lines().get(i);
			int primaryColor = RoutinizeSyntax.defaultColor(lineInfo.type());
			if (lineInfo.argumentOffset() < 0) {
				graphics.text(font, raw, left, y, primaryColor, true);
			} else {
				int leadingWs = raw.length() - raw.stripLeading().length();
				int splitAt = leadingWs + lineInfo.argumentOffset();
				String keywordPart = raw.substring(0, splitAt);
				String argumentPart = raw.substring(splitAt);
				graphics.text(font, keywordPart, left, y, primaryColor, true);
				graphics.text(font, argumentPart, left + font.width(keywordPart), y, RoutinizeSyntax.argumentColor(), true);
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (capturing == Capture.TOGGLE) {
			toggleKeyCode = event.key();
			capturing = Capture.NONE;
			toggleBindButton.setMessage(Component.literal("Run key: " + keyLabel(toggleKeyCode)));
			return true;
		}
		if (capturing == Capture.PAUSE) {
			pauseKeyCode = event.key();
			capturing = Capture.NONE;
			pauseBindButton.setMessage(Component.literal("Pause key: " + keyLabel(pauseKeyCode)));
			return true;
		}
		return super.keyPressed(event);
	}

	private void save() {
		String name = nameField.getValue().strip();
		if (name.isEmpty()) {
			message("Name is required");
			return;
		}

		RoutinizeSlot existing = RoutinizeManager.INSTANCE.findProfile(name);
		if (slot == null) {
			if (existing != null) {
				message("Profile already exists: " + name);
				return;
			}
			RoutinizeSlot newSlot = RoutinizeManager.INSTANCE.createProfile(name);
			newSlot.setToggleKeyCode(toggleKeyCode);
			newSlot.setPauseKeyCode(pauseKeyCode);
			try {
				newSlot.applySource(editor.getValue());
			} catch (IllegalArgumentException e) {
				RoutinizeManager.INSTANCE.deleteProfile(newSlot);
				message("Error: " + e.getMessage());
				return;
			}
		} else {
			if (existing != null && existing != slot) {
				message("Profile already exists: " + name);
				return;
			}
			slot.setName(name);
			slot.setToggleKeyCode(toggleKeyCode);
			slot.setPauseKeyCode(pauseKeyCode);
			try {
				slot.applySource(editor.getValue());
			} catch (IllegalArgumentException e) {
				message("Error: " + e.getMessage());
				return;
			}
		}

		RoutinizeConfig.save();
		message("Saved profile: " + name);

		Minecraft.getInstance().setScreen(new RoutinizeManagerScreen());
	}

	private String keyLabel(int keyCode) {
		if (keyCode == -1) return "unbound";
		String label = RoutinizeSlot.keyName(keyCode);
		return label == null ? "Key " + keyCode : label;
	}

	private void message(String text) {
		MinecraftRoutinizeState.INSTANCE.sendFeedback(text);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}