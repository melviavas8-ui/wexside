package ru.wexside.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.SegmentedControlOption;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.SegmentedControlStyle;

public final class SegmentedControl
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float selectionX;
    private boolean selectionPositionInitialized;
    private int selectedIndex = -1;
    private final SegmentedControlStyle segmentedControlStyle;
    private final List<SegmentedControlOption> options = new ArrayList<SegmentedControlOption>();
    private IntConsumer intConsumer = n -> {};

    public SegmentedControl(GuiBounds bounds2, String ... stringArray) {
        this(bounds2, new SegmentedControlStyle(), stringArray);
    }

    public SegmentedControl(GuiBounds bounds2, SegmentedControlStyle segmentedControlStyle, String ... stringArray) {
        super(bounds2);
        this.segmentedControlStyle = segmentedControlStyle == null ? new SegmentedControlStyle() : segmentedControlStyle;
        this.setString(stringArray);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        float f;
        if (n3 != 0 || this.options.isEmpty()) {
            return false;
        }
        float f2 = (float)n - this.getBounds().getX();
        if (!this.containsLocalPoint(f2, f = (float)n2 - this.getBounds().getY())) {
            return false;
        }
        for (int i = 0; i < this.options.size(); ++i) {
            if (!this.options.get(i).onMousePressed((int)f2, (int)f, n3)) continue;
            this.setIntType2(i);
            return true;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutOptions();
        GuiBounds bounds2 = this.getBounds();
        if (this.options.isEmpty()) {
            return bounds2.getY() + bounds2.getHeight();
        }
        GuiDrawApi drawApi = this.segmentedControlStyle.getGuiDrawApi();
        if (drawApi == null) {
            return bounds2.getY() + bounds2.getHeight();
        }
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(bounds2.getX(), bounds2.getY(), 0.0f);
        this.renderBackground(drawApi, matrix4f2, bounds2.getWidth(), bounds2.getHeight());
        this.renderSelection(drawApi, matrix4f2);
        for (SegmentedControlOption segmentedControlOption : this.options) {
            segmentedControlOption.render(f, matrix4f2);
        }
        return bounds2.getY() + bounds2.getHeight();
    }

    public void setIntConsumer(IntConsumer intConsumer) {
        this.intConsumer = intConsumer == null ? n -> {} : intConsumer;
    }

    public void setFloatType(float f) {
        if (Math.abs(this.segmentedControlStyle.getSegmentWidth() - f) < 0.01f) {
            return;
        }
        this.segmentedControlStyle.withSegmentWidth(f);
        this.layoutOptions();
    }

    public List<String> getList() {
        ArrayList<String> arrayList = new ArrayList<String>(this.options.size());
        for (SegmentedControlOption segmentedControlOption : this.options) {
            arrayList.add(segmentedControlOption.getString2());
        }
        return Collections.unmodifiableList(arrayList);
    }

    public void setIntType2(int n) {
        if (n < 0 || n >= this.options.size() || this.selectedIndex == n) {
            return;
        }
        this.selectedIndex = n;
        this.refreshSelection();
        this.intConsumer.accept(n);
    }

    public void setString(String ... stringArray) {
        String string = this.getString();
        this.options.clear();
        this.options.clear();
        if (stringArray != null) {
            for (String string2 : stringArray) {
                if (string2 == null || string2.isBlank()) continue;
                this.setString2(string2);
            }
        }
        this.selectedIndex = this.resolveSelectedIndex(string);
        this.selectionPositionInitialized = false;
        this.refreshSelection();
        this.layoutOptions();
    }

    public void addOption(String string, String string2) {
        if (string == null || string.isBlank()) {
            return;
        }
        this.createOption(string, string2);
        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
            this.selectionPositionInitialized = false;
        }
        this.refreshSelection();
        this.layoutOptions();
    }

    private boolean containsLocalPoint(float f, float f2) {
        return f >= 0.0f && f <= this.getBounds().getWidth() && f2 >= 0.0f && f2 <= this.getBounds().getHeight();
    }

    private void createOption(String string, String string2) {
        SegmentedControlOption segmentedControlOption = new SegmentedControlOption(new GuiBounds(0.0f, 0.0f, this.segmentedControlStyle.getSegmentWidth(), this.segmentedControlStyle.getSegmentHeight()), string, string2, this.segmentedControlStyle);
        this.options.add(segmentedControlOption);
        this.addChild(segmentedControlOption);
    }

    private int resolveSelectedIndex(String string) {
        if (this.options.isEmpty()) {
            return -1;
        }
        if (string != null && !string.isBlank()) {
            for (int i = 0; i < this.options.size(); ++i) {
                if (!string.equals(this.options.get(i).getString2())) continue;
                return i;
            }
        }
        if (this.selectedIndex >= 0 && this.selectedIndex < this.options.size()) {
            return this.selectedIndex;
        }
        return 0;
    }

    private void setString2(String string) {
        this.createOption(string, null);
    }

    public void setString3(String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        for (int i = 0; i < this.options.size(); ++i) {
            if (!string.equals(this.options.get(i).getString2())) continue;
            this.setIntType2(i);
            return;
        }
    }

    public int getIntType2() {
        return this.selectedIndex;
    }

    public void setString4(String string) {
        this.addOption(string, null);
    }

    public String getString() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.options.size() ? this.options.get(this.selectedIndex).getString2() : "";
    }

    private void renderBackground(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2) {
        drawApi.drawRoundedRectangle(matrix4f, 0.0f, 0.0f, f, f2, this.segmentedControlStyle.getBackgroundRadius(), this.segmentedControlStyle.getIntType3());
        drawApi.drawRoundedRectangleOutlined(matrix4f, 0.0f, 0.0f, f, f2, this.segmentedControlStyle.getBackgroundRadius(), this.segmentedControlStyle.getBorderWidth(), this.segmentedControlStyle.getIntType2(), this.segmentedControlStyle.getIntType6());
    }

    private void renderSelection(GuiDrawApi drawApi, Matrix4f matrix4f) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.options.size()) {
            return;
        }
        float f = (float)this.selectedIndex * this.segmentedControlStyle.getSegmentWidth();
        if (!this.selectionPositionInitialized) {
            this.selectionX = f;
            this.selectionPositionInitialized = true;
        } else {
            this.selectionX = FrameInterpolator.lerpTowards(this.selectionX, f, this.segmentedControlStyle.getSelectionAnimationSpeed());
        }
        drawApi.drawRoundedRectangle(matrix4f, this.selectionX + 0.5f, 0.5f, this.segmentedControlStyle.getSegmentWidth() - 1.0f, this.segmentedControlStyle.getSegmentHeight() - 1.0f, this.segmentedControlStyle.getSelectionRadius(), this.segmentedControlStyle.getIntType5());
    }

    public SegmentedControlStyle getSegmentedControlStyle() {
        return this.segmentedControlStyle;
    }

    private void refreshSelection() {
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).setBooleanType(i == this.selectedIndex);
        }
    }

    private void layoutOptions() {
        float f = this.segmentedControlStyle.getSegmentWidth();
        float f2 = this.segmentedControlStyle.getSegmentHeight();
        for (int i = 0; i < this.options.size(); ++i) {
            SegmentedControlOption segmentedControlOption = this.options.get(i);
            segmentedControlOption.getBounds().setPosition((float)i * f, 0.0f);
            segmentedControlOption.getBounds().setSize(f, f2);
        }
        this.getBounds().setSize(f * (float)this.options.size(), f2);
    }
}

