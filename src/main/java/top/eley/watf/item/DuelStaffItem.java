package top.eley.watf.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.eley.watf.mixin.PiglinAiInvoker;
import top.eley.watf.mixin.PiglinBruteAiInvoker;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 决斗法杖 - 选中两个生物让它们1v1
 */
public class DuelStaffItem extends Item {

    private static final Map<UUID, Entity> FIRST_TARGET = new HashMap<>();
    private static final double MAX_USE_DISTANCE = 15.0;

    public DuelStaffItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(64)
                .setId(top.eley.watf.Watf.DUEL_STAFF_KEY)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.watf.duel_staff.desc"));
        tooltip.add(Component.translatable("item.watf.duel_staff.desc2"));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        Entity targetEntity = getTargetEntity(player, level);

        if (targetEntity == null) {
            player.displayClientMessage(Component.translatable("msg.watf.no_target"), true);
            return InteractionResult.FAIL;
        }

        if (!(targetEntity instanceof LivingEntity livingTarget)) {
            player.displayClientMessage(Component.translatable("msg.watf.cannot_fight"), true);
            return InteractionResult.FAIL;
        }

        if (targetEntity instanceof AgeableMob) {
            player.displayClientMessage(Component.translatable("msg.watf.too_passive"), true);
            return InteractionResult.FAIL;
        }

        if (targetEntity.equals(player)) {
            player.displayClientMessage(Component.translatable("msg.watf.no_self"), true);
            return InteractionResult.FAIL;
        }

        UUID playerId = player.getUUID();

        if (!FIRST_TARGET.containsKey(playerId)) {
            FIRST_TARGET.put(playerId, targetEntity);
            String name = targetEntity.getName().getString();
            player.displayClientMessage(Component.translatable("msg.watf.first_selected", name), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResult.SUCCESS;
        } else {
            Entity firstEntity = FIRST_TARGET.remove(playerId);

            if (firstEntity.equals(targetEntity)) {
                player.displayClientMessage(Component.translatable("msg.watf.different_opponent"), true);
                return InteractionResult.FAIL;
            }

            if (!firstEntity.isAlive() || firstEntity.isRemoved()) {
                player.displayClientMessage(Component.translatable("msg.watf.first_gone"), true);
                return InteractionResult.FAIL;
            }

            if (!(firstEntity instanceof LivingEntity firstLiving)) {
                player.displayClientMessage(Component.translatable("msg.watf.first_cannot_fight"), true);
                return InteractionResult.FAIL;
            }

            // 让两个生物互相攻击
            startFight(level, firstLiving, livingTarget);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 1.0f);

            String name1 = firstEntity.getName().getString();
            String name2 = targetEntity.getName().getString();
            player.displayClientMessage(Component.translatable("msg.watf.duel_start", name1, name2), true);

            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            // 触发成就：世纪大战
            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementHolder adv = serverPlayer.server.getAdvancements().get(
                        ResourceLocation.fromNamespaceAndPath("watf", "start_duel"));
                if (adv != null) {
                    serverPlayer.getAdvancements().award(adv, "manual");
                }
            }

            return InteractionResult.SUCCESS;
        }
    }

    /**
     * 让两个生物互相仇恨并攻击对方
     * 兼容所有AI类型（Goal AI + Brain AI）
     */
    public static void startFight(Level level, LivingEntity a, LivingEntity b) {
        // 设置目标（对Goal AI生物有效）
        if (a instanceof Mob mobA) {
            mobA.setTarget(b);
        }
        if (b instanceof Mob mobB) {
            mobB.setTarget(a);
        }

        // 对于使用Brain AI的生物（猪灵、猪灵蛮兵），需要通过AI系统设置愤怒目标
        if (level instanceof ServerLevel serverLevel) {
            try {
                // 普通猪灵 - 使用 PiglinAi.setAngerTarget
                if (a instanceof net.minecraft.world.entity.monster.piglin.Piglin piglinA) {
                    PiglinAiInvoker.invokeSetAngerTarget(serverLevel, piglinA, b);
                }
                if (b instanceof net.minecraft.world.entity.monster.piglin.Piglin piglinB) {
                    PiglinAiInvoker.invokeSetAngerTarget(serverLevel, piglinB, a);
                }
                // 猪灵蛮兵 - 使用 PiglinBruteAi.setAngerTarget
                if (a instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute bruteA) {
                    PiglinBruteAiInvoker.invokeSetAngerTarget(bruteA, b);
                }
                if (b instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute bruteB) {
                    PiglinBruteAiInvoker.invokeSetAngerTarget(bruteB, a);
                }
            } catch (Exception e) {
                // 如果调用失败，使用备用方案
                if (a instanceof Mob mobA && b instanceof Mob mobB) {
                    mobA.hurt(mobA.damageSources().mobAttack(mobB), 0.001f);
                    mobB.hurt(mobB.damageSources().mobAttack(mobA), 0.001f);
                    mobA.setTarget(b);
                    mobB.setTarget(a);
                }
            }
        }
    }

    private Entity getTargetEntity(Player player, Level level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(MAX_USE_DISTANCE));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(MAX_USE_DISTANCE))
                .inflate(1.0);

        Entity closestEntity = null;
        double closestDist = MAX_USE_DISTANCE;

        List<Entity> entities = level.getEntities(player, searchBox,
                e -> e.isAlive() && e.isPickable() && e instanceof LivingEntity);

        for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitPos = entityBox.clip(eyePos, endPos);

            if (entityBox.contains(eyePos)) {
                closestEntity = entity;
                closestDist = 0.0;
            } else if (hitPos.isPresent()) {
                double dist = eyePos.distanceTo(hitPos.get());
                if (dist < closestDist) {
                    closestEntity = entity;
                    closestDist = dist;
                }
            }
        }

        return closestEntity;
    }
}