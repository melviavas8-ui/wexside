package ru.wexside.misc;

import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.ui.GuiElement;

public final class PopupTreeBinder {
    public static void bindTree(GuiElement root, PopupManager popupManager) {
        PopupTreeBinder.bindTree(root, popupManager, null);
    }

    public static void bindTree(GuiElement root, PopupManager popupManager, PopupOwner parentOwner) {
        if (root == null || popupManager == null) {
            return;
        }
        PopupTreeBinder.bindRecursively(root, popupManager, parentOwner);
    }

    private static void bindRecursively(GuiElement element, PopupManager popupManager, PopupOwner parentOwner) {
        if (element instanceof PopupOwner) {
            PopupOwner owner = (PopupOwner)((Object)element);
            popupManager.register(owner, parentOwner);
        }
        for (GuiElement child : element.getChildren()) {
            PopupTreeBinder.bindRecursively(child, popupManager, parentOwner);
        }
    }
}

