package ru.wexside.misc;

import java.util.List;
import java.util.Objects;
import ru.wexside.ui.GuiElement;

public final class LayoutGroup {
    private final List<GuiElement> elements;
    private final boolean sharedVisibility;
    private final float animationProgress;
    private final boolean targetVisible;

    public LayoutGroup(List<GuiElement> list, float f, boolean bl, boolean bl2) {
        this.elements = list;
        this.animationProgress = f;
        this.targetVisible = bl;
        this.sharedVisibility = bl2;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LayoutGroup)) {
            return false;
        }
        LayoutGroup layoutGroup2 = (LayoutGroup)object;
        return Objects.equals(this.elements, layoutGroup2.elements) && Float.compare(this.animationProgress, layoutGroup2.animationProgress) == 0 && this.targetVisible == layoutGroup2.targetVisible && this.sharedVisibility == layoutGroup2.sharedVisibility;
    }

    public String toString() {
        boolean bl = this.sharedVisibility;
        boolean bl2 = this.targetVisible;
        float f = this.animationProgress;
        String string = String.valueOf(this.elements);
        return "LayoutGroup[children=" + string + ", animation=" + f + ", targetVisible=" + bl2 + ", sharedVisibility=" + bl + "]";
    }

    public int hashCode() {
        return Objects.hash(this.elements, Float.valueOf(this.animationProgress), this.targetVisible, this.sharedVisibility);
    }

    public float getAnimationProgress() {
        return this.animationProgress;
    }

    public List<GuiElement> getChildren() {
        return this.elements;
    }

    public boolean isTargetVisible() {
        return this.targetVisible;
    }

    public boolean hasSharedVisibility() {
        return this.sharedVisibility;
    }
}

