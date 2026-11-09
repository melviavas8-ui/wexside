package ru.wexside.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.SearchQueryState;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleManager;
import ru.wexside.setting.Setting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ScrollController;

public final class ModuleSearchResults
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    static final int slot = 3;
    static final int slot2 = 6;
    private String string4 = null;
    private final String string5;
    private final ModuleManager moduleManager;
    private final String string6;
    private final String string7;
    private final ScrollController scrollController = new ScrollController(18.0f, 30.0f);
    static final int slot3 = 9;
    private final Consumer<Module> consumer;
    private final SearchQueryState searchQueryState;
    private final Function<Module, String> function;

    public ModuleSearchResults(GuiBounds bounds2, ModuleManager moduleManager, SearchQueryState searchQueryState, Consumer<Module> consumer, Function<Module, String> function) {
        super(bounds2);
        this.string5 = "Введите название функции или модуля в поисковую";
        this.string7 = "строку, что-бы найти нужный вам элемент";
        this.string6 = "Мы обыскали всё, но не нашли ни одной функции";
        this.moduleManager = moduleManager;
        this.searchQueryState = searchQueryState;
        this.consumer = consumer;
        this.function = function;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (this.getBounds().contains(n, n2)) {
            this.scrollController.scrollByWheel(d, this.getBounds().getHeight());
        }
    }

    @Override
    public void update() {
        for (GuiElement element2 : this.children) {
            element2.update();
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        this.refreshSearchResults();
        return this.getBounds().contains(n, n2) && super.onMousePressed(n, n2, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.refreshSearchResults();
        if (!this.searchQueryState.hasQuery()) {
            this.renderEmptyMessage(matrix4f, drawApi, bounds2, "Введите название функции или модуля в поисковую", "строку, что-бы найти нужный вам элемент");
            return bounds2.getY() + bounds2.getHeight();
        }
        if (this.children.isEmpty()) {
            this.scrollController.update(bounds2.getHeight(), 0.0f);
            this.scrollController.setContentHeight(bounds2.getHeight(), 0.0f);
            String string = this.searchQueryState.getQuery();
            String string2 = "или модуля похожих на \u00ab" + string + "\u00bb";
            this.renderEmptyMessage(matrix4f, drawApi, bounds2, "Мы обыскали всё, но не нашли ни одной функции", string2);
            return bounds2.getY() + bounds2.getHeight();
        }
        float f2 = this.getFloatType();
        this.scrollController.update(bounds2.getHeight(), f2);
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangleRadii(matrix4f, bounds2.getX() + 1.0f, bounds2.getY() + 1.0f, bounds2.getWidth() - 1.5f, bounds2.getHeight() - 1.5f, 10.5f, 0.0f, 0.0f, 0.0f, ColorUtils.rgba(0, 0, 0, 45));
        drawApi.applyStencilMask(1);
        float f3 = this.renderResultRows(f, matrix4f);
        drawApi.endStencil();
        this.scrollController.setContentHeight(bounds2.getHeight(), f3);
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void update2() {
        this.scrollController.scrollToTop();
    }

    private float renderResultRows(float f, Matrix4f matrix4f) {
        float f2;
        GuiBounds bounds2 = this.getBounds();
        float f3 = this.scrollController.getOffset();
        float f4 = bounds2.getX() + 8.0f;
        float f5 = f2 = bounds2.getY() + 8.0f + f3;
        float f6 = bounds2.getY() - 1.0f;
        float f7 = bounds2.getY() + bounds2.getHeight() + 1.0f;
        for (GuiElement element2 : this.children) {
            element2.getBounds().setPosition(f4, f2);
            float f8 = element2.getBounds().getHeight();
            f5 = f2 + f8 < f6 || f2 > f7 ? f2 + f8 : element2.render(f, matrix4f);
            f2 = f5 + 4.0f;
        }
        float f9 = f5 - f3;
        return f9 <= bounds2.getY() + 8.0f ? 0.0f : f9 - bounds2.getY() + 8.0f;
    }

    private void refreshSearchResults() {
        String string = this.searchQueryState.getQuery();
        if (Objects.equals(string, this.string4)) {
            return;
        }
        this.string4 = string;
        this.children.clear();
        if (string == null || string.isBlank()) {
            this.scrollController.scrollToTop();
            return;
        }
        String string2 = this.normalizeSearchText(string);
        if (string2.isEmpty()) {
            this.scrollController.scrollToTop();
            return;
        }
        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        ArrayList<Module> arrayList2 = new ArrayList<Module>(this.moduleManager.getModules());
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        if (espFeatures != null) {
            arrayList2.addAll(espFeatures.getDefaultModules());
        }
        for (Module module : arrayList2) {
            int n = this.scoreModuleMatch(module, string2);
            if (n < 0) continue;
            MatchKind matchKind = n < 6 ? MatchKind.MODULE : MatchKind.SETTING;
            String string3 = this.function == null ? "" : this.function.apply(module);
            results.add(new SearchResult(n, new SearchResultRow(module, string3, matchKind, this.consumer)));
        }
        results.sort(Comparator.comparingInt(SearchResult::score));
        for (SearchResult result : results) {
            this.addChild(result.row());
        }
        this.scrollController.scrollToTop();
    }

    private void renderEmptyMessage(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2, String string, String string2) {
        int n = ThemeColors.textPlaceholder();
        float f = bounds2.getX() + 87.5f;
        float f2 = bounds2.getY() + 119.0f;
        float f3 = FontRegistry.font2.textWidth(string, 6.5f);
        float f4 = FontRegistry.font2.textHeight(string, 6.5f);
        float f5 = f + f3 / 2.0f;
        float f6 = FontRegistry.font2.textWidth(string2, 6.5f);
        float f7 = f5 - f6 / 2.0f;
        float f8 = f2 + f4 + 3.0f - 1.0f;
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f, f2, 6.5f, n);
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f7, f8, 6.5f, n);
    }

    private String normalizeSearchText(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '-' || c == '_' || Character.isWhitespace(c)) continue;
            stringBuilder.append(Character.toLowerCase(c));
        }
        return stringBuilder.toString();
    }

    private int scoreModuleMatch(Module module, String string) {
        int n = Integer.MAX_VALUE;
        n = Math.min(n, this.scoreTextMatch(module.getDisplayName(), string, 0));
        for (String object : module.getAliases()) {
            n = Math.min(n, this.scoreTextMatch(object, string, 3));
        }
        for (Setting setting : module.getSettings()) {
            n = Math.min(n, this.scoreTextMatch(setting.getDisplayName(), string, 6));
            for (String string2 : setting.getAliases()) {
                n = Math.min(n, this.scoreTextMatch(string2, string, 9));
            }
        }
        return n == Integer.MAX_VALUE ? -1 : n;
    }

    private int scoreTextMatch(String string, String string2, int n) {
        if (string == null) {
            return Integer.MAX_VALUE;
        }
        String string3 = this.normalizeSearchText(string);
        if (string3.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        if (string3.equals(string2)) {
            return n;
        }
        if (string3.startsWith(string2)) {
            return n + 1;
        }
        if (string3.contains(string2)) {
            return n + 2;
        }
        return Integer.MAX_VALUE;
    }

    private float getFloatType() {
        float f = 8.0f;
        for (GuiElement element2 : this.children) {
            f += element2.getBounds().getHeight() + 4.0f;
        }
        return f - 4.0f + 8.0f;
    }

    public boolean selectFirstResult() {
        this.refreshSearchResults();
        if (!this.searchQueryState.hasQuery() || this.children.isEmpty()) {
            return false;
        }
        Object e = this.children.get(0);
        if (e instanceof SearchResultRow) {
            SearchResultRow row = (SearchResultRow)e;
            if (this.consumer != null) {
                this.consumer.accept(row.module());
                return true;
            }
        }
        return false;
    }

    private static enum MatchKind {
        MODULE,
        SETTING;

    }

    private record SearchResult(int score, SearchResultRow row) {
    }

    private static final class SearchResultRow
    extends GuiElement {
        private static final float HEIGHT = 31.0f;
        private final Module module;
        private final String context;
        private final MatchKind matchKind;
        private final Consumer<Module> onSelected;

        private SearchResultRow(Module module, String context, MatchKind matchKind, Consumer<Module> onSelected) {
            super(new GuiBounds(0.0f, 0.0f, 0.0f, 31.0f));
            this.module = module;
            this.context = context == null ? "" : context;
            this.matchKind = matchKind;
            this.onSelected = onSelected;
        }

        private Module module() {
            return this.module;
        }

        @Override
        public void onMouseScroll(int mouseX, int mouseY, double amount) {
        }

        @Override
        public void update() {
        }

        @Override
        public boolean onMousePressed(int mouseX, int mouseY, int button) {
            if (button != 0 || !this.getBounds().contains(mouseX, mouseY)) {
                return false;
            }
            if (this.onSelected != null) {
                this.onSelected.accept(this.module);
            }
            return true;
        }

        @Override
        public float render(float delta, Matrix4f matrix) {
            GuiBounds bounds = this.getBounds();
            bounds.setSize(bounds.getWidth(), 31.0f);
            GuiDrawApi renderer = WexSideClient.getGuiRenderer();
            renderer.drawRoundedRectangle(matrix, bounds.getX(), bounds.getY(), bounds.getWidth(), 31.0f, 7.0f, ThemeColors.borderSubtle());
            FontRegistry.font5.drawText(matrix, renderer, this.module.getDisplayName(), bounds.getX() + 8.0f, bounds.getY() + 6.0f, 6.5f, ThemeColors.textPrimary());
            String category = this.matchKind == MatchKind.SETTING ? "Setting" : this.module.getCategory().getName();
            String details = this.context.isBlank() ? category : category + "  \u00b7  " + this.context;
            FontRegistry.font2.drawText(matrix, renderer, details, bounds.getX() + 8.0f, bounds.getY() + 17.0f, 5.75f, ThemeColors.textPlaceholder());
            return bounds.getY() + 31.0f;
        }
    }
}

