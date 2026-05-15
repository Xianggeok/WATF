package top.eley.watf.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修改 LivingEntity 的 canAttack 方法，允许同种族生物互打
 */
@Mixin(LivingEntity.class)
public class MobMixin {
    
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void onCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        // 如果目标是同类型生物，允许攻击（覆盖原版的同种族限制）
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getType() == target.getType()) {
            cir.setReturnValue(true);
        }
    }
}