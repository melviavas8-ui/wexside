package ru.wexside.misc;

import java.util.Arrays;
import net.minecraft.util.Identifier;
import ru.wexside.misc.SpriteAtlasRegion;

public enum WexsideHitParticles {
    CROSS("Cross", 0),
    DOLLAR("Dollar", 1),
    STAR("Star", 2),
    BLOOM("Bloom", 3),
    SNOWFLAKE("Snowflake", 4),
    LINE("Line", 5),
    LIGHT("Light", 6);

    private static final int CELL_WIDTH = 1064;
    private static final int CELL_HEIGHT = 1066;
    private static final float ATLAS_WIDTH = 7449.0f;
    private static final float ATLAS_HEIGHT = 2133.0f;
    private static final Identifier ATLAS;
    private static final String[] LABELS;
    private final String label;
    private final SpriteAtlasRegion primarySprite;
    private final SpriteAtlasRegion secondarySprite;

    private WexsideHitParticles(String label, int column) {
        this.label = label;
        int left = column * 1064;
        int right = left + 1064;
        this.primarySprite = WexsideHitParticles.sprite(label, left, 0, right, 1066);
        this.secondarySprite = WexsideHitParticles.sprite(label, left, 1066, right, 2132);
    }

    public String getString() {
        return this.label;
    }

    public static String[] getString2() {
        return (String[])LABELS.clone();
    }

    public static Identifier getParticleTexture() {
        return ATLAS;
    }

    public SpriteAtlasRegion getSpriteAtlasRegion2() {
        return this.primarySprite;
    }

    public SpriteAtlasRegion getSpriteAtlasRegion() {
        return this.secondarySprite;
    }

    public static WexsideHitParticles fromLabel(String label) {
        for (WexsideHitParticles particle : WexsideHitParticles.values()) {
            if (!particle.label.equalsIgnoreCase(label)) continue;
            return particle;
        }
        return CROSS;
    }

    public SpriteAtlasRegion getSprite(boolean primary) {
        return primary ? this.primarySprite : this.secondarySprite;
    }

    private static SpriteAtlasRegion sprite(String name, int left, int top, int right, int bottom) {
        float u1 = ((float)left + 2.0f) / 7449.0f;
        float v1 = ((float)top + 2.0f) / 2133.0f;
        float u2 = ((float)right - 2.0f) / 7449.0f;
        float v2 = ((float)bottom - 2.0f) / 2133.0f;
        return new SpriteAtlasRegion(name, u1, v1, u2, v2);
    }

    static {
        ATLAS = Identifier.of((String)"wexside", (String)"textures/visuals/particles.png");
        LABELS = (String[])Arrays.stream(WexsideHitParticles.values()).map(WexsideHitParticles::getString).toArray(String[]::new);
    }
}

