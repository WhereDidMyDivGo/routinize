package com.github.wheredidmydivgo.routinize.gui;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class MultiLineEditBoxAccess {

	private static final Method GET_INNER_LEFT = findMethod("getInnerLeft");
	private static final Method GET_INNER_TOP = findMethod("getInnerTop");
	private static final Field TEXT_FIELD = findField("textField");

	private MultiLineEditBoxAccess() {}

	public static int innerLeft(MultiLineEditBox editor) {
		return invokeInt(GET_INNER_LEFT, editor);
	}

	public static int innerTop(MultiLineEditBox editor) {
		return invokeInt(GET_INNER_TOP, editor);
	}

	public static MultilineTextField textField(MultiLineEditBox editor) {
		try {
			return (MultilineTextField) TEXT_FIELD.get(editor);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
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

	private static Field findField(String name) {
		for (Class<?> type = MultiLineEditBox.class; type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
			}
		}
		throw new IllegalStateException("field not found: " + name);
	}
}