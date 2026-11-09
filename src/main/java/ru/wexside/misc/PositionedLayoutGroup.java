package ru.wexside.misc;

import java.util.Objects;
import ru.wexside.misc.LayoutGroup;

public final class PositionedLayoutGroup {
    private final LayoutGroup layoutGroup2;
    private final float x;
    private final boolean hasLeadingGap;
    private final float width;
    private final float y;

    public PositionedLayoutGroup(LayoutGroup layoutGroup2, float f, float f2, float f3, boolean bl) {
        this.layoutGroup2 = layoutGroup2;
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.hasLeadingGap = bl;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PositionedLayoutGroup)) {
            return false;
        }
        PositionedLayoutGroup positionedLayoutGroup = (PositionedLayoutGroup)object;
        return Objects.equals(this.layoutGroup2, positionedLayoutGroup.layoutGroup2) && Float.compare(this.x, positionedLayoutGroup.x) == 0 && Float.compare(this.y, positionedLayoutGroup.y) == 0 && Float.compare(this.width, positionedLayoutGroup.width) == 0 && this.hasLeadingGap == positionedLayoutGroup.hasLeadingGap;
    }

    public String toString() {
        boolean bl = this.hasLeadingGap;
        float f = this.width;
        float f2 = this.y;
        float f3 = this.x;
        String string = String.valueOf(this.layoutGroup2);
        return "PositionedLayoutGroup[layoutGroup=" + string + ", x=" + f3 + ", y=" + f2 + ", width=" + f + ", hasLeadingGap=" + bl + "]";
    }

    public int hashCode() {
        return Objects.hash(this.layoutGroup2, Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(this.width), this.hasLeadingGap);
    }

    public LayoutGroup getLayoutGroup() {
        return this.layoutGroup2;
    }

    public boolean hasLeadingGap() {
        return this.hasLeadingGap;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }
}

