package com.github.wheredidmydivgo.routinize.routinize;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftRoutinizeState implements RoutinizeState {

	public static final MinecraftRoutinizeState INSTANCE = new MinecraftRoutinizeState();

	private MinecraftRoutinizeState() {}

	private AbstractContainerMenu currentMenu() {
		Minecraft client = Minecraft.getInstance();
		if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
			return containerScreen.getMenu();
		}
		return null;
	}

	@Override
	public boolean screenOpen() {
		return currentMenu() != null;
	}

	@Override
	public boolean anyScreenOpen() {
		return Minecraft.getInstance().screen != null;
	}

	@Override
	public List<String> fingerprint() {
		AbstractContainerMenu menu = currentMenu();
		List<String> out = new ArrayList<>();
		if (menu == null) return out;
		for (Slot slot : menu.slots) {
			out.add(describe(slot.getItem()));
		}
		return out;
	}

	@Override
	public boolean matchExists(String nameContains, String loreContains) {
		AbstractContainerMenu menu = currentMenu();
		if (menu == null) return false;
		for (Slot slot : menu.slots) {
			if (matches(slot.getItem(), nameContains, loreContains)) return true;
		}
		return matches(menu.getCarried(), nameContains, loreContains);
	}

	@Override
	public boolean clickSlot(String button, boolean shift, String nameContains, String loreContains) {
		Minecraft client = Minecraft.getInstance();
		AbstractContainerMenu menu = currentMenu();
		if (menu == null || client.gameMode == null || client.player == null) return false;
		int rawButton = button.equals("rclick") ? 1 : 0;
		ContainerInput inputType = button.equals("mclick") ? ContainerInput.CLONE : shift ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP;
		for (Slot slot : menu.slots) {
			if (matches(slot.getItem(), nameContains, loreContains)) {
				client.gameMode.handleContainerInput(menu.containerId, slot.index, rawButton, inputType, client.player);
				return true;
			}
		}
		return false;
	}

	@Override
	public void closeScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.closeContainer();
		}
	}

	@Override
	public void runCommand(String command) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			String stripped = command.startsWith("/") ? command.substring(1) : command;
			client.player.connection.sendCommand(stripped);
		}
	}

	@Override
	public void setKeyState(String key, boolean down) {
		KeyActions.set(key, down);
	}

	@Override
	public void sendFeedback(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal("[Routinize] " + message));
		}
	}

	private boolean matches(ItemStack stack, String nameContains, String loreContains) {
		if (stack.isEmpty()) return false;
		if (nameContains != null && !stack.getHoverName().getString().contains(nameContains)) return false;
		if (loreContains != null && !loreContains(stack, loreContains)) return false;
		return true;
	}

	private boolean loreContains(ItemStack stack, String needle) {
		var lore = stack.get(DataComponents.LORE);
		if (lore == null) return false;
		for (Component line : lore.lines()) {
			if (line.getString().contains(needle)) return true;
		}
		return false;
	}

	private String describe(ItemStack stack) {
		if (stack.isEmpty()) return "";
		StringBuilder sb = new StringBuilder(stack.getHoverName().getString());
		var lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				sb.append('|').append(line.getString());
			}
		}
		return sb.toString();
	}
}