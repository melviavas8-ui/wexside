package ru.wexside.module.render;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.LazyMeshModel;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.render.DynamicTransforms;
import ru.wexside.render.model.BuiltInMesh;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class BlockOverlayModule
extends Module
implements ConfigSerializable {
    private static final double MIN_SIZE = 1.0E-4;
    private static final double SNAP_DISTANCE = 6.0;
    private static final float OUTLINE_WIDTH = 2.0f;
    private static final float ANIM_STEP = 0.004f;
    private static final double MAX_LOOK_DISTANCE_SQ = 36.0;
    private static final String[] SHADER_MODES = new String[]{"Static", "Nebula", "Ribbons", "Liquid", "Aurora", "Plasma"};
    private static volatile BlockOverlayModule instance;
    private static final LazyMeshModel MODEL;
    private static final DynamicTransforms TRANSFORMS;
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final ModeSetting mode;
    private final NumberSetting smoothFactor;
    private final BooleanSetting outline;
    private Box smoothedBox;
    private float animation;

    public BlockOverlayModule(EventBus eventBus) {
        super(eventBus, "block_overlay", "Block Overlay", "Подсвечивает блок, на который наведён прицел", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет подсветки").aliases("color", "цвет")).build();
        colorSetting.setPrimaryColor(0, -2140275457);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Static", "Nebula", "Ribbons", "Liquid", "Aurora", "Plasma").defaultOption("Static").name("Mode").id("mode").description("Режим шейдера подсветки").aliases("mode", "режим")).build();
        this.registerSetting(this.mode);
        this.smoothFactor = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 100.0).defaultValue(5.0).multiplier(0.001).precision(0).animationSpeed(20.0f).markers(10.0).snapTo(10.0).name("Smooth factor").id("smooth_factor").description("Плавность перетекания подсветки между блоками").aliases("smooth", "плавность")).build();
        this.registerSetting(this.smoothFactor);
        this.outline = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Outline").id("outline").description("Обводка по контуру блока")).build();
        this.registerSetting(this.outline);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::onWorldRender);
    }

    public static boolean isEnabled() {
        BlockOverlayModule module = instance;
        return module != null && module.enabledSetting.isEnabled();
    }

    private void onWorldRender(WorldRenderEvent event) {
        BlockHitResult blockHit;
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        if (!this.enabledSetting.isEnabled() || client.player == null || client.world == null || cameraPos == null) {
            this.smoothedBox = null;
            return;
        }
        if (AttackAuraModule.isActive4()) {
            this.smoothedBox = null;
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult) || (blockHit = (BlockHitResult)hit).getType() != HitResult.Type.BLOCK) {
            this.smoothedBox = null;
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        VoxelShape shape = client.world.getBlockState(pos).getOutlineShape((BlockView)client.world, pos);
        if (shape.isEmpty()) {
            this.smoothedBox = null;
            return;
        }
        if (client.player.getRotationVec(1.0f).squaredDistanceTo(this.blockCenter(pos)) > 36.0) {
            this.smoothedBox = null;
            return;
        }
        if (!MODEL.isLoaded() || MODEL.getMeshModel() == null || MODEL.getMeshModel().getList().isEmpty()) {
            return;
        }
        Box box = this.smooth(shape.getBoundingBox().offset(pos), this.smoothFactor.getValue());
        SpatialTransform transform = this.toTransform(box);
        if (transform == null) {
            return;
        }
        int fill = this.color.getColor(0.0f);
        int line = fill & 0xFFFFFF | 0xFF000000;
        this.animation += 0.004f;
        if (this.animation > 1000.0f) {
            this.animation = 0.0f;
        }
        TRANSFORMS.renderTransformedMeshes(MODEL.getMeshModel().getList(), cameraPos, fill, line, List.of(transform), this.outline.isEnabled(), 2.0f, this.animation, this.shaderIndex());
    }

    private Vec3d blockCenter(BlockPos pos) {
        return new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    private int shaderIndex() {
        String selected = this.mode.getSelectedOption();
        for (int i = 0; i < SHADER_MODES.length; ++i) {
            if (!SHADER_MODES[i].equals(selected)) continue;
            return i;
        }
        return 0;
    }

    private SpatialTransform toTransform(Box box) {
        if (box == null) {
            return null;
        }
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        if (sizeX <= 1.0E-4 || sizeY <= 1.0E-4 || sizeZ <= 1.0E-4) {
            return null;
        }
        return new SpatialTransform((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5, (float)sizeX, (float)sizeY, (float)sizeZ);
    }

    private Box smooth(Box target, double factor) {
        if (target == null) {
            this.smoothedBox = null;
            return null;
        }
        if (this.smoothedBox == null || this.centerDistance(this.smoothedBox, target) > 6.0) {
            this.smoothedBox = target;
            return this.smoothedBox;
        }
        this.smoothedBox = new Box(this.damp(this.smoothedBox.minX, target.minX, factor), this.damp(this.smoothedBox.minY, target.minY, factor), this.damp(this.smoothedBox.minZ, target.minZ, factor), this.damp(this.smoothedBox.maxX, target.maxX, factor), this.damp(this.smoothedBox.maxY, target.maxY, factor), this.damp(this.smoothedBox.maxZ, target.maxZ, factor));
        return this.smoothedBox;
    }

    private double damp(double current, double target, double factor) {
        double stiffness = 2.0 / Math.max(0.001, factor);
        double decay = Math.exp(-(stiffness * 0.016));
        double delta = current - target;
        return target + (delta + stiffness * delta * 0.016) * decay;
    }

    private double centerDistance(Box a, Box b) {
        double dx = (a.minX + a.maxX) * 0.5 - (b.minX + b.maxX) * 0.5;
        double dy = (a.minY + a.maxY) * 0.5 - (b.minY + b.maxY) * 0.5;
        double dz = (a.minZ + a.maxZ) * 0.5 - (b.minZ + b.maxZ) * 0.5;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static {
        MODEL = LazyMeshModel.create(BuiltInMesh.CUBE);
        TRANSFORMS = new DynamicTransforms();
    }
}

