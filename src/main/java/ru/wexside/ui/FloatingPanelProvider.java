package ru.wexside.ui;

import ru.wexside.ui.FloatingPanel;
import ru.wexside.ui.FloatingPanelManager;

public interface FloatingPanelProvider {
    public FloatingPanel getFloatingPanel();

    public void updateFloatingPanelPosition();

    default public void setFloatingPanelManager(FloatingPanelManager manager) {
    }
}

