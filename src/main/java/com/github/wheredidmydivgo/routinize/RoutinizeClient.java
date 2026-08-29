package com.github.wheredidmydivgo.routinize;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.github.wheredidmydivgo.routinize.gui.RoutinizeEditorScreen;
import com.github.wheredidmydivgo.routinize.gui.RoutinizeManagerScreen;
import com.github.wheredidmydivgo.routinize.routinize.MinecraftRoutinizeState;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeConfig;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeManager;
import com.github.wheredidmydivgo.routinize.routinize.RoutinizeSlot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RoutinizeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RoutinizeConfig.load();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			dispatcher.register(ClientCommands.literal("routinize")
				.executes(ctx -> {
					Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RoutinizeManagerScreen()));
					return 1;
				})
				.then(ClientCommands.literal("edit")
					.then(ClientCommands.argument("name", StringArgumentType.greedyString())
						.executes(ctx -> {
							String name = StringArgumentType.getString(ctx, "name");
							Minecraft.getInstance().execute(() -> {
								RoutinizeSlot slot = RoutinizeManager.INSTANCE.findProfile(name);
								if (slot == null) {
									MinecraftRoutinizeState.INSTANCE.sendFeedback("Profile not found: " + name);
									return;
								}
								Minecraft.getInstance().setScreen(new RoutinizeEditorScreen(slot, false));
							});
							return 1;
						}))
				)
				.then(ClientCommands.literal("delete")
					.then(ClientCommands.argument("name", StringArgumentType.greedyString())
						.executes(ctx -> {
							String name = StringArgumentType.getString(ctx, "name");
							Minecraft.getInstance().execute(() -> {
								RoutinizeSlot slot = RoutinizeManager.INSTANCE.findProfile(name);
								if (slot == null) {
									MinecraftRoutinizeState.INSTANCE.sendFeedback("Profile not found: " + name);
									return;
								}
								RoutinizeManager.INSTANCE.deleteProfile(slot);
								RoutinizeConfig.save();
								MinecraftRoutinizeState.INSTANCE.sendFeedback("Deleted profile: " + name);
							});
							return 1;
						}))
				)
				.then(ClientCommands.literal("dump")
					.executes(ctx -> {
						Minecraft.getInstance().execute(() -> {
							List<String> lines = MinecraftRoutinizeState.INSTANCE.fingerprint();
							if (lines.isEmpty()) {
								MinecraftRoutinizeState.INSTANCE.sendFeedback("No open menu or no items detected");
								return;
							}
							try {
								Path file = writeDump(lines);
								MinecraftRoutinizeState.INSTANCE.sendFeedback("Dumped " + lines.size() + " slots to .minecraft/routinize-dumps/" + file.getFileName());
								openDumpFile(file);
							} catch (IOException e) {
								MinecraftRoutinizeState.INSTANCE.sendFeedback("Failed to write dump: " + e.getMessage());
							}
						});
						return 1;
					})
				)
				.then(ClientCommands.literal("reload")
					.executes(ctx -> {
						Minecraft.getInstance().execute(() -> {
							RoutinizeManager.INSTANCE.reset();
							RoutinizeConfig.load();
							MinecraftRoutinizeState.INSTANCE.sendFeedback("Reloaded (" + RoutinizeManager.INSTANCE.profiles().size() + " routines)");
						});
						return 1;
					})
				)
			);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> RoutinizeManager.INSTANCE.tick(MinecraftRoutinizeState.INSTANCE));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RoutinizeManager.INSTANCE.onDisconnect(MinecraftRoutinizeState.INSTANCE));

		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("routinize", "slot_states"), new HudElement() {
			@Override
			public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
				Minecraft mc = Minecraft.getInstance();
				var font = mc.font;
				StringBuilder sb = new StringBuilder("Routinize");
				for (RoutinizeSlot slot : RoutinizeManager.INSTANCE.profiles()) {
					char state = slot.isPaused() ? 'P' : slot.isRunning() ? 'R' : '-';
					sb.append(' ').append(slot.name()).append(':').append(state);
				}
				graphics.text(font, sb.toString(), 4, 4, 0xFFFFFF);
			}
		});
	}

	private static Path writeDump(List<String> lines) throws IOException {
		Path dir = FabricLoader.getInstance().getGameDir().resolve("routinize-dumps");
		Files.createDirectories(dir);
		String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
		Path file = dir.resolve("dump_" + timestamp + ".txt");

		StringBuilder sb = new StringBuilder();
		sb.append("Routinize dump - ").append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())).append('\n');
		sb.append(lines.size()).append(" slots\n\n");
		for (int i = 0; i < lines.size(); i++) {
			String entry = lines.get(i);
			if (entry.isEmpty()) {
				sb.append("slot ").append(i).append(": (empty)\n\n");
				continue;
			}
			String[] parts = entry.split("\\|");
			sb.append("slot ").append(i).append(": ").append(parts[0]).append('\n');
			for (int j = 1; j < parts.length; j++) {
				sb.append("  lore: ").append(parts[j]).append('\n');
			}
			sb.append('\n');
		}

		Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
		return file;
	}

	private static void openDumpFile(Path file) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			MinecraftRoutinizeState.INSTANCE.sendFeedback("Can't auto-open on this system, file's still at .minecraft/routinize-dumps/" + file.getFileName());
			return;
		}
		try {
			Desktop.getDesktop().open(file.toFile());
		} catch (IOException e) {
			MinecraftRoutinizeState.INSTANCE.sendFeedback("Failed to open dump file: " + e.getMessage());
		}
	}
}