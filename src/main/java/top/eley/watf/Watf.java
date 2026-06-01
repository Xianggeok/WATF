package top.eley.watf;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
    public static final Item DUEL_STAFF = registerItem("duel_staff", new DuelStaffItem());

    // 团战法杖
    public static final Item TEAM_BATTLE = registerItem("team_battle", new TeamBattleItem());

    private static Item registerItem(String name, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name),
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
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(DUEL_STAFF))
                    .title(Component.translatable("itemGroup.watf.main"))
                    .build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[WATF] Why Are They Fighting? Mod loaded!");

        // 把道具添加到自定义标签页
        CreativeModeTabEvents.modifyOutputEvent(TAB_KEY).register(output -> {
            output.prepend(DUEL_STAFF);
            output.prepend(TEAM_BATTLE);
        });
    }
}