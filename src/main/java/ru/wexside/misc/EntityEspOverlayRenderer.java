package ru.wexside.misc;

import java.util.List;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.item.ItemBadge;
import ru.wexside.misc.BoxEspSettings;
import ru.wexside.misc.EntityEquipmentOverlayRenderer;
import ru.wexside.misc.EspBoxDecorationRenderer;
import ru.wexside.misc.NameTagSettings;
import ru.wexside.misc.WorldBoxSettings;
import ru.wexside.model.esp.EspRelation;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.EspBoxRenderer;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;

public final class EntityEspOverlayRenderer {
    private final EspBoxDecorationRenderer espBoxDecorationRenderer;
    private final EspBoxRenderer espBoxRenderer = new EspBoxRenderer();
    private final EntityEquipmentOverlayRenderer entityEquipmentOverlayRenderer;

    public EntityEspOverlayRenderer() {
        this.espBoxDecorationRenderer = new EspBoxDecorationRenderer();
        this.entityEquipmentOverlayRenderer = new EntityEquipmentOverlayRenderer();
    }

    public void prepareEquipmentIcons(EspTargetType espTargetType, EspRelation espRelation, List<ItemStack> list, float f) {
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        NameTagSettings talisman = espFeatures == null || espTargetType == null ? null : espFeatures.getNameTagSettings(espTargetType, espRelation);
        boolean bl = talisman != null && talisman.isEnabled() && talisman.areItemsVisible();
        this.entityEquipmentOverlayRenderer.member11505(bl ? list : List.of(), f);
    }

    public void renderOverlay(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2, EspTargetType espTargetType, EspRelation espRelation, float f, String string, int n, int n2, ItemBadge itemBadge, String string2) {
        NameTagSettings talisman;
        BoxEspSettings rectangle;
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        if (espFeatures == null || espTargetType == null) {
            return;
        }
        WorldBoxSettings dotted = espFeatures.getWorldBoxSettings(espTargetType, espRelation);
        if (dotted != null && dotted.isEnabled()) {
            this.espBoxRenderer.member1871(drawApi, matrix4f, bounds2, dotted, f);
        }
        if ((rectangle = espFeatures.getBox2dSettings(espTargetType, espRelation)) != null && rectangle.isEnabled()) {
            this.espBoxDecorationRenderer.member9165(drawApi, matrix4f, bounds2, rectangle);
        }
        if ((talisman = espFeatures.getNameTagSettings(espTargetType, espRelation)) != null && talisman.isEnabled()) {
            this.entityEquipmentOverlayRenderer.member2669(drawApi, matrix4f, bounds2, talisman, string, n, n2, itemBadge, string2);
        }
    }

    public void update() {
        this.entityEquipmentOverlayRenderer.member5307();
    }
}

