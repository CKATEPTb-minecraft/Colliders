package dev.ckateptb.minecraft.colliders;

import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Collider {
    Collider at(Vector center);

    Collider scale(double amount);

    ImmutableVector getHalfExtents();

    <RT extends Collider> boolean intersects(RT collider);

    boolean contains(Vector vector);

    Collider affectEntities(Consumer<Stream<Entity>> consumer);

    Collider affectBlocks(Consumer<Stream<Block>> consumer);

    Collider affectPositions(Consumer<Stream<Location>> consumer);

    World getWorld();

    ImmutableVector getCenter();
}
