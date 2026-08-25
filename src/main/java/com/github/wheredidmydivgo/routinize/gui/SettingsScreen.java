package com.github.wheredidmydivgo.routinize.gui;

import com.github.wheredidmydivgo.routinize.routinize.MinecraftRoutinizeState;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeConfig;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {

	private EditBox clickRetryField;
	private boolean chatFeedbackEnabled;
	private Button chatFeedbackButton;

	public SettingsScreen() {
		super(Component.literal("Routinize - Settings"));
	}

	@Override
	protected void init() {
		clickRetryField = new EditBox(font, width / 2 - 100, 50, 200, 20, Component.literal(""));
		clickRetryField.setMaxLength(6);
		clickRetryField.setValue(Integer.toString(RoutinizeSettings.INSTANCE.clickRetryTimeoutMs()));
		clickRetryField.setTextColor(0xFFFFFFFF);
		clickRetryField.setTextShadow(true);
		addRenderableWidget(clickRetryField);

		chatFeedbackEnabled = RoutinizeSettings.INSTANCE.chatFeedbackEnabled();
		chatFeedbackButton = Button.builder(Component.literal("Chat feedback: " + (chatFeedbackEnabled ? "ON" : "OFF")), b -> {
			chatFeedbackEnabled = !chatFeedbackEnabled;
			b.setMessage(Component.literal("Chat feedback: " + (chatFeedbackEnabled ? "ON" : "OFF")));
		}).bounds(width / 2 - 100, 90, 200, 20).build();
		addRenderableWidget(chatFeedbackButton);

		addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
			.bounds(width / 2 - 100, height - 45, 95, 20)
			.build());

		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> Minecraft.getInstance().setScreen(new RoutinizeManagerScreen()))
			.bounds(width / 2 + 5, height - 45, 95, 20)
			.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, "Click retry timeout (ms)", width / 2 - 100, 38, 0xFFFFFFFF, true);
	}

	private void save() {
		int value;
		try {
			value = Integer.parseInt(clickRetryField.getValue().strip());
		} catch (NumberFormatException e) {
			MinecraftRoutinizeState.INSTANCE.sendFeedback("Invalid number: " + clickRetryField.getValue());
			return;
		}
		RoutinizeSettings.INSTANCE.setClickRetryTimeoutMs(value);
		RoutinizeSettings.INSTANCE.setChatFeedbackEnabled(chatFeedbackEnabled);
		RoutinizeConfig.save();
		MinecraftRoutinizeState.INSTANCE.sendFeedback("Settings saved");
		Minecraft.getInstance().setScreen(new RoutinizeManagerScreen());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}