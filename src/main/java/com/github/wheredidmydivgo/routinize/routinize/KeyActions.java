package com.github.wheredidmydivgo.routinize.routinize;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.function.Function;

public final class KeyActions {

	private static final Map<String, Function<Minecraft, KeyMapping>> BINDINGS = Map.of(
		"forward", mc -> mc.options.keyUp,
		"backward", mc -> mc.options.keyDown,
		"left", mc -> mc.options.keyLeft,
		"right", mc -> mc.options.keyRight,
		"jump", mc -> mc.options.keyJump,
		"sneak", mc -> mc.options.keyShift,
		"lclick", mc -> mc.options.keyAttack,
		"rclick", mc -> mc.options.keyUse
	);

	private KeyActions() {}

	public static boolean isValid(String key) {
		return BINDINGS.containsKey(key.toLowerCase());
	}

	public static void set(String key, boolean down) {
		Function<Minecraft, KeyMapping> lookup = BINDINGS.get(key.toLowerCase());
		if (lookup == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (down && mc.screen != null) return;
		lookup.apply(mc).setDown(down);
	}
}