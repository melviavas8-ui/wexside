package ru.wexside.module.player;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.BlockInteractEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;

public final class NoInteractModule
extends Module
implements ConfigSerializable {
    private static final List<BlockFilter> FILTERS = List.of(new BlockFilter("Craft-Table", block -> block == Blocks.CRAFTING_TABLE), new BlockFilter("Brewing-Stand", block -> block == Blocks.BREWING_STAND), new BlockFilter("Door", block -> block.getDefaultState().isIn(BlockTags.DOORS)), new BlockFilter("Hopper", block -> block == Blocks.HOPPER), new BlockFilter("Button", block -> block.getDefaultState().isIn(BlockTags.BUTTONS)), new BlockFilter("Note-Block", block -> block == Blocks.NOTE_BLOCK), new BlockFilter("Trap-Door", block -> block.getDefaultState().isIn(BlockTags.TRAPDOORS)), new BlockFilter("Furnace", block -> block == Blocks.FURNACE), new BlockFilter("Chest", block -> block == Blocks.CHEST), new BlockFilter("Trapped-Chest", block -> block == Blocks.TRAPPED_CHEST), new BlockFilter("Ender-Chest", block -> block == Blocks.ENDER_CHEST), new BlockFilter("Gate", block -> block.getDefaultState().isIn(BlockTags.FENCE_GATES)), new BlockFilter("Anvil", block -> block.getDefaultState().isIn(BlockTags.ANVIL)), new BlockFilter("Dispenser", block -> block == Blocks.DISPENSER), new BlockFilter("Lever", block -> block == Blocks.LEVER), new BlockFilter("Sign", block -> block.getDefaultState().isIn(BlockTags.ALL_SIGNS)), new BlockFilter("Shulker-Box", block -> block.getDefaultState().isIn(BlockTags.SHULKER_BOXES)), new BlockFilter("Barrel", block -> block == Blocks.BARREL), new BlockFilter("Smoker", block -> block == Blocks.SMOKER), new BlockFilter("Dropper", block -> block == Blocks.DROPPER), new BlockFilter("Crafter", block -> block == Blocks.CRAFTER), new BlockFilter("Beacon", block -> block == Blocks.BEACON), new BlockFilter("Enchanting-Table", block -> block == Blocks.ENCHANTING_TABLE), new BlockFilter("Grindstone", block -> block == Blocks.GRINDSTONE), new BlockFilter("Stonecutter", block -> block == Blocks.STONECUTTER), new BlockFilter("Loom", block -> block == Blocks.LOOM), new BlockFilter("Cartography-Table", block -> block == Blocks.CARTOGRAPHY_TABLE), new BlockFilter("Smithing-Table", block -> block == Blocks.SMITHING_TABLE), new BlockFilter("Lectern", block -> block == Blocks.LECTERN), new BlockFilter("Chiseled-Bookshelf", block -> block == Blocks.CHISELED_BOOKSHELF), new BlockFilter("Decorated-Pot", block -> block == Blocks.DECORATED_POT), new BlockFilter("Comparator", block -> block == Blocks.COMPARATOR), new BlockFilter("Repeater", block -> block == Blocks.REPEATER), new BlockFilter("Jukebox", block -> block == Blocks.JUKEBOX), new BlockFilter("Bell", block -> block == Blocks.BELL), new BlockFilter("Composter", block -> block == Blocks.COMPOSTER), new BlockFilter("Respawn-Anchor", block -> block == Blocks.RESPAWN_ANCHOR), new BlockFilter("Cauldron", block -> block.getDefaultState().isIn(BlockTags.CAULDRONS)));
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Блокировка взаимодействия с блоками").withKeybind().toggle()).build();
    private final BooleanSetting allBlocks;
    private final MultiSelectSetting ignore;

    public NoInteractModule(EventBus eventBus) {
        super(eventBus, "no_interact", "No Interact", "Блокировка взаимодействия с блоками", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.allBlocks = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("All Blocks").id("all_blocks").description("Блокировка всех блоков из списка")).build();
        this.registerSetting(this.allBlocks);
        MultiSelectSetting ignoreSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Craft-Table", "Brewing-Stand", "Door", "Hopper", "Button", "Note-Block", "Trap-Door", "Furnace", "Chest", "Trapped-Chest", "Ender-Chest", "Gate", "Anvil", "Dispenser", "Lever", "Sign", "Shulker-Box", "Barrel", "Smoker", "Dropper", "Crafter", "Beacon", "Enchanting-Table", "Grindstone", "Stonecutter", "Loom", "Cartography-Table", "Smithing-Table", "Lectern", "Chiseled-Bookshelf", "Decorated-Pot", "Comparator", "Repeater", "Jukebox", "Bell", "Composter", "Respawn-Anchor", "Cauldron").selectAll(false).optionListEnabled(false).name("Ignore").id("ignore").description("Блоки, взаимодействие с которыми блокируется").visibleWhen(() -> !this.allBlocks.isEnabled())).build();
        ignoreSetting.setOptions(new String[0]);
        this.ignore = ignoreSetting;
        this.registerSetting(ignoreSetting);
    }

    @Override
    protected void initialize() {
        this.listen(BlockInteractEvent.class, this::onBlockInteract);
    }

    private void onBlockInteract(BlockInteractEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        if (this.shouldBlock(event.getBlock())) {
            event.cancel();
        }
    }

    private boolean shouldBlock(Block block) {
        boolean all = this.allBlocks.isEnabled();
        List<String> selected = this.ignore.getSelectedOptions();
        for (BlockFilter filter : FILTERS) {
            if (!all && !selected.contains(filter.name) || !filter.matches.test(block)) continue;
            return true;
        }
        return false;
    }

    private record BlockFilter(String name, Predicate<Block> matches) {
    }
}

