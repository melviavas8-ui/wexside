package ru.wexside.misc;

import ru.wexside.misc.ConfigRegistry;
import ru.wexside.misc.ContainerDisplay;

public final class ContainerDisplaySettings {
    private final ContainerDisplay containerDisplay = new ContainerDisplay();

    public ContainerDisplaySettings(ConfigRegistry configRegistry) {
        configRegistry.register(this.containerDisplay);
    }

    public ContainerDisplay getContainerDisplay() {
        return this.containerDisplay;
    }
}

