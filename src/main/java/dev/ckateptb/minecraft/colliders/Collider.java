package dev.ckateptb.minecraft.colliders;

import dev.ckateptb.minecraft.atom.async.block.ThreadSafeBlock;
import dev.ckateptb.minecraft.atom.chain.CurrentThreadAtomChain;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.World;
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

    Collider affectEntities(Consumer<Stream<CurrentThreadAtomChain<Entity>>> consumer);

    Collider affectBlocks(Consumer<Stream<CurrentThreadAtomChain<ThreadSafeBlock>>> consumer);

    Collider affectPositions(Consumer<Stream<CurrentThreadAtomChain<Location>>> consumer);

    World getWorld();

    ImmutableVector getCenter();
}
