package dev.ckateptb.minecraft.colliders.geometry;

import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CombinedBoundingBoxCollider implements Collider {
    private final World world;
    private final CombinedIntersectsMode mode;
    private final Collider[] colliders;

    public CombinedBoundingBoxCollider(World world, CombinedIntersectsMode mode, Collider... colliders) {
        this.world = world;
        this.mode = mode;
        this.colliders = colliders;
    }

    @Override
    public CombinedBoundingBoxCollider at(Vector center) {
        return new CombinedBoundingBoxCollider(world, mode, this.getColliders().map(collider ->
                collider.at(center)).toArray(Collider[]::new));
    }

    @Override
    public CombinedBoundingBoxCollider scale(double amount) {
        return new CombinedBoundingBoxCollider(world, mode, this.getColliders().map(collider ->
                collider.scale(amount)).toArray(Collider[]::new));
    }

    @Override
    public ImmutableVector getHalfExtents() {
        return this.getColliders().findFirst().map(Collider::getHalfExtents).orElse(ImmutableVector.ZERO);
    }

    @Override
    public boolean intersects(Collider other) {
        return mode == CombinedIntersectsMode.ANY ? this.intersectsAny(other) : this.intersectsAll(other);
    }

    public boolean intersectsAny(Collider other) {
        return this.getColliders().anyMatch(collider -> collider.intersects(other));
    }

    public boolean intersectsAll(Collider other) {
        return this.getColliders().allMatch(collider -> collider.intersects(other));
    }

    @Override
    public boolean contains(Vector vector) {
        return mode == CombinedIntersectsMode.ANY ? this.containsAny(vector) : this.containsAll(vector);
    }

    public boolean containsAny(Vector vector) {
        return this.getColliders().anyMatch(collider -> collider.contains(vector));
    }

    public boolean containsAll(Vector vector) {
        return this.getColliders().allMatch(collider -> collider.contains(vector));
    }

    @Override
    public CombinedBoundingBoxCollider affectEntities(Consumer<ParallelFlux<Entity>> consumer) {
        consumer.accept(applyFilter(Flux.fromArray(colliders).parallel().flatMap(collider -> {
            AtomicReference<ParallelFlux<Entity>> atomicReference = new AtomicReference<>();
            collider.affectEntities(atomicReference::set);
            return atomicReference.get();
        }), Colliders::aabb));
        return this;
    }

    @Override
    public CombinedBoundingBoxCollider affectBlocks(Consumer<ParallelFlux<Block>> consumer) {
        consumer.accept(applyFilter(Flux.fromArray(colliders).parallel().flatMap(collider -> {
            AtomicReference<ParallelFlux<Block>> atomicReference = new AtomicReference<>();
            collider.affectBlocks(atomicReference::set);
            return atomicReference.get();
        }), Colliders::aabb));
        return this;
    }

    @Override
    public CombinedBoundingBoxCollider affectLocations(Consumer<ParallelFlux<Location>> consumer) {
        consumer.accept(applyFilter(Flux.fromArray(colliders).parallel().flatMap(collider -> {
            AtomicReference<ParallelFlux<Location>> atomicReference = new AtomicReference<>();
            collider.affectLocations(atomicReference::set);
            return atomicReference.get();
        }), Colliders::aabb));
        return this;
    }

    private <T> ParallelFlux<T> applyFilter(ParallelFlux<T> flux, Function<T, Collider> getter) {
        return flux.filter(t -> {
            Collider aabb = getter.apply(t);
            return mode == CombinedIntersectsMode.ANY || intersectsAll(aabb);
        });
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public ImmutableVector getCenter() {
        return this.getColliders().findFirst().map(Collider::getCenter).orElse(ImmutableVector.ZERO);
    }

    public Stream<Collider> getColliders() {
        return Arrays.stream(colliders);
    }

    public enum CombinedIntersectsMode {
        ANY,
        ALL
    }
}
