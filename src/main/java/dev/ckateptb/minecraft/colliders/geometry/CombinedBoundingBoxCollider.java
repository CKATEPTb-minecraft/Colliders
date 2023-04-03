package dev.ckateptb.minecraft.colliders.geometry;

import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CombinedBoundingBoxCollider implements Collider {
    private final World world;
    private final CombinedIntersectsMode mode;
    private final Stream<Collider> colliders;

    public CombinedBoundingBoxCollider(World world, CombinedIntersectsMode mode, Collider... colliders) {
        this.world = world;
        this.mode = mode;
        this.colliders = Arrays.stream(colliders);
    }

    @Override
    public CombinedBoundingBoxCollider at(Vector center) {
        return new CombinedBoundingBoxCollider(world, mode, this.colliders.map(collider -> collider.at(center)).toArray(Collider[]::new));
    }

    @Override
    public CombinedBoundingBoxCollider scale(double amount) {
        return new CombinedBoundingBoxCollider(world, mode, this.colliders.map(collider -> collider.scale(amount)).toArray(Collider[]::new));
    }

    @Override
    public ImmutableVector getHalfExtents() {
        return colliders.findFirst().map(Collider::getHalfExtents).orElse(ImmutableVector.ZERO);
    }

    @Override
    public boolean intersects(Collider other) {
        return mode == CombinedIntersectsMode.ANY ? this.intersectsAny(other) : this.intersectsAll(other);
    }

    public boolean intersectsAny(Collider other) {
        return colliders.anyMatch(collider -> collider.intersects(other));
    }

    public boolean intersectsAll(Collider other) {
        return colliders.allMatch(collider -> collider.intersects(other));
    }

    @Override
    public boolean contains(Vector vector) {
        return mode == CombinedIntersectsMode.ANY ? this.containsAny(vector) : this.containsAll(vector);
    }

    public boolean containsAny(Vector vector) {
        return colliders.anyMatch(collider -> collider.contains(vector));
    }

    public boolean containsAll(Vector vector) {
        return colliders.allMatch(collider -> collider.contains(vector));
    }

    @Override
    public CombinedBoundingBoxCollider affectEntities(Consumer<Stream<Entity>> consumer) {
        consumer.accept(colliders.flatMap(collider -> {
            Set<Entity> entities = ConcurrentHashMap.newKeySet();
            collider.affectEntities(stream ->
                    stream.parallel().forEach(entities::add));
            return Stream.of(entities.toArray(Entity[]::new));
        }).parallel().filter(entity -> {
            if (mode == CombinedIntersectsMode.ANY) return true;
            return intersectsAll(Colliders.aabb(entity));
        }));
        return this;
    }

    @Override
    public CombinedBoundingBoxCollider affectBlocks(Consumer<Stream<Block>> consumer) {
        consumer.accept(colliders.flatMap(collider -> {
            Set<Block> blocks = ConcurrentHashMap.newKeySet();
            collider.affectBlocks(stream ->
                    stream.parallel().forEach(blocks::add));
            return Stream.of(blocks.toArray(Block[]::new));
        }).parallel().filter(block -> {
            if (mode == CombinedIntersectsMode.ANY) return true;
            return intersectsAll(Colliders.aabb(world, ImmutableVector.ZERO, ImmutableVector.ONE)
                    .at(block.getLocation().toVector()));
        }));
        return this;
    }

    @Override
    public CombinedBoundingBoxCollider affectPositions(Consumer<Stream<Location>> consumer) {
        consumer.accept(colliders.flatMap(collider -> {
            Set<Location> locations = ConcurrentHashMap.newKeySet();
            collider.affectPositions(stream -> stream.parallel().forEach(locations::add));
            return Stream.of(locations.toArray(Location[]::new));
        }).parallel().filter(location -> {
            if (mode == CombinedIntersectsMode.ANY) return true;
            return intersectsAll(Colliders.aabb(world, ImmutableVector.ZERO, ImmutableVector.ONE)
                    .at(location.toVector()));
        }));
        return this;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public ImmutableVector getCenter() {
        return colliders.findFirst().map(Collider::getCenter).orElse(ImmutableVector.ZERO);
    }

    public enum CombinedIntersectsMode {
        ANY,
        ALL
    }
}
