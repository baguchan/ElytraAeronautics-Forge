package com.github.Soulphur0.mixin;

import com.github.Soulphur0.config.EanConfigFile;
import com.github.Soulphur0.utility.EanMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    // * Config file data

    protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyArg(method = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 6))
    private Vec3 modifyVelocity(Vec3 vec3) {
        // ? Re-read config file if data has been modified.

        // ? Gradual pitch realignment
        if (EanConfigFile.isSneakRealignsPitch() && this.isShiftKeyDown()) {
            float pitch = this.getXRot();

            float alignmentAngle = EanConfigFile.getRealignmentAngle();
            float alignementRate = EanConfigFile.getRealignmentRate();

            if (Math.abs(pitch) <= alignementRate * 2) {
                this.setXRot(alignmentAngle);
            } else {
                if (pitch > alignmentAngle) {
                    this.setXRot(pitch - alignementRate);
                } else {
                    this.setXRot(pitch + alignementRate);
                }
            }
        }

        // ? Get player altitude
        Vec3 positionVector = super.position();
        double playerAltitude = positionVector.y;

        // ? Calculate player speed based on altitude and return
        Vec3 movementVector;
        movementVector = calcMovementVector(playerAltitude);
        return movementVector.multiply(0.99f, 0.98f, 0.99f);
    }

    private Vec3 calcMovementVector(double playerAltitude) {
        double speedConstant = 0.08;
        double aux;
        double aux2;

        // * Calculate additional speed based on player altitude.
        double minSpeed = EanConfigFile.getMinSpeed();
        double maxSpeed = EanConfigFile.getMaxSpeed();
        double curveStart = EanConfigFile.getCurveStart();
        double curveEnd = EanConfigFile.getCurveEnd();
        double modHSpeed;

        // * Clamp the calculated modified speed to not be below or over the speed range.
        modHSpeed = (EanConfigFile.isAltitudeDeterminesSpeed()) ? Mth.clamp(EanMath.getLinealValue(curveStart, minSpeed, curveEnd, maxSpeed, playerAltitude), minSpeed, maxSpeed) : minSpeed;

        Vec3 movementVector = this.getDeltaMovement();
        if (movementVector.y > -0.5) {
            this.fallDistance = 1.0f;
        }

        Vec3 rotationVector = this.getLookAngle();
        float pitchInRadians = this.getXRot() * ((float) Math.PI / 180);
        double angleToTheGround = Math.sqrt(rotationVector.x * rotationVector.x + rotationVector.z * rotationVector.z);
        double speed = movementVector.horizontalDistance();
        double rotationVectorLength = rotationVector.length();

        float fallSpeedMultiplier = Mth.cos(pitchInRadians);
        fallSpeedMultiplier = (float) ((double) fallSpeedMultiplier * ((double) fallSpeedMultiplier * Math.min(1.0, rotationVectorLength / 0.4)));

        movementVector = this.getDeltaMovement().add(0.0, speedConstant * (-1.0 + (double) fallSpeedMultiplier * 0.75), 0.0); // ! Set Y=0.0 to turn off downwards speed.

        // * Looking under the horizon
        // Horizontal movement uses aux plus the (+1 m/s) constant multiplied by the speed set by the player minus default speed.
        if (movementVector.y < 0.0 && angleToTheGround > 0.0) {
            aux = movementVector.y * -0.1 * (double)fallSpeedMultiplier;

            // ! The value 30.1298 should only be subtracted when downwards speed is active, since this speed affects the horizontal speed that it is already tweaked to almost perfectly reflect config file values.
            aux2 = aux + (modHSpeed-30.1298D)*0.0005584565076792029D; // ? This is the 1 m/s constant, it is not 100% accurate, but it is close enough.

            movementVector = movementVector.add(rotationVector.x * aux2 / angleToTheGround, aux, rotationVector.z * aux2 / angleToTheGround); // ! Set Y=0.0 to turn off downwards speed.
        }

        // * Looking over the horizon
        // Vertical speed decreases with the player realtime speed.
        if (pitchInRadians < 0.0f && angleToTheGround > 0.0) {
            aux = speed * (double) (-Mth.sin(pitchInRadians)) * 0.04;

            movementVector = movementVector.add(-rotationVector.x * aux / angleToTheGround, Math.min((aux * 3.2), 0.1D), -rotationVector.z * aux / angleToTheGround);
        }

        if (angleToTheGround > 0.0) {
            movementVector = movementVector.add((rotationVector.x / angleToTheGround * speed - movementVector.x) * 0.1, 0.0, (rotationVector.z / angleToTheGround * speed - movementVector.z) * 0.1);
        }

        return movementVector;
    }
}
