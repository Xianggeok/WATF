package top.eley.watf.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 调用 PiglinBruteAi 的 protected 方法
 */
@Mixin(PiglinBruteAi.class)
public interface PiglinBruteAiInvoker {
    @Invoker("wasHurtBy")
    static void invokeWasHurtBy(ServerLevel level, PiglinBrute brute, LivingEntity attacker) {
        throw new AssertionError();
    }
    
    @Invoker("setAngerTarget")
    static void invokeSetAngerTarget(PiglinBrute brute, LivingEntity target) {
        throw new AssertionError();
    }
}