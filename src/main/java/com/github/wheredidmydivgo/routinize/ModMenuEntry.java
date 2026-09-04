package com.github.wheredidmydivgo.routinize;

import com.github.wheredidmydivgo.routinize.gui.RoutinizeManagerScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuEntry implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new RoutinizeManagerScreen();
	}
}