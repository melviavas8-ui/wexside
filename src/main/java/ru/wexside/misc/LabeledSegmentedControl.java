package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LabeledSegmentOption;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class LabeledSegmentedControl
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private int slot;
    private final List<LabeledSegmentOption> options;
    private final float separatorTextWidth;
    private IntConsumer intConsumer = n -> {};
    private final float separatorTextHeight;

    public LabeledSegmentedControl(String string, String ... stringArray) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.separatorTextWidth = FontRegistry.font4.textWidth("или", 6.0f);
        this.separatorTextHeight = FontRegistry.font4.textHeight("или", 6.0f);
        float f = 11.5f;
        this.options = new ArrayList<LabeledSegmentOption>(stringArray.length);
        int n2 = 0;
        while (n2 < stringArray.length) {
            LabeledSegmentOption labeledSegmentOption = new LabeledSegmentOption(string, stringArray[n2], f);
            int n3 = n2++;
            labeledSegmentOption.setRunnable(() -> this.setIntType2(n3));
            this.options.add(labeledSegmentOption);
        }
        if (!this.options.isEmpty()) {
            this.options.get(0).setBooleanType(true);
        }
        float f2 = 1.0f;
        for (int i = 0; i < this.options.size(); ++i) {
            f2 += this.options.get(i).getBounds().getWidth();
            if (i >= this.options.size() - 1) continue;
            f2 += 3.0f + this.separatorTextWidth + 3.0f;
        }
        this.getBounds().setSize(f2 += 1.0f, 13.5f);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        for (LabeledSegmentOption labeledSegmentOption : this.options) {
            if (!labeledSegmentOption.onMousePressed(n, n2, n3)) continue;
            return true;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        float f2 = bounds2.getX() + 1.0f;
        float f3 = bounds2.getY() + 1.0f;
        float f4 = bounds2.getY() + (13.5f - this.separatorTextHeight) / 2.0f;
        for (int i = 0; i < this.options.size(); ++i) {
            LabeledSegmentOption labeledSegmentOption = this.options.get(i);
            labeledSegmentOption.getBounds().setPosition(f2, f3);
            labeledSegmentOption.render(f, matrix4f);
            f2 += labeledSegmentOption.getBounds().getWidth();
            if (i >= this.options.size() - 1) continue;
            float f5 = f2 + 3.0f;
            FontRegistry.font4.drawText(matrix4f, drawApi, "или", f5, f4, 6.0f, ThemeColors.textSecondary());
            f2 += 3.0f + this.separatorTextWidth + 3.0f;
        }
        return bounds2.getY() + bounds2.getHeight();
    }

    public void setIntConsumer(IntConsumer intConsumer) {
        this.intConsumer = intConsumer == null ? n -> {} : intConsumer;
    }

    public void setIntType2(int n) {
        if (n < 0 || n >= this.options.size() || n == this.slot) {
            return;
        }
        this.slot = n;
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).setBooleanType(i == n);
        }
        this.intConsumer.accept(n);
    }

    public int getIntType2() {
        return this.slot;
    }
}

