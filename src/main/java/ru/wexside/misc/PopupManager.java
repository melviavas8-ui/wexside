package ru.wexside.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupOwner;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.ModalPopup;
import ru.wexside.ui.PopupPanel;

public final class PopupManager
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final Map<PopupOwner, PopupOwner> parentsByOwner;
    private final List<PopupOwner> owners = new ArrayList<PopupOwner>();

    public PopupManager() {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.parentsByOwner = new IdentityHashMap<PopupOwner, PopupOwner>();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupOwner owner = this.owners.get(i);
            owner.update2();
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null || !popupPanel.isActive2()) continue;
            if (popupPanel.getBounds().contains(n, n2)) {
                popupPanel.onMouseScroll(n, n2, d);
                return;
            }
            if (this.isInsideAncestorPopup(n, n2, owner)) {
                this.closeBranch(owner);
                continue;
            }
            this.closeBranch(owner);
        }
    }

    @Override
    public void update() {
        for (PopupOwner owner : this.owners) {
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null) continue;
            popupPanel.update();
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        boolean handledPopup = false;
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupOwner owner = this.owners.get(i);
            owner.update2();
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null || !popupPanel.isActive2()) continue;
            handledPopup = true;
            if (popupPanel.getBounds().contains(n, n2)) {
                if (popupPanel.onMousePressed(n, n2, n3)) {
                    return true;
                }
                this.closeBranch(owner);
                return true;
            }
            if (this.isInsideAncestorPopup(n, n2, owner)) {
                this.closeBranch(owner);
                if (!owner.containsScreenPoint(n, n2)) continue;
                return true;
            }
            this.closeBranch(owner);
        }
        return handledPopup;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        return this.renderOpenPopups(f, matrix4f);
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupPanel popupPanel = this.owners.get(i).getPopup();
            if (popupPanel == null || !popupPanel.isActive2()) continue;
            popupPanel.onMouseReleased(n, n2, n3);
        }
    }

    @Override
    public boolean onCharTyped(char c) {
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupPanel popupPanel = this.owners.get(i).getPopup();
            if (popupPanel == null || !popupPanel.isActive2() || !popupPanel.onCharTyped(c)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPressed(int n) {
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupPanel popupPanel = this.owners.get(i).getPopup();
            if (popupPanel == null || !popupPanel.isActive2() || !popupPanel.onKeyPressed(n)) continue;
            return true;
        }
        return false;
    }

    public boolean containsOpenPopup(int n, int n2) {
        for (int i = this.owners.size() - 1; i >= 0; --i) {
            PopupOwner owner = this.owners.get(i);
            owner.update2();
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null || !popupPanel.isActive2() || !popupPanel.getBounds().contains(n, n2)) continue;
            return true;
        }
        return false;
    }

    public ModalPopup getModalPopup() {
        for (PopupOwner owner : this.owners) {
            ModalPopup modalPopup;
            PopupPanel popupPanel = owner.getPopup();
            if (!(popupPanel instanceof ModalPopup) || !(modalPopup = (ModalPopup)popupPanel).isActive()) continue;
            owner.update2();
            return modalPopup;
        }
        return null;
    }

    public void closeAll() {
        for (PopupOwner owner : this.owners) {
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null) continue;
            popupPanel.setBooleanType(false);
        }
    }

    public float renderOpenPopups(float delta, Matrix4f matrix) {
        float bottom = 0.0f;
        for (PopupOwner owner : this.owners) {
            owner.update2();
            PopupPanel popupPanel = owner.getPopup();
            if (popupPanel == null || popupPanel instanceof ModalPopup || !popupPanel.isActive()) continue;
            bottom = popupPanel.render(delta, matrix);
        }
        return bottom;
    }

    public void registerRoot(PopupOwner owner) {
        this.register(owner, null);
    }

    public void closeDescendants(PopupOwner owner) {
        if (owner == null) {
            return;
        }
        for (PopupOwner candidate : this.owners) {
            PopupPanel popupPanel;
            if (!this.isDescendant(candidate, owner) || (popupPanel = candidate.getPopup()) == null) continue;
            popupPanel.setBooleanType(false);
        }
    }

    private boolean isInsideAncestorPopup(int x, int y, PopupOwner owner) {
        PopupOwner ancestor = this.parentsByOwner.get(owner);
        while (ancestor != null) {
            PopupPanel popupPanel = ancestor.getPopup();
            if (popupPanel != null && popupPanel.isActive2() && popupPanel.getBounds().contains(x, y)) {
                return true;
            }
            ancestor = this.parentsByOwner.get(ancestor);
        }
        return false;
    }

    public List<PopupOwner> getOwners() {
        return Collections.unmodifiableList(this.owners);
    }

    public void toggle(PopupOwner owner) {
        PopupPanel popupPanel;
        PopupPanel popupPanel2 = popupPanel = owner == null ? null : owner.getPopup();
        if (popupPanel == null) {
            return;
        }
        if (popupPanel.isActive2()) {
            this.closeBranch(owner);
            return;
        }
        this.closeUnrelated(owner);
        popupPanel.setBooleanType(true);
    }

    public void closeBranch(PopupOwner owner) {
        this.closeOwnerAndDescendants(owner);
    }

    public void register(PopupOwner owner, PopupOwner parent) {
        if (owner == null || this.owners.contains(owner)) {
            return;
        }
        this.owners.add(owner);
        this.parentsByOwner.put(owner, parent);
        PopupPanel popupPanel = owner.getPopup();
        if (popupPanel != null && !this.owners.contains(popupPanel)) {
            this.addChild(popupPanel);
        }
        owner.setPopupManager(this);
    }

    private boolean isDescendant(PopupOwner candidate, PopupOwner ancestor) {
        PopupOwner parent = this.parentsByOwner.get(candidate);
        while (parent != null) {
            if (parent == ancestor) {
                return true;
            }
            parent = this.parentsByOwner.get(parent);
        }
        return false;
    }

    private void closeUnrelated(PopupOwner owner) {
        Set ownerChain = Collections.newSetFromMap(new IdentityHashMap());
        PopupOwner current = owner;
        while (current != null) {
            ownerChain.add(current);
            current = this.parentsByOwner.get(current);
        }
        for (PopupOwner candidate : this.owners) {
            PopupPanel popupPanel;
            if (ownerChain.contains(candidate) || (popupPanel = candidate.getPopup()) == null) continue;
            popupPanel.setBooleanType(false);
        }
    }

    private void closeOwnerAndDescendants(PopupOwner owner) {
        if (owner == null) {
            return;
        }
        for (PopupOwner candidate : this.owners) {
            PopupPanel popupPanel;
            if (candidate != owner && !this.isDescendant(candidate, owner) || (popupPanel = candidate.getPopup()) == null) continue;
            popupPanel.setBooleanType(false);
        }
    }

    public Map<PopupOwner, PopupOwner> getParentMap() {
        return Collections.unmodifiableMap(this.parentsByOwner);
    }
}

