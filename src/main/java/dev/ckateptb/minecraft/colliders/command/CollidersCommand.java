package dev.ckateptb.minecraft.colliders.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import dev.ckateptb.common.tableclothcontainer.IoC;
import dev.ckateptb.common.tableclothcontainer.annotation.Component;
import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.geometry.OrientedBoundingBoxCollider;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import dev.ckateptb.minecraft.supervisor.Command;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Component
public class CollidersCommand implements Command<Colliders> {
    private final Colliders plugin;

    public CollidersCommand() {
        this.plugin = IoC.getBean(Colliders.class);
    }

    @CommandMethod("colliders debug direct aabb <x> <y> <z> [duration]")
    @CommandPermission("colliders.admin")
    public void aabbDirect(Player player, @Argument("x") Double x, @Argument("y") Double y, @Argument("z") Double z, @Argument("duration") Long duration) {
        ImmutableVector immutableVector = new ImmutableVector(x, y, z);
        this.renderDirect(Colliders.aabb(player.getWorld(), immutableVector.negative(), immutableVector), player, immutableVector.maxComponent() + 3, duration);
    }

    @CommandMethod("colliders debug direct sphere <radius> [duration]")
    @CommandPermission("colliders.admin")
    public void sphereDirect(Player player, @Argument("radius") Double radius, @Argument("duration") Long duration) {
        this.renderDirect(Colliders.sphere(player.getWorld(), ImmutableVector.ZERO, radius), player, radius + 3, duration);
    }

    @CommandMethod("colliders debug direct obb <x> <y> <z> [duration]")
    @CommandPermission("colliders.admin")
    public void obbDirect(Player player, @Argument("x") Double x, @Argument("y") Double y, @Argument("z") Double z, @Argument("duration") Long duration) {
        ImmutableVector immutableVector = new ImmutableVector(x, y, z);
        Location location = player.getLocation();
        float pitch = location.getPitch();
        float yaw = location.getYaw();
        float roll = 0;
        EulerAngle eulerAngle = new ImmutableVector(pitch, yaw, roll).radians().toEulerAngle();
        this.renderDirect(Colliders.obb(player.getWorld(), ImmutableVector.ZERO, immutableVector, eulerAngle), player, immutableVector.maxComponent() + 3, duration);
    }

    private void renderDirect(Collider collider, Player player, Double distance, Long duration) {
        if (duration == null) {
            collider.at(getCenter(distance, player)).affectBlocks(stream -> stream.forEach(chain -> chain.run(block -> block.setType(Material.SAND))));
        } else {
            AtomicReference<Collider> colliderReference = new AtomicReference<>(collider);
            Disposable disposable = Schedulers.boundedElastic().schedulePeriodically(() -> {
                if(collider instanceof OrientedBoundingBoxCollider) {
                    Location location = player.getLocation();
                    float pitch = location.getPitch();
                    float yaw = location.getYaw();
                    float roll = 0;
                    EulerAngle eulerAngle = new ImmutableVector(pitch, yaw, roll).radians().toEulerAngle();
                    colliderReference.set(Colliders.obb(collider.getWorld(), collider.getCenter(), collider.getHalfExtents(), eulerAngle));
                }
                colliderReference.get().at(getCenter(distance, player)).affectPositions(stream -> stream.forEach(chain -> chain.run(location -> Particle.REDSTONE.builder().force(true).location(location).count(1).color(Color.RED, 0.5f).spawn())));
            }, 0, 500, TimeUnit.MILLISECONDS);
            Schedulers.single().schedule(disposable::dispose, duration, TimeUnit.MILLISECONDS);
        }
    }

    private ImmutableVector getCenter(double distance, Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().multiply(distance);
        return ImmutableVector.of(eyeLocation.add(direction));
    }
}
