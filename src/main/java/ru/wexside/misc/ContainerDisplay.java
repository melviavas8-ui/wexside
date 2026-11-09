package ru.wexside.misc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.misc.ContainerColumnLayout;
import ru.wexside.misc.MenuConfigSection;

public final class ContainerDisplay
extends MenuConfigSection
implements ConfigSerializable {
    private boolean settingDescriptionsVisible = true;
    private boolean moduleDescriptionsVisible = true;
    private ContainerColumnLayout containerColumnLayout = ContainerColumnLayout.TWO_COLUMNS;

    public ContainerDisplay() {
        super("container_display");
    }

    @Override
    public void writeConfig(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(this.containerColumnLayout.ordinal());
        dataOutputStream.writeBoolean(this.moduleDescriptionsVisible);
        dataOutputStream.writeBoolean(this.settingDescriptionsVisible);
    }

    @Override
    public void readConfig(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        ContainerColumnLayout[] cls0273Array = ContainerColumnLayout.values();
        this.containerColumnLayout = n >= 0 && n < cls0273Array.length ? cls0273Array[n] : ContainerColumnLayout.SINGLE_COLUMN;
        try {
            this.moduleDescriptionsVisible = dataInputStream.readBoolean();
            this.settingDescriptionsVisible = dataInputStream.readBoolean();
        }
        catch (EOFException eOFException) {
            this.moduleDescriptionsVisible = true;
            this.settingDescriptionsVisible = true;
        }
    }

    public boolean areModuleDescriptionsVisible() {
        return this.moduleDescriptionsVisible;
    }

    public boolean areSettingDescriptionsVisible() {
        return this.settingDescriptionsVisible;
    }

    public ContainerColumnLayout getContainerColumnLayout() {
        return this.containerColumnLayout;
    }

    public void setSettingDescriptionsVisible(boolean bl) {
        this.settingDescriptionsVisible = bl;
    }

    public void setModuleDescriptionsVisible(boolean bl) {
        this.moduleDescriptionsVisible = bl;
    }

    public void setContainerColumnLayout(ContainerColumnLayout containerColumnLayout) {
        this.containerColumnLayout = containerColumnLayout;
    }

    public int getVisibleDescriptionCount() {
        int n = 0;
        if (this.moduleDescriptionsVisible) {
            ++n;
        }
        if (this.settingDescriptionsVisible) {
            ++n;
        }
        return n;
    }
}

