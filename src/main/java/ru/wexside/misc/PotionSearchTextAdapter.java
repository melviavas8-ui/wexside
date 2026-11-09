package ru.wexside.misc;

import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.TextInputModel;

final class PotionSearchTextAdapter
implements TextInputModel {
    private final PotionEditorState potionEditorState;

    PotionSearchTextAdapter(PotionEditorState potionEditorState) {
        this.potionEditorState = potionEditorState;
    }

    @Override
    public int getMaximumLength() {
        return 48;
    }

    @Override
    public String getText() {
        return this.potionEditorState.getSearchQuery();
    }

    @Override
    public void setText(String text) {
        this.potionEditorState.setSearchQuery(text == null ? "" : text);
    }
}

