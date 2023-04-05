package dev.ckateptb.minecraft.colliders;

import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import reactor.core.publisher.ParallelFlux;

import java.util.function.Consumer;

public interface Collider {
    Collider at(Vector center);

    Collider scale(double amount);

    ImmutableVector getHalfExtents();

    <RT extends Collider> boolean intersects(RT collider);

    boolean contains(Vector vector);

    Collider affectEntities(Consumer<ParallelFlux<Entity>> consumer);

    Collider affectBlocks(Consumer<ParallelFlux<Block>> consumer);

    Collider affectLocations(Consumer<ParallelFlux<Location>> consumer);

    World getWorld();

    ImmutableVector getCenter();

    default <T extends Collider> T at(Location location) {
        return (T) this.at(ImmutableVector.of(location));
    }
}
