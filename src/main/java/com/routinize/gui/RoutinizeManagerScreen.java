package com.routinize.gui;

import com.routinize.routinize.RoutinizeConfig;
import com.routinize.routinize.RoutinizeManager;
import com.routinize.routinize.RoutinizeSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RoutinizeManagerScreen extends Screen {

	public RoutinizeManagerScreen() {
		super(Component.literal("Routinize - My Routines"));
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("New Routine"), button -> {
			Minecraft.getInstance().setScreen(new RoutinizeEditorScreen(null, true));
		}).bounds(width / 2 - 150, 20, 140, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
			.bounds(width / 2 + 10, 20, 140, 20)
			.build());

		addRenderableWidget(Button.builder(Component.literal("Settings"), button -> {
			Minecraft.getInstance().setScreen(new SettingsScreen());
		}).bounds(width - 90, 10, 80, 20).build());

		int row = 1;
		for (RoutinizeSlot slot : RoutinizeManager.INSTANCE.profiles()) {
			int y = 80 + (row - 1) * 24;
			addRenderableWidget(Button.builder(Component.literal("Edit"), button -> {
				Minecraft.getInstance().setScreen(new RoutinizeEditorScreen(slot, false));
			}).bounds(width / 2 + 40, y, 60, 20).build());

			addRenderableWidget(Button.builder(Component.literal("Delete"), button -> {
				RoutinizeManager.INSTANCE.deleteProfile(slot);
				RoutinizeConfig.save();
				Minecraft.getInstance().setScreen(new RoutinizeManagerScreen());
			}).bounds(width / 2 + 110, y, 60, 20).build());

			row++;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int row = 1;
		for (RoutinizeSlot slot : RoutinizeManager.INSTANCE.profiles()) {
			int y = 80 + (row - 1) * 24;
			String label = slot.name();
			if (slot.toggleKeyCode() != -1) {
				label += " [" + slot.toggleKeyName() + "]";
			}
			if (slot.pauseKeyCode() != -1) {
				label += " (pause " + slot.pauseKeyName() + ")";
			}
			graphics.text(font, label, width / 2 - 150, y + 6, 0xFFFFFFFF, true);
			row++;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}