package ru.wexside.module.misc;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.wexside.misc.PotionPresetDraft;
import ru.wexside.misc.ServerKind;
import ru.wexside.module.misc.PotionCombinerModule;
import ru.wexside.setting.BindSetting;
import ru.wexside.util.PotionPresetController;

public final class PotionCombinerRadialScreen
extends Screen {
    private final BindSetting bind;
    private final PotionPresetController potionCombiner;
    private final Consumer<PotionPresetDraft> onSelect;

    public PotionCombinerRadialScreen(BindSetting bind, PotionPresetController potionCombiner, Consumer<PotionPresetDraft> onSelect) {
        super((Text)Text.empty());
        this.bind = bind;
        this.potionCombiner = potionCombiner;
        this.onSelect = onSelect;
    }

    public BindSetting bind() {
        return this.bind;
    }

    public List<PotionPresetDraft> presets() {
        return this.potionCombiner.getFavoritePresets();
    }

    public ServerKind serverMode() {
        return PotionCombinerModule.getServerKind();
    }

    public void tick() {
        if (this.bind == null || !this.bind.isPressed()) {
            this.close();
        }
    }

    public boolean shouldPause() {
        return false;
    }

    public void select(PotionPresetDraft preset) {
        if (preset != null && this.onSelect != null) {
            this.onSelect.accept(preset);
        }
        this.close();
    }
}

