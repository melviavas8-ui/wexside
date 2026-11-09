package ru.wexside.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.item.ItemBadge;
import ru.wexside.item.ItemBadgeCategory;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FriendList;
import ru.wexside.misc.NameTagSettings;
import ru.wexside.model.esp.EspTargetClassifier;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.module.player.NameProtectModule;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.NameplateLayout;
import ru.wexside.render.RenderCamera;
import ru.wexside.render.RenderProjection;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;

public final class NameplateRenderer {
    private static final float value5 = 1.15f;
    private static final float value6 = 11.0f;
    private final int backgroundColor;
    private static final float value7 = 6.25f;
    private final int specialItemBackgroundColor;
    private static final float value8 = 5.0f;
    private static final int slot3 = 0xFF5555;
    private static final int process19 = 0xFFFF00;
    static volatile NameplateRenderer fire2;
    private static final float value9 = 2.0f;
    private static final int slot4 = -1;
    private static final MsdfFontRenderer enchantmentFont;
    private static final EquipmentSlot[] ARMOR_SLOTS;
    private static final int slot5 = 0x55FF55;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float value10 = 2.0f;
    private final Text friendSuffix;
    private static final int slot6 = 0xAAAAAA;
    private static final float value11 = 1.15f;
    private final Text separator;
    private static final MsdfFontRenderer nameFont;
    private final EspFeatureRegistry espFeatures;
    private final ItemIconCache itemIcons;
    private final int friendBackgroundColor;
    private static final int slot8 = 0x808080;

    public NameplateRenderer(EventBus eventBus, EspFeatureRegistry espFeatures) {
        this.espFeatures = espFeatures;
        this.itemIcons = new ItemIconCache();
        this.backgroundColor = -871362544;
        this.specialItemBackgroundColor = -869654000;
        this.friendBackgroundColor = -871355880;
        this.separator = Text.literal((String)" ");
        this.friendSuffix = Text.literal((String)"[Friend]").withColor(0x55FF55);
        eventBus.subscribe(HudRenderEvent.class, this::setHudRenderEvent, -100);
        eventBus.subscribe(WorldSessionEvent.class, this::onWorldChanged);
        fire2 = this;
    }

    private void onWorldChanged(WorldSessionEvent event) {
        this.itemIcons.close();
    }

