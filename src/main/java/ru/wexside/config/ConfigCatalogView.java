package ru.wexside.config;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.config.LocalConfigCatalog;
import ru.wexside.config.LocalConfigEntry;
import ru.wexside.misc.ConfigManager;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.TextureResource;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ConfigCatalogView
extends GuiElement {
    private final String title;
    private final LocalConfigCatalog catalog;
    private final StringBuilder profileName = new StringBuilder();
    private float scrollOffset;
    private float targetScrollOffset;
    private int selectedTab;
    private boolean createDialogOpen;
    private boolean serverSpecific;
    private String status = "";
    private long statusExpiresAt;

    public ConfigCatalogView(GuiBounds bounds, String title, LocalConfigCatalog catalog) {
        super(bounds);
        this.title = title;
        this.catalog = catalog;
        this.refresh();
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY, int button) {
        if (!this.getBounds().contains(mouseX, mouseY)) {
            return false;
        }
        if (this.createDialogOpen) {
            return this.handleCreateDialogClick(mouseX, mouseY, button);
        }
        if (button == 0 && this.tabBounds(0).contains(mouseX, mouseY)) {
            this.selectedTab = 0;
            this.targetScrollOffset = 0.0f;
            return true;
        }
        if (button == 0 && this.tabBounds(1).contains(mouseX, mouseY)) {
            this.selectedTab = 1;
            this.targetScrollOffset = 0.0f;
            return true;
        }
        float toolbarY = this.getBounds().getY();
        if (button == 0 && this.contains(mouseX, mouseY, this.toolbarButtonX(3), toolbarY, 15.0f, 15.0f)) {
            this.saveActiveProfile();
            return true;
        }
        if (button == 0 && this.contains(mouseX, mouseY, this.toolbarButtonX(2), toolbarY, 15.0f, 15.0f)) {
            this.refresh();
            this.showStatus("Список обновлён");
            return true;
        }
        if (button == 0 && this.contains(mouseX, mouseY, this.toolbarButtonX(1), toolbarY, 15.0f, 15.0f)) {
            this.openCreateDialog();
            return true;
        }
        if (button == 0 && this.contains(mouseX, mouseY, this.toolbarButtonX(0), toolbarY, 15.0f, 15.0f)) {
            this.openFolder();
            return true;
        }
        int index = this.rowIndexAt(mouseX, mouseY);
        List<LocalConfigEntry> entries = this.visibleEntries();
        if (index < 0 || index >= entries.size()) {
            return true;
        }
        LocalConfigEntry entry = entries.get(index);
        if (button == 0 && this.menuButtonBounds(this.rowBounds(index)).contains(mouseX, mouseY)) {
            this.save(entry);
            return true;
        }
        if (button == 1) {
            this.delete(entry);
            return true;
        }
        if (button == 0) {
            this.load(entry);
        }
        return true;
    }

    @Override
    public void onMouseScroll(int mouseX, int mouseY, double amount) {
        if (!this.getBounds().contains(mouseX, mouseY) || this.createDialogOpen) {
            return;
        }
        this.targetScrollOffset = Math.clamp(this.targetScrollOffset - (float)amount * 18.0f, 0.0f, this.maximumScrollOffset());
    }

    @Override
    public boolean onCharTyped(char character) {
        if (!this.createDialogOpen || Character.isISOControl(character) || this.profileName.length() >= 32) {
            return false;
        }
        if ("\\/:*?\"<>|".indexOf(character) < 0) {
            this.profileName.append(character);
        }
        return true;
    }

    @Override
    public boolean onKeyPressed(int keyCode) {
        if (!this.createDialogOpen) {
            return false;
        }
        if (keyCode == 256) {
            this.createDialogOpen = false;
            return true;
        }
        if (keyCode == 259 && !this.profileName.isEmpty()) {
            this.profileName.deleteCharAt(this.profileName.length() - 1);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.createProfile();
            return true;
        }
        return false;
    }

    @Override
    public float render(float delta, Matrix4f matrix) {
        this.scrollOffset = FrameInterpolator.lerpTowards(this.scrollOffset, this.targetScrollOffset, 30.0f);
        GuiDrawApi renderer = WexSideClient.getGuiRenderer();
        this.renderTabs(renderer, matrix);
        this.renderToolbar(renderer, matrix);
        this.renderRows(renderer, matrix);
        if (this.createDialogOpen) {
            this.renderCreateDialog(renderer, matrix);
        }
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    private void renderTabs(GuiDrawApi renderer, Matrix4f matrix) {
        this.renderTab(renderer, matrix, 0, "Все");
        this.renderTab(renderer, matrix, 1, "Локальные");
    }

    private void renderTab(GuiDrawApi renderer, Matrix4f matrix, int index, String label) {
        GuiBounds tab = this.tabBounds(index);
        boolean active = this.selectedTab == index;
        renderer.drawRoundedRectangle(matrix, tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight(), 6.0f, active ? ThemeColors.backgroundControl() : ThemeColors.backgroundSecondary());
        renderer.drawRoundedOutline(matrix, tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight(), 6.0f, 0.75f, active ? ThemeColors.accent() : ThemeColors.borderPrimary());
        float textWidth = FontRegistry.font4.textWidth(label, 6.5f);
        float textHeight = FontRegistry.font4.textHeight(label, 6.5f);
        FontRegistry.font4.drawText(matrix, renderer, label, tab.getX() + (tab.getWidth() - textWidth) * 0.5f, tab.getY() + (tab.getHeight() - textHeight) * 0.5f, 6.5f, active ? ThemeColors.accent() : ThemeColors.textSecondary());
    }

    private void renderToolbar(GuiDrawApi renderer, Matrix4f matrix) {
        float y = this.getBounds().getY();
        this.renderToolbarButton(renderer, matrix, this.toolbarButtonX(3), y, "S");
        this.renderToolbarButton(renderer, matrix, this.toolbarButtonX(2), y, "е");
        this.renderToolbarButton(renderer, matrix, this.toolbarButtonX(1), y, "Я");
        this.renderToolbarButton(renderer, matrix, this.toolbarButtonX(0), y, "x");
        FontRegistry.font2.drawText(matrix, renderer, this.statusText(), this.getBounds().getX() + 8.0f, this.getBounds().getY() + 17.0f, 5.0f, ThemeColors.textMuted());
    }

    private void renderToolbarButton(GuiDrawApi renderer, Matrix4f matrix, float x, float y, String icon) {
        boolean hovered = this.hovered(x, y, 15.0f, 15.0f);
        renderer.drawRoundedRectangle(matrix, x, y, 15.0f, 15.0f, 5.0f, hovered ? ThemeColors.backgroundHover() : ThemeColors.backgroundControl());
        renderer.drawRoundedOutline(matrix, x, y, 15.0f, 15.0f, 5.0f, 0.75f, ThemeColors.borderPrimary());
        float iconWidth = FontRegistry.font3.textWidth(icon, 6.0f);
        float iconHeight = FontRegistry.font3.textHeight(icon, 6.0f);
        FontRegistry.font3.drawGlyphText(matrix, renderer, icon, x + (15.0f - iconWidth) * 0.5f, y + (15.0f - iconHeight) * 0.5f, 6.0f, hovered ? ThemeColors.accent() : ThemeColors.textSecondary());
    }

    private void renderRows(GuiDrawApi renderer, Matrix4f matrix) {
        float contentY = this.getBounds().getY() + 24.0f;
        float contentHeight = Math.max(0.0f, this.getBounds().getHeight() - 24.0f);
        renderer.pushScissor(matrix, this.getBounds().getX(), contentY, this.getBounds().getWidth(), contentHeight);
        List<LocalConfigEntry> entries = this.visibleEntries();
        for (int index = 0; index < entries.size(); ++index) {
            GuiBounds row = this.rowBounds(index);
            if (!(row.getY() + row.getHeight() >= contentY) || !(row.getY() <= contentY + contentHeight)) continue;
            this.renderRow(renderer, matrix, row, entries.get(index));
        }
        renderer.popScissor();
    }

    private void renderRow(GuiDrawApi renderer, Matrix4f matrix, GuiBounds row, LocalConfigEntry entry) {
        boolean active = this.isActive(entry);
        boolean hovered = this.hovered(row.getX(), row.getY(), row.getWidth(), row.getHeight());
        renderer.drawRoundedRectangle(matrix, row.getX(), row.getY(), row.getWidth(), row.getHeight(), 11.0f, hovered ? ThemeColors.backgroundHover() : ColorUtils.withAlpha(ThemeColors.backgroundSecondary(), 0.0f));
        renderer.drawRoundedOutline(matrix, row.getX(), row.getY(), row.getWidth(), row.getHeight(), 11.0f, 0.75f, active ? ThemeColors.accent() : ThemeColors.backgroundControl());
        this.renderAvatar(renderer, matrix, row, entry.avatar());
        float textX = row.getX() + 28.0f;
        FontRegistry.font4.drawText(matrix, renderer, entry.name(), textX, row.getY() + 6.0f, 6.5f, ThemeColors.textPrimary());
        FontRegistry.font2.drawText(matrix, renderer, "Локальный  \u2022  " + entry.updatedAt(), textX, row.getY() + 16.5f, 5.5f, ThemeColors.textMuted());
        this.renderStateButton(renderer, matrix, row, active);
        this.renderSaveButton(renderer, matrix, row, active);
    }

    private void renderAvatar(GuiDrawApi renderer, Matrix4f matrix, GuiBounds row, TextureResource avatar) {
        float x = row.getX() + 6.0f;
        float y = row.getY() + 6.0f;
        if (avatar != null) {
            int texture = renderer.bindTexture(avatar.getTextureId(), avatar.getWidth(), avatar.getHeight());
            renderer.drawRoundedTextureTinted(matrix, x, y, 18.0f, 18.0f, 12.0f, texture, -1);
            return;
        }
        renderer.drawRoundedRectangle(matrix, x, y, 18.0f, 18.0f, 12.0f, ThemeColors.backgroundControl());
        renderer.drawRoundedOutline(matrix, x, y, 18.0f, 18.0f, 12.0f, 0.75f, ThemeColors.borderPrimary());
        float glyphWidth = FontRegistry.font3.textWidth("@", 9.0f);
        float glyphHeight = FontRegistry.font3.textHeight("@", 9.0f);
        FontRegistry.font3.drawGlyphText(matrix, renderer, "@", x + (18.0f - glyphWidth) * 0.5f, y + (18.0f - glyphHeight) * 0.5f, 9.0f, ThemeColors.textSecondary());
    }

    private void renderStateButton(GuiDrawApi renderer, Matrix4f matrix, GuiBounds row, boolean active) {
        float x = row.getX() + row.getWidth() - 42.0f;
        float y = row.getY() + 7.5f;
        renderer.drawRoundedRectangle(matrix, x, y, 15.0f, 15.0f, 9.0f, active ? ThemeColors.accentTint() : ThemeColors.backgroundControl());
        String glyph = active ? "ON" : ">";
        float width = FontRegistry.font6.textWidth(glyph, 5.0f);
        float height = FontRegistry.font6.textHeight(glyph, 5.0f);
        FontRegistry.font6.drawText(matrix, renderer, glyph, x + (15.0f - width) * 0.5f, y + (15.0f - height) * 0.5f, 5.0f, active ? ThemeColors.accent() : ThemeColors.textSecondary());
    }

    private void renderSaveButton(GuiDrawApi renderer, Matrix4f matrix, GuiBounds row, boolean active) {
        GuiBounds button = this.menuButtonBounds(row);
        int background = active ? ThemeColors.accentTint() : ThemeColors.backgroundControl();
        renderer.drawRoundedRectangle(matrix, button.getX(), button.getY(), button.getWidth(), button.getHeight(), 9.0f, background);
        float width = FontRegistry.font6.textWidth("S", 5.0f);
        float height = FontRegistry.font6.textHeight("S", 5.0f);
        FontRegistry.font6.drawText(matrix, renderer, "S", button.getX() + (button.getWidth() - width) * 0.5f, button.getY() + (button.getHeight() - height) * 0.5f, 5.0f, active ? ThemeColors.accent() : ThemeColors.textSecondary());
    }

    private void renderCreateDialog(GuiDrawApi renderer, Matrix4f matrix) {
        GuiBounds dialog = this.createDialogBounds();
        renderer.drawRoundedRectangle(matrix, this.getBounds().getX(), this.getBounds().getY(), this.getBounds().getWidth(), this.getBounds().getHeight(), 10.5f, ThemeColors.modalScrim());
        renderer.drawRoundedRectangle(matrix, dialog.getX(), dialog.getY(), dialog.getWidth(), dialog.getHeight(), 10.5f, ThemeColors.backgroundPrimary());
        renderer.drawRoundedOutline(matrix, dialog.getX(), dialog.getY(), dialog.getWidth(), dialog.getHeight(), 10.5f, 0.75f, ThemeColors.borderStrong());
        FontRegistry.font7.drawText(matrix, renderer, "Создать конфигурацию", dialog.getX() + 8.0f, dialog.getY() + 8.0f, 8.0f, ThemeColors.textPrimary());
        GuiBounds input = this.createInputBounds();
        renderer.drawRoundedRectangle(matrix, input.getX(), input.getY(), input.getWidth(), input.getHeight(), 6.0f, ThemeColors.formatFieldFill());
        renderer.drawRoundedOutline(matrix, input.getX(), input.getY(), input.getWidth(), input.getHeight(), 6.0f, 0.75f, ThemeColors.borderPrimary());
        String inputText = this.profileName.isEmpty() ? "Название" : this.profileName.toString();
        FontRegistry.font4.drawText(matrix, renderer, inputText, input.getX() + 6.0f, input.getY() + 5.0f, 6.5f, this.profileName.isEmpty() ? ThemeColors.textPlaceholder() : ThemeColors.textPrimary());
        GuiBounds serverToggle = this.serverToggleBounds();
        renderer.drawRoundedRectangle(matrix, serverToggle.getX(), serverToggle.getY(), serverToggle.getWidth(), serverToggle.getHeight(), 6.0f, this.serverSpecific ? ThemeColors.accentTint() : ThemeColors.backgroundControl());
        FontRegistry.font4.drawText(matrix, renderer, this.serverSpecific ? "Для текущего сервера" : "Общий конфиг", serverToggle.getX() + 6.0f, serverToggle.getY() + 4.0f, 6.0f, this.serverSpecific ? ThemeColors.accent() : ThemeColors.textSecondary());
        GuiBounds create = this.createButtonBounds();
        renderer.drawRoundedRectangle(matrix, create.getX(), create.getY(), create.getWidth(), create.getHeight(), 8.0f, this.profileName.isEmpty() ? ThemeColors.backgroundControl() : ThemeColors.accentTint());
        FontRegistry.font4.drawText(matrix, renderer, "ENTER   Создать конфигурацию", create.getX() + 8.0f, create.getY() + 9.0f, 6.0f, this.profileName.isEmpty() ? ThemeColors.textDisabled() : ThemeColors.accent());
    }

    private boolean handleCreateDialogClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return true;
        }
        if (this.serverToggleBounds().contains(mouseX, mouseY)) {
            this.serverSpecific = !this.serverSpecific;
            return true;
        }
        if (this.createButtonBounds().contains(mouseX, mouseY)) {
            this.createProfile();
            return true;
        }
        if (!this.createDialogBounds().contains(mouseX, mouseY)) {
            this.createDialogOpen = false;
        }
        return true;
    }

    private void openCreateDialog() {
        this.profileName.setLength(0);
        this.serverSpecific = false;
        this.createDialogOpen = true;
    }

    private void createProfile() {
        String name = this.profileName.toString().trim();
        if (name.isBlank() || name.length() > 32) {
            this.showStatus("Название должно быть от 1 до 32 символов");
            return;
        }
        ConfigManager manager = WexSideClient.getConfigManager();
        if (manager == null || manager.profileExists(name)) {
            this.showStatus("Конфиг с таким именем уже существует");
            return;
        }
        try {
            manager.saveProfileWithDisplayName(name, this.serverSpecific ? this.currentServer() : "Общий");
            this.createDialogOpen = false;
            this.refresh();
            this.showStatus("Конфиг создан: " + name);
        }
        catch (IOException exception) {
            this.showStatus("Не удалось создать конфиг");
        }
    }

    private void load(LocalConfigEntry entry) {
        ConfigManager manager = WexSideClient.getConfigManager();
        if (manager == null) {
            return;
        }
        try {
            manager.loadProfile(entry.name());
            this.showStatus("Загружен: " + entry.name());
        }
        catch (RuntimeException exception) {
            this.showStatus("Не удалось загрузить конфиг");
        }
    }

    private void save(LocalConfigEntry entry) {
        this.save(entry.name());
    }

    private void save(String profileName) {
        ConfigManager manager = WexSideClient.getConfigManager();
        if (manager == null) {
            return;
        }
        try {
            manager.saveProfile(profileName);
            this.refresh();
            this.showStatus("Сохранён: " + profileName);
        }
        catch (IOException | RuntimeException exception) {
            this.showStatus("Не удалось сохранить конфиг");
        }
    }

    private void saveActiveProfile() {
        String profileName;
        ConfigManager manager = WexSideClient.getConfigManager();
        String string = profileName = manager == null ? null : manager.getCurrentProfileName();
        if (profileName == null || profileName.isBlank()) {
            this.showStatus("Сначала создайте или загрузите конфиг");
            return;
        }
        this.save(profileName);
    }

    private void delete(LocalConfigEntry entry) {
        if (this.isActive(entry)) {
            this.showStatus("Нельзя удалить активный конфиг");
            return;
        }
        if (this.catalog.delete(entry)) {
            this.showStatus("Конфиг удалён: " + entry.name());
        } else {
            this.showStatus("Не удалось удалить конфиг");
        }
    }

    private void openFolder() {
        ConfigManager manager = WexSideClient.getConfigManager();
        if (manager == null) {
            return;
        }
        try {
            manager.openConfigFolder();
        }
        catch (IOException exception) {
            this.showStatus("Не удалось открыть папку конфигов");
        }
    }

    private void refresh() {
        if (this.catalog != null) {
            this.catalog.refresh();
        }
        this.targetScrollOffset = Math.min(this.targetScrollOffset, this.maximumScrollOffset());
    }

    private List<LocalConfigEntry> visibleEntries() {
        return this.catalog == null ? List.of() : this.catalog.entries();
    }

    private GuiBounds tabBounds(int index) {
        return new GuiBounds(this.getBounds().getX() + (float)index * 87.5f, this.getBounds().getY(), 87.5f, 12.0f);
    }

    private GuiBounds rowBounds(int index) {
        return new GuiBounds(this.getBounds().getX() + 8.0f, this.getBounds().getY() + 30.0f + (float)index * 34.0f - this.scrollOffset, this.getBounds().getWidth() - 16.0f, 30.0f);
    }

    private GuiBounds menuButtonBounds(GuiBounds row) {
        return new GuiBounds(row.getX() + row.getWidth() - 22.0f, row.getY() + 7.5f, 15.0f, 15.0f);
    }

    private int rowIndexAt(int mouseX, int mouseY) {
        List<LocalConfigEntry> entries = this.visibleEntries();
        for (int index = 0; index < entries.size(); ++index) {
            if (!this.rowBounds(index).contains(mouseX, mouseY)) continue;
            return index;
        }
        return -1;
    }

    private float maximumScrollOffset() {
        return Math.max(0.0f, (float)this.visibleEntries().size() * 34.0f - Math.max(0.0f, this.getBounds().getHeight() - 30.0f));
    }

    private float toolbarButtonX(int positionFromRight) {
        return this.getBounds().getX() + this.getBounds().getWidth() - 23.0f - (float)positionFromRight * 19.0f;
    }

    private GuiBounds createDialogBounds() {
        return new GuiBounds(this.getBounds().getX() + (this.getBounds().getWidth() - 175.0f) * 0.5f, this.getBounds().getY() + (this.getBounds().getHeight() - 112.0f) * 0.5f, 175.0f, 112.0f);
    }

    private GuiBounds createInputBounds() {
        GuiBounds dialog = this.createDialogBounds();
        return new GuiBounds(dialog.getX() + 8.0f, dialog.getY() + 29.0f, 159.0f, 18.0f);
    }

    private GuiBounds serverToggleBounds() {
        GuiBounds dialog = this.createDialogBounds();
        return new GuiBounds(dialog.getX() + 8.0f, dialog.getY() + 53.0f, 159.0f, 16.0f);
    }

    private GuiBounds createButtonBounds() {
        GuiBounds dialog = this.createDialogBounds();
        return new GuiBounds(dialog.getX() + 8.0f, dialog.getY() + 77.0f, 159.0f, 27.0f);
    }

    private boolean isActive(LocalConfigEntry entry) {
        ConfigManager manager = WexSideClient.getConfigManager();
        return manager != null && entry.name().equals(manager.getCurrentProfileName());
    }

    private String currentServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getCurrentServerEntry() == null ? "Одиночная игра" : client.getCurrentServerEntry().address;
    }

    private boolean hovered(float x, float y, float width, float height) {
        GuiInteractionState interaction = GuiInteractionState.getInstance();
        float mouseX = (float)interaction.getScaledMouseX() - interaction.getRootPanel().getBounds().getX();
        float mouseY = (float)interaction.getScaledMouseY() - interaction.getRootPanel().getBounds().getY();
        return this.contains(mouseX, mouseY, x, y, width, height);
    }

    private boolean contains(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String statusText() {
        if (!this.status.isBlank() && System.currentTimeMillis() < this.statusExpiresAt) {
            return this.status;
        }
        return this.title;
    }

    private void showStatus(String message) {
        this.status = message;
        this.statusExpiresAt = System.currentTimeMillis() + 3000L;
    }
}

