package dev.ckateptb.minecraft.colliders.geometry;

import com.google.common.base.Objects;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import dev.ckateptb.minecraft.atom.async.AsyncService;
import dev.ckateptb.minecraft.atom.async.block.ThreadSafeBlock;
import dev.ckateptb.minecraft.atom.chain.AtomChain;
import dev.ckateptb.minecraft.atom.chain.CurrentThreadAtomChain;
import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public class AxisAlignedBoundingBoxCollider implements Collider {

    protected final World world;
    protected final ImmutableVector min;
    protected final ImmutableVector max;

    public AxisAlignedBoundingBoxCollider(World world, ImmutableVector min, ImmutableVector max) {
        this.world = world;
        this.min = min.min(max);
        this.max = max.max(min);
    }

    @Override
    public AxisAlignedBoundingBoxCollider at(Vector center) {
        ImmutableVector halfExtents = this.getHalfExtents();
        ImmutableVector immutableCenter = ImmutableVector.of(center);
        return new AxisAlignedBoundingBoxCollider(world, immutableCenter.add(halfExtents.negative()), immutableCenter.add(halfExtents));
    }

    @Override
    public AxisAlignedBoundingBoxCollider scale(double multiplier) {
        return this.scale(multiplier, multiplier, multiplier);
    }

    public AxisAlignedBoundingBoxCollider scale(double multiplierX, double multiplierY, double multiplierZ) {
        ImmutableVector halfExtents = this.getHalfExtents();
        ImmutableVector newExtents = new ImmutableVector(halfExtents.getX() * multiplierX,
                halfExtents.getY() * multiplierY,
                halfExtents.getZ() * multiplierZ);
        ImmutableVector diff = newExtents.subtract(halfExtents);
        return new AxisAlignedBoundingBoxCollider(world, min.subtract(diff), max.add(diff));
    }

    @Override
    public ImmutableVector getHalfExtents() {
        return max.subtract(min).multiply(0.5).abs();
    }

    @Override
    public ImmutableVector getCenter() {
        return min.add(max.subtract(min).multiply(0.5));
    }

    @Override
    public boolean contains(Vector vector) {
        return vector.isInAABB(min, max);
    }

    @Override
    public boolean intersects(Collider other) {
        World otherWorld = other.getWorld();
        if (!otherWorld.equals(world)) return false;
        if (other instanceof AxisAlignedBoundingBoxCollider aabb) {
            return this.min.getX() < aabb.max.getX()
                    && this.max.getX() > aabb.min.getX()
                    && this.min.getY() < aabb.max.getY()
                    && this.max.getY() > aabb.min.getY()
                    && this.min.getZ() < aabb.max.getZ()
                    && this.max.getZ() > aabb.min.getZ();
        }
        if (other instanceof SphereBoundingBoxCollider sphere) {
            return sphere.intersects(this);
        }
        if (other instanceof OrientedBoundingBoxCollider obb) {
            return obb.intersects(this);
        }
        return false;
    }

    @Override
    public AxisAlignedBoundingBoxCollider affectEntities(Consumer<Stream<CurrentThreadAtomChain<Entity>>> consumer) {
        AsyncService asyncService = Colliders.getAsyncService();
        ImmutableVector vector = min.max(max);
        consumer.accept(
                asyncService.getNearbyEntities(
                                this.getCenter().toLocation(world),
                                vector.getX(),
                                vector.getY(),
                                vector.getZ()
                        )
                        .stream()
                        .parallel()
                        .filter(entity -> this.intersects(
                                        Colliders.aabb(
                                                entity
                                        )
                                )
                        )
                        .map(AtomChain::of));
        return this;
    }

    @Override
    public AxisAlignedBoundingBoxCollider affectBlocks(Consumer<Stream<CurrentThreadAtomChain<ThreadSafeBlock>>> consumer) {
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        try (EditSession editSession = ThreadSafeBlock.defaultEditSession(bukkitWorld)) {
            consumer.accept(
                    new CuboidRegion(
                            bukkitWorld,
                            min.toWorldEditBlockVector(),
                            max.toWorldEditBlockVector()
                    )
                            .stream()
                            .map(blockVector -> AtomChain.of(
                                            new ThreadSafeBlock(
                                                    bukkitWorld,
                                                    blockVector
                                            )
                                                    .setEditSession(editSession)
                                    )
                            )
            );
        }
        return this;
    }

    @Override
    public AxisAlignedBoundingBoxCollider affectPositions(Consumer<Stream<CurrentThreadAtomChain<Location>>> consumer) {
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        consumer.accept(
                new CuboidRegion(
                        bukkitWorld,
                        min.toWorldEditBlockVector(),
                        max.toWorldEditBlockVector()
                )
                        .stream()
                        .map(blockVector -> AtomChain.of(
                                        BukkitAdapter.adapt(
                                                world,
                                                blockVector
                                        )
                                )
                        )
        );
        return this;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AxisAlignedBoundingBoxCollider that)) return false;
        return Objects.equal(world, that.world) && Objects.equal(min, that.min) && Objects.equal(max, that.max);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(world, min, max);
    }

    @Override
    public String toString() {
        return "AxisAlignedBoundingBoxCollider{" +
                "world=" + world.getName() +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
