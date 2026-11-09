package ru.wexside.misc;

import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.SelectableButton;
import ru.wexside.model.esp.EspRelation;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;

public final class EspRelationSelector {
    private final SelectableButton selectableButton;
    private static final float value = 33.0f;
    private static final float value2 = 34.5f;
    private static final String string = "Д";
    private EspRelation espRelation = EspRelation.DEFAULT;
    private final SelectableButton selectableButton2;
    private static final float value3 = 3.0f;
    private static final String string2 = "х";
    private Consumer<EspRelation> consumer = espRelation -> {};
    public static final float value4 = 11.5f;

    public EspRelationSelector() {
        this.selectableButton = new SelectableButton(string, EspRelation.FRIEND.getTitle(), 33.0f, 11.5f, () -> this.setEspRelation2(EspRelation.FRIEND));
        this.selectableButton2 = new SelectableButton(string2, EspRelation.DEFAULT.getTitle(), 34.5f, 11.5f, () -> this.setEspRelation2(EspRelation.DEFAULT));
        this.update();
    }

    public void setEspRelation(EspRelation espRelation) {
        this.setEspRelation2(espRelation);
    }

    public float enabled() {
        return 70.5f;
    }

    public boolean onMousePressed(int n, int n2, int n3) {
        return this.selectableButton.onMousePressed(n, n2, n3) || this.selectableButton2.onMousePressed(n, n2, n3);
    }

    public void setConsumer(Consumer<EspRelation> consumer) {
        this.consumer = consumer == null ? espRelation -> {} : consumer;
    }

    private void setEspRelation2(EspRelation espRelation) {
        if (this.espRelation == espRelation) {
            return;
        }
        this.espRelation = espRelation;
        this.update();
        this.consumer.accept(espRelation);
    }

    private void update() {
        this.selectableButton.setBooleanType(this.espRelation == EspRelation.FRIEND);
        this.selectableButton2.setBooleanType(this.espRelation == EspRelation.DEFAULT);
    }

    public void renderWithOpacity(float f, Matrix4f matrix4f2, float f2) {
        if (f2 <= 0.01f) {
            return;
        }
        float f3 = this.selectableButton.getBounds().getX();
        float f4 = this.selectableButton.getBounds().getY();
        ClippedLayerRenderer.renderLayer(WexSideClient.getGuiRenderer(), matrix4f2, f3, f4, this.enabled(), 11.5f, 0.0f, f2 < 0.99f, ColorUtils.withAlpha(-1, 255.0f * f2), contentMatrix -> {
            Matrix4f translatedMatrix = new Matrix4f((Matrix4fc)contentMatrix).translate(-f3, -f4, 0.0f);
            this.selectableButton.render(f, translatedMatrix);
            this.selectableButton2.render(f, translatedMatrix);
        });
    }

    public void setPosition(float f, float f2) {
        this.selectableButton.getBounds().setPosition(f, f2);
        this.selectableButton2.getBounds().setPosition(f + 33.0f + 3.0f, f2);
    }

    public EspRelation getEspRelation() {
        return this.espRelation;
    }
}

