package top.eley.watf.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 调用 PiglinAi 的 protected 方法
 */
@Mixin(PiglinAi.class)
public interface PiglinAiInvoker {
    @Invoker("setAngerTarget")
    static void invokeSetAngerTarget(ServerLevel level, AbstractPiglin piglin, LivingEntity target) {
        throw new AssertionError();
    }
}
