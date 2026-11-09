package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.misc.ListLayout;
import ru.wexside.misc.ModuleListPanel;
import ru.wexside.ui.GuiElement;

class GuiListLayoutAdapter
implements ListLayout {
    final float value;
    final ModuleListPanel this$0;

    GuiListLayoutAdapter(ModuleListPanel miscellaneousModules, float f) {
        this.this$0 = miscellaneousModules;
        this.value = f;
    }

    @Override
    public float getItemHeight(int n) {
        return this.this$0.getChildren().get(n).getBounds().getHeight();
    }

    @Override
    public int getItemCount() {
        return this.this$0.getChildren().size();
    }

    @Override
    public float renderItem(int n, Matrix4f matrix4f, float f, float f2, float f3) {
        GuiElement element2 = this.this$0.getChildren().get(n);
        element2.getBounds().setPosition(f, f2);
        element2.getBounds().setSize(f3, element2.getBounds().getHeight());
        return element2.render(this.value, matrix4f);
    }
}

