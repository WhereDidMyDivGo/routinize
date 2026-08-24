package com.routinize.routinize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RoutinizeConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger("routinize");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("routinize.json");

	private RoutinizeConfig() {}

	public static void save() {
		ConfigData data = new ConfigData();
		data.settings.clickRetryTimeoutMs = RoutinizeSettings.INSTANCE.clickRetryTimeoutMs();
		for (RoutinizeSlot slot : RoutinizeManager.INSTANCE.profiles()) {
			RoutinizeEntry entry = new RoutinizeEntry();
			entry.name = slot.name();
			entry.toggleKeyCode = slot.toggleKeyCode();
			entry.pauseKeyCode = slot.pauseKeyCode();
			entry.source = slot.sourceText();
			data.routines.add(entry);
		}
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save routinize config", e);
		}
	}

	public static void load() {
		if (!Files.exists(PATH)) return;
		ConfigData data;
		try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			data = GSON.fromJson(reader, ConfigData.class);
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("Failed to load routinize config", e);
			return;
		}
		if (data == null) return;
		if (data.settings != null) {
			RoutinizeSettings.INSTANCE.setClickRetryTimeoutMs(data.settings.clickRetryTimeoutMs);
		}
		if (data.routines == null) return;
		for (RoutinizeEntry entry : data.routines) {
			if (entry.name == null || entry.name.isBlank()) continue;
			RoutinizeSlot slot;
			try {
				slot = RoutinizeManager.INSTANCE.createProfile(entry.name, entry.toggleKeyCode, entry.pauseKeyCode);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Skipping duplicate routine profile '{}'", entry.name);
				continue;
			}
			try {
				slot.applySource(entry.source == null ? "" : entry.source);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Failed to parse routine '{}': {}", entry.name, e.getMessage());
				RoutinizeManager.INSTANCE.deleteProfile(slot);
			}
		}
	}

	private static final class ConfigData {
		SettingsData settings = new SettingsData();
		List<RoutinizeEntry> routines = new ArrayList<>();
	}

	private static final class SettingsData {
		int clickRetryTimeoutMs = 5000;
	}

	private static final class RoutinizeEntry {
		String name;
		int toggleKeyCode = -1;
		int pauseKeyCode = -1;
		String source = "";
	}
}