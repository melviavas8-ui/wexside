package ru.wexside.input;

import java.util.Objects;
import ru.wexside.input.BindDevice;

public final class BindInput {
    private static final BindInput UNBOUND = new BindInput(BindDevice.NONE, 0);
    private final BindDevice device;
    private final int code;

    public BindInput(BindDevice device, int code) {
        this.device = Objects.requireNonNull(device, "device");
        this.code = device == BindDevice.NONE ? 0 : code;
    }

    public static BindInput unbound() {
        return UNBOUND;
    }

    public static BindInput fromLegacyCode(int code) {
        if (code == 0) {
            return UNBOUND;
        }
        return code < 0 ? BindInput.mouse(code + 100) : BindInput.keyboard(code);
    }

    public static BindInput keyboard(int keyCode) {
        return new BindInput(BindDevice.KEYBOARD, keyCode);
    }

    public static BindInput mouse(int buttonCode) {
        return new BindInput(BindDevice.MOUSE, buttonCode);
    }

    public int toLegacyCode() {
        return switch (this.device) {
            default -> throw new MatchException(null, null);
            case BindDevice.NONE -> 0;
            case BindDevice.KEYBOARD -> this.code;
            case BindDevice.MOUSE -> this.code - 100;
        };
    }

    public boolean matchesKeyboard(int keyCode) {
        return this.device == BindDevice.KEYBOARD && this.code == keyCode;
    }

    public boolean matchesMouse(int buttonCode) {
        return this.device == BindDevice.MOUSE && this.code == buttonCode;
    }

    public boolean isUnbound() {
        return this.device == BindDevice.NONE;
    }

    public boolean isKeyboard() {
        return this.device == BindDevice.KEYBOARD;
    }

    public boolean isMouse() {
        return this.device == BindDevice.MOUSE;
    }

    public BindDevice device() {
        return this.device;
    }

    public int code() {
        return this.code;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BindInput)) return false;
        BindInput other = (BindInput)object;
        if (this.device != other.device) return false;
        if (this.code != other.code) return false;
        return true;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.device, this.code});
    }

    public String toString() {
        return "BindInput[device=" + String.valueOf((Object)this.device) + ", code=" + this.code + "]";
    }
}

