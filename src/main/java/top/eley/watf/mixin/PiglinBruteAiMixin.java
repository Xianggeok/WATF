package top.eley.watf.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修改猪灵蛮兵的 wasHurtBy 方法，移除同种族限制
 */
@Mixin(PiglinBruteAi.class)
public class PiglinBruteAiMixin {
    
    @Inject(method = "wasHurtBy", at = @At("HEAD"), cancellable = true)
    private static void onWasHurtBy(ServerLevel level, PiglinBrute brute, LivingEntity attacker, CallbackInfo ci) {
        // 移除原版的同种族限制，允许猪灵蛮兵仇恨同种族
        // 原版代码：if (attacker instanceof AbstractPiglin) return;
        // 我们跳过这个检查，让它继续执行设置愤怒目标的逻辑
    }
}