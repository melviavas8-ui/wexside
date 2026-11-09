package ru.wexside.render.hand;

/** Shader IDs recovered from the original enum constructor arguments. */
public enum HandMaterialShaderMode {
    FILL(1), GLASS(2), BUBBLE(5), GALAXY(6);

    private final int shaderModeId;
    HandMaterialShaderMode(int shaderModeId) { this.shaderModeId = shaderModeId; }
    public int getShaderModeId() { return shaderModeId; }
}
