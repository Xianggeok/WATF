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
import top.eley.watf.Watf;
import top.eley.watf.mixin.PiglinBruteAiInvoker;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
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
                .setId(Watf.DUEL_STAFF_KEY)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.translatable("item.watf.duel_staff.desc"));
        consumer.accept(Component.translatable("item.watf.duel_staff.desc2"));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        Entity targetEntity = getTargetEntity(player, level);

        if (targetEntity == null) {
            player.sendSystemMessage(Component.translatable("msg.watf.no_target"));
            return InteractionResult.FAIL;
        }

        if (!(targetEntity instanceof LivingEntity livingTarget)) {
            player.sendSystemMessage(Component.translatable("msg.watf.cannot_fight"));
            return InteractionResult.FAIL;
        }

        if (targetEntity instanceof AgeableMob) {
            player.sendSystemMessage(Component.translatable("msg.watf.too_passive"));
            return InteractionResult.FAIL;
        }

        if (targetEntity.equals(player)) {
            player.sendSystemMessage(Component.translatable("msg.watf.no_self"));
            return InteractionResult.FAIL;
        }

        UUID playerId = player.getUUID();

        if (!FIRST_TARGET.containsKey(playerId)) {
            FIRST_TARGET.put(playerId, targetEntity);
            String name = targetEntity.getName().getString();
            player.sendSystemMessage(Component.translatable("msg.watf.first_selected", name));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResult.SUCCESS;
        } else {
            Entity firstEntity = FIRST_TARGET.remove(playerId);

            if (firstEntity.equals(targetEntity)) {
                player.sendSystemMessage(Component.translatable("msg.watf.different_opponent"));
                return InteractionResult.FAIL;
            }

            if (!firstEntity.isAlive() || firstEntity.isRemoved()) {
                player.sendSystemMessage(Component.translatable("msg.watf.first_gone"));
                return InteractionResult.FAIL;
            }

            if (!(firstEntity instanceof LivingEntity firstLiving)) {
                player.sendSystemMessage(Component.translatable("msg.watf.first_cannot_fight"));
                return InteractionResult.FAIL;
            }

            startFight(firstLiving, livingTarget);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 1.0f);

            String name1 = firstEntity.getName().getString();
            String name2 = targetEntity.getName().getString();
            player.sendSystemMessage(Component.translatable("msg.watf.duel_start", name1, name2));

            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementHolder adv = serverPlayer.level().getServer().getAdvancements().get(
                        Identifier.fromNamespaceAndPath("watf", "start_duel"));
                if (adv != null) {
                    serverPlayer.getAdvancements().award(adv, "manual");
                }
            }

            return InteractionResult.SUCCESS;
        }
    }

    public static void startFight(LivingEntity a, LivingEntity b) {
        if (a instanceof Mob mobA) {
            mobA.setTarget(b);
        }
        if (b instanceof Mob mobB) {
            mobB.setTarget(a);
        }

        try {
            if (a instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute bruteA) {
                PiglinBruteAiInvoker.invokeSetAngerTarget(bruteA, b);
            }
            if (b instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute bruteB) {
                PiglinBruteAiInvoker.invokeSetAngerTarget(bruteB, a);
            }
        } catch (Exception e) {
            if (a instanceof Mob mobA && b instanceof Mob mobB) {
                mobA.hurt(mobA.damageSources().mobAttack(mobB), 0.001f);
                mobB.hurt(mobB.damageSources().mobAttack(mobA), 0.001f);
                mobA.setTarget(b);
                mobB.setTarget(a);
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
