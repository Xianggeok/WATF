package top.eley.watf.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 团战法杖 - 选中两个物种，让它们群体互殴
 */
public class TeamBattleItem extends Item {

    private static final Map<UUID, EntityType<?>> FIRST_TYPE = new HashMap<>();
    private static final double SEARCH_RADIUS = 32.0;
    private static final double MAX_USE_DISTANCE = 15.0;

    public TeamBattleItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(32)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.watf.team_battle.desc"));
        tooltip.add(Component.translatable("item.watf.team_battle.desc2"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }

        Entity targetEntity = getTargetEntity(player, level);

        if (targetEntity == null) {
            player.sendOverlayMessage(Component.translatable("msg.watf.no_target_team"));
            return InteractionResultHolder.fail(stack);
        }

        if (!(targetEntity instanceof LivingEntity)) {
            player.sendOverlayMessage(Component.translatable("msg.watf.cannot_fight"));
            return InteractionResultHolder.fail(stack);
        }

        if (targetEntity instanceof AgeableMob) {
            player.sendOverlayMessage(Component.translatable("msg.watf.species_passive"));
            return InteractionResultHolder.fail(stack);
        }

        if (targetEntity.equals(player)) {
            player.sendOverlayMessage(Component.translatable("msg.watf.no_self_team"));
            return InteractionResultHolder.fail(stack);
        }

        EntityType<?> targetType = targetEntity.getType();
        String speciesName = targetType.getDescription().getString();
        UUID playerId = player.getUUID();

        if (!FIRST_TYPE.containsKey(playerId)) {
            FIRST_TYPE.put(playerId, targetType);
            player.sendOverlayMessage(Component.translatable("msg.watf.first_species", speciesName));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResultHolder.success(stack);
        } else {
            EntityType<?> firstType = FIRST_TYPE.remove(playerId);

            if (firstType.equals(targetType)) {
                player.sendOverlayMessage(Component.translatable("msg.watf.different_species"));
                return InteractionResultHolder.fail(stack);
            }

            // 搜索范围内所有属于这两个物种的生物
            Vec3 center = player.position();
            AABB searchArea = new AABB(
                    center.x - SEARCH_RADIUS, center.y - SEARCH_RADIUS, center.z - SEARCH_RADIUS,
                    center.x + SEARCH_RADIUS, center.y + SEARCH_RADIUS, center.z + SEARCH_RADIUS
            );

            List<LivingEntity> teamA = new ArrayList<>();
            List<LivingEntity> teamB = new ArrayList<>();
            String speciesAName = firstType.getDescription().getString();
            String speciesBName = targetType.getDescription().getString();

            List<Entity> allEntities = level.getEntities(player, searchArea,
                    e -> e.isAlive() && e instanceof LivingEntity && !e.equals(player));

            for (Entity entity : allEntities) {
                if (entity.getType().equals(firstType)) {
                    teamA.add((LivingEntity) entity);
                } else if (entity.getType().equals(targetType)) {
                    teamB.add((LivingEntity) entity);
                }
            }

            if (teamA.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("msg.watf.species_not_found", speciesAName));
                return InteractionResultHolder.fail(stack);
            }

            if (teamB.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("msg.watf.species_not_found", speciesBName));
                return InteractionResultHolder.fail(stack);
            }

            // 团战：A队每个成员攻击B队，B队每个成员攻击A队
            int fightCount = 0;
            for (LivingEntity memberA : teamA) {
                LivingEntity target = teamB.get(fightCount % teamB.size());
                DuelStaffItem.startFight(level, memberA, target);
                fightCount++;
            }
            for (LivingEntity memberB : teamB) {
                LivingEntity target = teamA.get(fightCount % teamA.size());
                DuelStaffItem.startFight(level, memberB, target);
                fightCount++;
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 1.0f);

            player.sendOverlayMessage(Component.translatable("msg.watf.team_start", speciesAName, teamA.size(), speciesBName, teamB.size()));

            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            // 触发成就：物种战争
            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementHolder adv = serverPlayer.server.getAdvancements().get(
                        Identifier.fromNamespaceAndPath("watf", "start_team_battle"));
                if (adv != null) {
                    serverPlayer.getAdvancements().award(adv, "manual");
                }
            }

            return InteractionResultHolder.success(stack);
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