    private boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }

    private MutableText buildLabel(Entity entity, NameTagSettings settings) {
        Text text;
        boolean localPlayer = entity == this.mc.player;
        FriendList friends = WexSideClient.getFriends();
        boolean friend = friends != null && friends.contains(entity.getName().getString());
        boolean bl = friend;
        if (entity instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)entity;
            text = item.getStack().getName();
        } else {
            text = entity.getDisplayName();
        }
        Text displayName = text;
        MutableText label = displayName != null ? displayName.copy() : entity.getName().copy();
        MutableText mutableText = label;
        if (displayName != null && displayName.getContent() instanceof TranslatableTextContent) {
            label = displayName.copy().fillStyle(Style.EMPTY.withColor(0x808080));
        }
        if (friend && !localPlayer && NameProtectModule.isEnabled()) {
            label = Text.literal((String)NameProtectModule.getString());
        }
        if (settings.isHealthVisible() && entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            label.append(this.separator).append((Text)this.bracketedText(0xFF5555, String.valueOf(this.getHealth(entity, living))));
        }
        int reward = settings.isMoneyVisible() && entity instanceof HostileEntity ? this.getMobReward(entity) : 0;
        int n = reward;
        if (reward != 0) {
            label.append(this.separator).append((Text)this.bracketedText(0xFFFF00, "~" + reward + "$"));
        }
        if (friend && !localPlayer) {
            label.append(this.separator).append(this.friendSuffix);
        }
        return label;
    }

    private float equipmentCenterX(NameplateLayout nameplateLayout, int n) {
        return nameplateLayout.centerX + ((float)n - (float)(nameplateLayout.equipment.size() - 1) / 2.0f) * 11.0f;
    }

    private static int enchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        ItemEnchantmentsComponent espFeatures = EnchantmentHelper.getEnchantments((ItemStack)stack);
        for (Object2IntMap.Entry entry : espFeatures.getEnchantmentEntries()) {
            if (!((RegistryEntry)entry.getKey()).matchesKey(enchantment)) continue;
            return entry.getIntValue();
        }
        return 0;
    }

    private float drawTextRun(GuiDrawApi drawApi, Matrix4f matrix4f, StringBuilder stringBuilder, float f, float f2, float f3, int n) {
        String string = stringBuilder.toString();
        stringBuilder.setLength(0);
        nameFont.drawText(matrix4f, drawApi, string, f, f2, f3, n);
        return nameFont.textWidth(string, f3);
    }

    private List<String> enchantmentLabels(ItemStack stack) {
        ArrayList<String> arrayList = new ArrayList<String>();
        int n = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.PROTECTION);
        int n2 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.THORNS);
        int n3 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.UNBREAKING);
        int n4 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.MENDING);
        int n5 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.FEATHER_FALLING);
        int n6 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.DEPTH_STRIDER);
        int n7 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.SHARPNESS);
        int n8 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.LOOTING);
        int n9 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.INFINITY);
        int n10 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.POWER);
        int n11 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.PUNCH);
        int n12 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.FLAME);
        int n13 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.KNOCKBACK);
        int n14 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.FIRE_ASPECT);
        int n15 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.EFFICIENCY);
        int n16 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.SILK_TOUCH);
        int n17 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.FORTUNE);
        int n18 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.FIRE_PROTECTION);
        int n19 = NameplateRenderer.enchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.BLAST_PROTECTION);
        if (this.isAxe(stack)) {
            if (n7 > 0) {
                int n20 = n7;
                arrayList.add("Shr" + n20);
            }
            if (n15 > 0) {
                int n21 = n15;
                arrayList.add("Eff" + n21);
            }
            if (n3 > 0) {
                int n22 = n3;
                arrayList.add("Unb" + n22);
            }
        }
        if (this.isArmorEnchantable(stack)) {
            if (n18 > 0) {
                int n23 = n18;
                arrayList.add("Fire" + n23);
            }
            if (n19 > 0) {
                int n24 = n19;
                arrayList.add("Bla" + n24);
            }
            if (n6 > 0) {
                int n25 = n6;
                arrayList.add("Dep" + n25);
            }
            if (n5 > 0) {
                int n26 = n5;
                arrayList.add("Fea" + n26);
            }
            if (n > 0) {
                int n27 = n;
                arrayList.add("Pro" + n27);
            }
            if (n2 > 0) {
                int n28 = n2;
                arrayList.add("Thr" + n28);
            }
            if (n4 > 0) {
                arrayList.add("Mn");
            }
            if (n3 > 0) {
                int n29 = n3;
                arrayList.add("Unb" + n29);
            }
        }
        if (this.isBow(stack)) {
            if (n9 > 0) {
                int n30 = n9;
                arrayList.add("Inf" + n30);
            }
            if (n10 > 0) {
                int n31 = n10;
                arrayList.add("Pow" + n31);
            }
            if (n11 > 0) {
                int n32 = n11;
                arrayList.add("Pun" + n32);
            }
            if (n4 > 0) {
                arrayList.add("Mn");
            }
            if (n12 > 0) {
                int n33 = n12;
                arrayList.add("Fla" + n33);
            }
            if (n3 > 0) {
                int n34 = n3;
                arrayList.add("Unb" + n34);
            }
        }
        if (this.isSword(stack)) {
            if (n8 > 0) {
                int n35 = n8;
                arrayList.add("L" + n35);
            }
            if (n7 > 0) {
                int n36 = n7;
                arrayList.add("Shr" + n36);
            }
            if (n13 > 0) {
                int n37 = n13;
                arrayList.add("Kno" + n37);
            }
            if (n14 > 0) {
                int n38 = n14;
                arrayList.add("Fir" + n38);
            }
            if (n3 > 0) {
                int n39 = n3;
                arrayList.add("Unb" + n39);
            }
            if (n4 > 0) {
                arrayList.add("Mn");
            }
        }
        if (this.isMiningTool(stack)) {
            if (n3 > 0) {
                int n40 = n3;
                arrayList.add("Unb" + n40);
            }
            if (n4 > 0) {
                arrayList.add("Mn");
            }
            if (n15 > 0) {
                int n41 = n15;
                arrayList.add("Eff" + n41);
            }
            if (n16 > 0) {
                int n42 = n16;
                arrayList.add("Sil" + n42);
            }
            if (n17 > 0) {
                int n43 = n17;
                arrayList.add("For" + n43);
            }
        }
        return arrayList;
    }

    private void renderBadgeLabel(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        Text iIiilliIII2 = nameplateLayout.badge.label();
        float f = this.formattedTextWidth(iIiilliIII2, 6.25f);
        this.drawFormattedText(drawApi, matrix4f, iIiilliIII2, nameplateLayout.centerX - f / 2.0f, this.badgeLabelY(nameplateLayout), 6.25f);
    }

    private void renderNameBackground(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        float f = nameFont.textHeight(nameplateLayout.label.getString(), nameplateLayout.fontSize);
        float f2 = nameplateLayout.labelWidth;
        float f3 = nameplateLayout.centerX - f2 / 2.0f;
        float f4 = f3 - 1.5f;
        float f5 = nameplateLayout.baselineY - f - 2.5f;
        float f6 = f3 + f2 + 1.5f - f4;
        float f7 = nameplateLayout.baselineY - 2.0f - f5;
        drawApi.fillRectangle(matrix4f, f4, f5, f6, f7, nameplateLayout.friend ? this.friendBackgroundColor : this.backgroundColor);
    }

    private boolean isArmorEnchantable(ItemStack stack) {
        return stack.isIn(ItemTags.ARMOR_ENCHANTABLE);
    }

    private boolean shouldRenderEntity(Entity entity2, Frustum frustum2) {
        EspTargetType espTargetType = EspTargetClassifier.targetType(entity2, this.mc.player);
        NameTagSettings talisman = this.espFeatures.getNameTagSettings(espTargetType, EspTargetClassifier.relation(entity2));
        return espTargetType != null && talisman != null && talisman.isEnabled() && entity2.isAlive() && RenderProjection.isVisible(entity2, frustum2) && (entity2 != this.mc.player || !this.mc.options.getPerspective().isFirstPerson());
    }

    private float formattedTextWidth(Text iIiilliIII2, float f) {
        float[] fArray = new float[]{0.0f};
        StringBuilder stringBuilder = new StringBuilder(16);
        iIiilliIII2.visit((style, string) -> {
            String string2 = string;
            stringBuilder.setLength(0);
            for (int i = 0; i < string2.length(); ++i) {
                char c = string2.charAt(i);
                if ((c == '\u00a7' || c == '&') && i + 1 < string2.length() && Formatting.byCode((char)Character.toLowerCase(string2.charAt(i + 1))) != null) {
                    ++i;
                    continue;
                }
                stringBuilder.append(c);
            }
            if (stringBuilder.length() > 0) {
                fArray[0] = fArray[0] + nameFont.textWidth(stringBuilder.toString(), f);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return fArray[0];
    }

    private float rectangleOverlapRatio(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        float f9;
        float f10 = Math.max(0.0f, Math.min(f + f3, f5 + f7) - Math.max(f, f5));
        float f11 = f10 * (f9 = Math.max(0.0f, Math.min(f2 + f4, f6 + f8) - Math.max(f2, f6)));
        if (f11 == 0.0f) {
            return 0.0f;
        }
        return f11 / Math.min(f3 * f4, f7 * f8);
    }

    private float enchantmentBaselineY(NameplateLayout nameplateLayout) {
        if (nameplateLayout.badge != null) {
            return this.badgeLabelY(nameplateLayout) - nameFont.textHeight("Ag", 6.25f) - 2.0f;
        }
        return nameplateLayout.baselineY - 23.0f - 10.0f;
    }

    public static boolean hasCustomNameplate(Entity entity2) {
        NameplateRenderer fire = fire2;
        if (fire == null) {
            return false;
        }
        MinecraftClient mc2 = MinecraftClient.getInstance();
        EspTargetType espTargetType = EspTargetClassifier.targetType(entity2, mc2.player);
        NameTagSettings talisman = fire.espFeatures.getNameTagSettings(espTargetType, EspTargetClassifier.relation(entity2));
        return espTargetType != null && talisman != null && talisman.isEnabled() && (entity2 != mc2.player || !mc2.options.getPerspective().isFirstPerson());
    }

    private float badgeLabelY(NameplateLayout nameplateLayout) {
        return nameplateLayout.baselineY - 23.0f - 9.0f;
    }

    private int getHealth(Entity entity, LivingEntity living) {
        Scoreboard scoreboard = this.mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        ReadableScoreboardScore score = objective == null ? null : scoreboard.getScore((ScoreHolder)entity, objective);
        ReadableScoreboardScore readableScoreboardScore = score;
        if (score != null) {
            return score.getScore();
        }
        return Math.round(living.getHealth());
    }

    private List<ItemStack> collectEquipment(LivingEntity entity3) {
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>();
        arrayList.add(entity3.getMainHandStack());
        arrayList.add(entity3.getOffHandStack());
        for (EquipmentSlot iliiIIiliI2 : ARMOR_SLOTS) {
            arrayList.add(entity3.getEquippedStack(iliiIIiliI2));
        }
        arrayList.removeIf(ItemStack::isEmpty);
        return arrayList;
    }

    private void renderEquipmentBackgrounds(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        float f = nameplateLayout.baselineY - 23.0f;
        for (int i = 0; i < nameplateLayout.equipment.size(); ++i) {
            float f2 = this.equipmentCenterX(nameplateLayout, i);
            boolean bl = this.isSpecialEquipment(nameplateLayout.equipment.get(i));
            drawApi.fillRectangle(matrix4f, f2 - 5.0f, f, 10.0f, 10.0f, bl ? this.specialItemBackgroundColor : this.backgroundColor);
        }
    }

    private boolean isMiningTool(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.SHOVELS) || stack.isIn(ItemTags.HOES);
    }

    private float enchantmentFontSize(NameplateLayout nameplateLayout) {
        float f = 0.0f;
        for (ItemStack stack : nameplateLayout.equipment) {
            for (String string : this.enchantmentLabels(stack)) {
                f = Math.max(f, enchantmentFont.textWidth(string, 5.0f));
            }
        }
        float f2 = 10.5f;
        if (f > f2) {
            return 5.0f * f2 / f;
        }
        return 5.0f;
    }

    private boolean isFriend(Entity entity2) {
        if (entity2 == this.mc.player) {
            return false;
        }
        FriendList friendList2 = WexSideClient.getFriends();
        return friendList2 != null && friendList2.contains(entity2.getName().getString());
    }

    private int getMobReward(Entity entity2) {
        if (entity2 instanceof SpiderEntity) {
            return 700;
        }
        if (entity2 instanceof CreeperEntity) {
            return 2000;
        }
        if (entity2 instanceof StrayEntity) {
            return 4000;
        }
        if (entity2 instanceof SkeletonEntity || entity2 instanceof ZombieEntity) {
            return 600;
        }
        if (entity2 instanceof EndermanEntity) {
            return 1000;
        }
        if (entity2 instanceof WitherEntity) {
            return 10000;
        }
        return 0;
    }

    private boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void setHudRenderEvent(HudRenderEvent gameEvent20) {
        if (!this.espFeatures.hasEnabledNameTags()) {
            this.itemIcons.close();
            return;
        }
        if (this.mc.world == null || this.mc.player == null || RenderCamera.position() == null) {
            return;
        }
        Frustum frustum = RenderProjection.frustum();
        Matrix4f viewProjection = RenderProjection.viewProjectionMatrix();
        ArrayList<Entity> visibleEntities = new ArrayList<Entity>();
        for (Entity entity2 : this.mc.world.getEntities()) {
            if (!this.shouldRenderEntity(entity2, frustum)) continue;
            visibleEntities.add(entity2);
        }
        if (visibleEntities.isEmpty()) {
            return;
        }
        visibleEntities.sort(Comparator.comparingDouble(entity -> this.mc.player.getEntityPos().squaredDistanceTo(entity.getEntityPos())));
        ArrayList<NameplateLayout> nameplates = new ArrayList<NameplateLayout>();
        ArrayList<float[]> occupiedBounds = new ArrayList<float[]>();
        for (Entity class_12972 : visibleEntities) {
            LivingEntity living;
            List<ItemStack> equipment;
            float f4;
            NameTagSettings settings = this.espFeatures.getNameTagSettings(EspTargetClassifier.targetType(class_12972, this.mc.player), EspTargetClassifier.relation(class_12972));
            Vector2f projected = RenderProjection.projectEntityLabel(class_12972, viewProjection);
            if (settings == null || projected == null) continue;
            MutableText label = this.buildLabel(class_12972, settings);
            String string = label.getString();
            float f = string.contains("\u2605") ? 9.0f : 7.0f;
            float f5 = f4 = this.formattedTextWidth((Text)label, f);
            float f6 = nameFont.textHeight(string, f) + 4.0f;
            float f7 = projected.x - f5 / 2.0f;
            float f8 = projected.y - f6;
            if (settings.shouldPreventOverlap()) {
                boolean overlaps = false;
                for (float[] bounds : occupiedBounds) {
                    if (!((double)this.rectangleOverlapRatio(f7, f8, f5, f6, bounds[0], bounds[1], bounds[2], bounds[3]) > settings.getOverlapThreshold())) continue;
                    overlaps = true;
                    break;
                }
                if (overlaps) continue;
                occupiedBounds.add(new float[]{f7, f8, f5, f6});
            }
            NameplateLayout nameplate = new NameplateLayout(projected.x, projected.y, label, f, f4);
            nameplate.friend = this.isFriend(class_12972);
            nameplate.showEnchantments = settings.shouldShowEnchantments();
            if (settings.areItemsVisible() && class_12972 instanceof LivingEntity && !(equipment = this.collectEquipment(living = (LivingEntity)class_12972)).isEmpty()) {
                nameplate.equipment = equipment;
            }
            if (class_12972 instanceof LivingEntity) {
                living = (LivingEntity)class_12972;
                nameplate.badge = NameplateRenderer.offhandBadge(living, settings.isTalismanVisible(), settings.isSphereVisible());
            }
            nameplates.add(nameplate);
        }
        if (nameplates.isEmpty()) {
            return;
        }
        float scale = this.mc.getWindow().getScaleFactor();
        this.itemIcons.beginFrame();
        for (NameplateLayout nameplate : nameplates) {
            if (nameplate.equipment == null) continue;
            nameplate.equipmentIcons = new ArrayList<BakedItemIcon>(nameplate.equipment.size());
            for (ItemStack stack : nameplate.equipment) {
                nameplate.equipmentIcons.add(this.itemIcons.get(stack));
            }
        }
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        this.itemIcons.collectBakeEntries(scale, arrayList);
        if (!arrayList.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList);
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f guiMatrix = new Matrix4f().scale(scale);
        renderer.begin();
        try {
            for (NameplateLayout nameplate : nameplates) {
                this.renderNameBackground(renderer, guiMatrix, nameplate);
                if (nameplate.badge != null) {
                    this.renderBadgeBackground(renderer, guiMatrix, nameplate);
                }
                if (nameplate.equipment == null) continue;
                this.renderEquipmentBackgrounds(renderer, guiMatrix, nameplate);
            }
            for (NameplateLayout nameplate : nameplates) {
                if (nameplate.equipment == null) continue;
                this.renderEquipmentIcons(renderer, guiMatrix, nameplate);
            }
        }
        finally {
            renderer.end();
        }
        renderer.begin();
        try {
            for (NameplateLayout nameplate : nameplates) {
                this.renderNameLabel(renderer, guiMatrix, nameplate);
                if (nameplate.badge == null) continue;
                this.renderBadgeLabel(renderer, guiMatrix, nameplate);
            }
            for (NameplateLayout nameplate : nameplates) {
                if (nameplate.equipment == null || !nameplate.showEnchantments) continue;
                this.renderEnchantmentLabels(renderer, guiMatrix, nameplate);
            }
        }
        finally {
            renderer.end();
        }
        this.itemIcons.evictUnused();
    }

    private boolean isSpecialEquipment(ItemStack stack) {
        BlockItem text2;
        Item iiIilIIilI2 = stack.getItem();
        boolean bl = iiIilIIilI2 instanceof BlockItem && (text2 = (BlockItem)iiIilIIilI2).getBlock() instanceof AbstractSkullBlock;
        return bl || stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private void renderEnchantmentLabels(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        float f = this.enchantmentFontSize(nameplateLayout);
        float f2 = enchantmentFont.textHeight("Ag", f) + 1.0f;
        float f3 = this.enchantmentBaselineY(nameplateLayout);
        for (int i = 0; i < nameplateLayout.equipment.size(); ++i) {
            float f4 = this.equipmentCenterX(nameplateLayout, i);
            List<String> list = this.enchantmentLabels(nameplateLayout.equipment.get(i));
            for (int j = 0; j < list.size(); ++j) {
                String string = list.get(j);
                float f5 = enchantmentFont.textWidth(string, f);
                enchantmentFont.drawText(matrix4f, drawApi, string, f4 - f5 / 2.0f, f3 - (float)j * f2, f, -1);
            }
        }
    }

    private void renderBadgeBackground(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        Text iIiilliIII2 = nameplateLayout.badge.label();
        float f = this.formattedTextWidth(iIiilliIII2, 6.25f);
        float f2 = nameFont.textHeight(iIiilliIII2.getString(), 6.25f);
        float f3 = nameplateLayout.centerX - f / 2.0f;
        float f4 = this.badgeLabelY(nameplateLayout);
        drawApi.fillRectangle(matrix4f, f3 - 1.5f, f4 - 1.0f, f + 3.0f, f2 + 2.0f, this.backgroundColor);
    }

    private void renderEquipmentIcons(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        float f = nameplateLayout.baselineY - 23.0f;
        for (int i = 0; i < nameplateLayout.equipment.size(); ++i) {
            float f2 = this.equipmentCenterX(nameplateLayout, i);
            this.itemIcons.drawBakedIcon(drawApi, matrix4f, nameplateLayout.equipmentIcons.get(i), f2 - 4.5f, f + 0.5f, 9.0f);
        }
    }

    private boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    private MutableText bracketedText(int color, String value) {
        return Text.empty().append((Text)Text.literal((String)"[").withColor(0xAAAAAA)).append((Text)Text.literal((String)value).withColor(color)).append((Text)Text.literal((String)"]").withColor(0xAAAAAA));
    }

    private void renderNameLabel(GuiDrawApi drawApi, Matrix4f matrix4f, NameplateLayout nameplateLayout) {
        float f3 = nameFont.textHeight(nameplateLayout.label.getString(), nameplateLayout.fontSize);
        float f4 = nameplateLayout.labelWidth;
        float f5 = nameplateLayout.centerX - f4 / 2.0f;
        float f6 = nameplateLayout.baselineY - f3 - 2.5f;
        this.drawFormattedText(drawApi, matrix4f, (Text)nameplateLayout.label, f5, f6, nameplateLayout.fontSize);
    }

    private void drawFormattedText(GuiDrawApi drawApi, Matrix4f matrix4f, Text iIiilliIII2, float f, float f2, float f3) {
        float[] fArray = new float[]{f};
        StringBuilder stringBuilder = new StringBuilder(16);
        iIiilliIII2.visit((style, string) -> {
            int baseColor;
            String string2 = string;
            TextColor textColor = style.getColor();
            int currentColor = baseColor = 0xFF000000 | (textColor != null ? textColor.getRgb() : 0xFFFFFF);
            for (int i = 0; i < string2.length(); ++i) {
                Formatting formatting;
                char c = string2.charAt(i);
                if ((c == '\u00a7' || c == '&') && i + 1 < string2.length() && (formatting = Formatting.byCode((char)Character.toLowerCase(string2.charAt(i + 1)))) != null) {
                    int nextColor = formatting == Formatting.RESET ? baseColor : (formatting.getColorValue() != null ? 0xFF000000 | formatting.getColorValue() : currentColor);
                    if (nextColor != currentColor && stringBuilder.length() > 0) {
                        fArray[0] = fArray[0] + this.drawTextRun(drawApi, matrix4f, stringBuilder, fArray[0], f2, f3, currentColor);
                    }
                    currentColor = nextColor;
                    ++i;
                    continue;
                }
                stringBuilder.append(c);
            }
            if (stringBuilder.length() > 0) {
                fArray[0] = fArray[0] + this.drawTextRun(drawApi, matrix4f, stringBuilder, fArray[0], f2, f3, currentColor);
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static ItemBadge offhandBadge(LivingEntity entity3, boolean bl, boolean bl2) {
        if (!bl && !bl2) {
            return null;
        }
        ItemBadge itemBadge = ItemBadge.fromStack(entity3.getOffHandStack());
        if (itemBadge == null) {
            return null;
        }
        boolean enabledForCategory = itemBadge.category() == ItemBadgeCategory.TALISMAN ? bl : bl2;
        return enabledForCategory ? itemBadge : null;
    }

    static {
        enchantmentFont = FontRegistry.icons;
        ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        nameFont = FontRegistry.regularText;
    }
}

