package com.github.wheredidmydivgo.routinize.gui;

import net.minecraft.client.gui.components.MultiLineEditBox;

import java.lang.reflect.Method;

public final class MultiLineEditBoxAccess {

	private static final Method GET_INNER_LEFT = findMethod("getInnerLeft");
	private static final Method GET_INNER_TOP = findMethod("getInnerTop");

	private MultiLineEditBoxAccess() {}

	public static int innerLeft(MultiLineEditBox editor) {
		return invokeInt(GET_INNER_LEFT, editor);
	}

	public static int innerTop(MultiLineEditBox editor) {
		return invokeInt(GET_INNER_TOP, editor);
	}

	private static int invokeInt(Method method, MultiLineEditBox editor) {
		try {
			return (int) method.invoke(editor);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Method findMethod(String name) {
		for (Class<?> type = MultiLineEditBox.class; type != null; type = type.getSuperclass()) {
			try {
				Method method = type.getDeclaredMethod(name);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ignored) {
			}
		}
		throw new IllegalStateException("method not found: " + name);
	}
}