package top.eley.watf;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.eley.watf.item.DuelStaffItem;
import top.eley.watf.item.TeamBattleItem;

public class Watf implements net.fabricmc.api.ModInitializer {

    public static final String MOD_ID = "watf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ========== 道具注册 ==========

    // 决斗法杖
    public static final Item DUEL_STAFF = registerItem(DUEL_STAFF_KEY, new DuelStaffItem());

    // 团战法杖
    public static final Item TEAM_BATTLE = registerItem(TEAM_BATTLE_KEY, new TeamBattleItem());

    public static final ResourceKey<Item> DUEL_STAFF_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "duel_staff")
    );

    public static final ResourceKey<Item> TEAM_BATTLE_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "team_battle")
    );

    private static Item registerItem(ResourceKey<Item> key, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                item
        );
    }

    // ========== 自定义标签页 ==========

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    public static final CreativeModeTab WATF_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            TAB_KEY,
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(DUEL_STAFF))
                    .title(Component.translatable("itemGroup.watf.main"))
                    .build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[WATF] Why Are They Fighting? Mod loaded!");

        // 把道具添加到自定义标签页
        ItemGroupEvents.modifyEntriesEvent(TAB_KEY).register(entries -> {
            entries.prepend(DUEL_STAFF);
            entries.prepend(TEAM_BATTLE);
        });
    }
}