/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */
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
import dev.ckateptb.minecraft.colliders.math.LerpUtil;
import lombok.Getter;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public class OrientedBoundingBoxCollider implements Collider {
    protected final World world;
    protected final ImmutableVector center;
    protected final EulerAngle rotation;
    protected final ImmutableVector right;
    protected final ImmutableVector up;
    protected final ImmutableVector forward;
    protected final ImmutableVector halfExtents;

    private OrientedBoundingBoxCollider(OrientedBoundingBoxCollider obb, ImmutableVector center) {
        this(obb, center, obb.halfExtents);
    }

    private OrientedBoundingBoxCollider(OrientedBoundingBoxCollider obb, ImmutableVector center, ImmutableVector halfExtents) {
        this.world = obb.world;
        this.center = center;
        this.rotation = obb.rotation;
        this.right = obb.right;
        this.up = obb.up;
        this.forward = obb.forward;
        this.halfExtents = halfExtents;
    }

    public OrientedBoundingBoxCollider(AxisAlignedBoundingBoxCollider aabb, EulerAngle eulerAngle) {
        this(aabb.world, aabb.getCenter(), aabb.max, eulerAngle);
    }

    public OrientedBoundingBoxCollider(World world, ImmutableVector center, ImmutableVector halfExtents, EulerAngle eulerAngle) {
        this.world = world;
        this.center = center;
        this.rotation = eulerAngle.setZ(0); // Roll is not implement now
        this.right = ImmutableVector.PLUS_I.rotate(this.rotation);
        this.up = ImmutableVector.PLUS_J.rotate(this.rotation);
        this.forward = ImmutableVector.PLUS_K.rotate(this.rotation);
        this.halfExtents = halfExtents;
    }

    public ImmutableVector getClosestPosition(ImmutableVector target) {
        ImmutableVector destination = target.subtract(center);
        ImmutableVector closest = center;
        for (int i = 0; i < 3; i++) {
            ImmutableVector axis = switch (i) {
                case 0 -> right;
                case 1 -> up;
                case 2 -> forward;
                default -> throw new IllegalStateException("Unexpected value: " + i);
            };
            double halfComponent = halfExtents.getComponent(i);
            double dist = LerpUtil.clamp(destination.dot(axis), -halfComponent, halfComponent);
            closest = closest.add(axis.multiply(dist));
        }
        return closest;
    }

    @Override
    public boolean intersects(Collider other) {
        World otherWorld = other.getWorld();
        if (!otherWorld.equals(world)) return false;
        if (other instanceof OrientedBoundingBoxCollider obb) {
            ImmutableVector centerDifference = obb.center.subtract(this.center);
            for (int i = 0; i < 15; i++) {
                ImmutableVector current = this.getByIndex(i, obb);
                if (projectionOnAxis(centerDifference, current) >
                        projectionOnAxis(this.right.multiply(this.halfExtents.getX()), current) +
                                projectionOnAxis(this.up.multiply(this.halfExtents.getY()), current) +
                                projectionOnAxis(this.forward.multiply(this.halfExtents.getZ()), current) +
                                projectionOnAxis(obb.right.multiply(obb.halfExtents.getX()), current) +
                                projectionOnAxis(obb.up.multiply(obb.halfExtents.getY()), current) +
                                projectionOnAxis(obb.forward.multiply(obb.halfExtents.getZ()), current)) {
                    return false;
                }
            }
            return true;
        }
        if (other instanceof AxisAlignedBoundingBoxCollider aabb) {
            return this.intersects(new OrientedBoundingBoxCollider(aabb, EulerAngle.ZERO));
        }
        if (other instanceof SphereBoundingBoxCollider sphere) {
            ImmutableVector distance = sphere.center.subtract(getClosestPosition(sphere.center));
            return distance.dot(distance) <= sphere.radius * sphere.radius;
        }
        return false;
    }

    private double projectionOnAxis(Vector vector, Vector vector2) {
        return FastMath.abs(vector.dot(vector2));
    }

    private ImmutableVector getByIndex(int n, OrientedBoundingBoxCollider other) {
        return switch (n) {
            case 0 -> this.right;
            case 1 -> this.up;
            case 2 -> this.forward;
            case 3 -> other.right;
            case 4 -> other.up;
            case 5 -> other.forward;
            case 6 -> this.right.getCrossProduct(other.right);
            case 7 -> this.right.getCrossProduct(other.up);
            case 8 -> this.right.getCrossProduct(other.forward);
            case 9 -> this.up.getCrossProduct(other.right);
            case 10 -> this.up.getCrossProduct(other.up);
            case 11 -> this.up.getCrossProduct(other.forward);
            case 12 -> this.forward.getCrossProduct(other.right);
            case 13 -> this.forward.getCrossProduct(other.up);
            case 14 -> this.forward.getCrossProduct(other.forward);
            default -> throw new IllegalStateException("Unexpected value: " + n);
        };
    }

    @Override
    public OrientedBoundingBoxCollider at(Vector center) {
        return new OrientedBoundingBoxCollider(this, ImmutableVector.of(center));
    }

    @Override
    public Collider scale(double amount) {
        return new OrientedBoundingBoxCollider(this, center, halfExtents.multiply(amount));
    }

    @Override
    public boolean contains(Vector vector) {
        ImmutableVector point = ImmutableVector.of(vector);
        return getClosestPosition(point).distanceSquared(point) <= 0.01;
    }

    @Override
    public Collider affectEntities(Consumer<Stream<CurrentThreadAtomChain<Entity>>> consumer) {
        double max = halfExtents.maxComponent();
        AsyncService asyncService = Colliders.getAsyncService();
        consumer.accept(
                asyncService.getNearbyEntities(
                                this.getCenter().toLocation(world),
                                max,
                                max,
                                max
                        )
                        .stream()
                        .parallel()
                        .filter(entity -> {
                            AxisAlignedBoundingBoxCollider aabb = Colliders.aabb(entity);
                            return this.intersects(aabb) && this.contains(this.getClosestPosition(aabb.getCenter()));
                        })
                        .map(AtomChain::of)
        );
        return this;
    }

    @Override
    public Collider affectBlocks(Consumer<Stream<CurrentThreadAtomChain<ThreadSafeBlock>>> consumer) {
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        double maxComponent = this.halfExtents.maxComponent();
        ImmutableVector halfExtents = new ImmutableVector(maxComponent, maxComponent, maxComponent);
        try (EditSession editSession = ThreadSafeBlock.defaultEditSession(bukkitWorld)) {
            consumer.accept(
                    new CuboidRegion(
                            bukkitWorld,
                            halfExtents.negative().add(center).toWorldEditBlockVector(),
                            halfExtents.add(center).toWorldEditBlockVector()
                    )
                            .stream()
                            .filter(position -> this.intersects(
                                            Colliders.aabb(
                                                    world,
                                                    ImmutableVector.ZERO,
                                                    ImmutableVector.ONE
                                            ).at(ImmutableVector.of(position))
                                    ) && this.contains(ImmutableVector.of(position))
                            ).map(position -> AtomChain.of(
                                            new ThreadSafeBlock(
                                                    bukkitWorld,
                                                    position
                                            )
                                                    .setEditSession(editSession)
                                    )
                            )
            );
        }
        return this;
    }

    @Override
    public Collider affectPositions(Consumer<Stream<CurrentThreadAtomChain<Location>>> consumer) {
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        double maxComponent = this.halfExtents.maxComponent();
        ImmutableVector halfExtents = new ImmutableVector(maxComponent, maxComponent, maxComponent);
        consumer.accept(
                new CuboidRegion(
                        bukkitWorld,
                        halfExtents.negative().add(center).toWorldEditBlockVector(),
                        halfExtents.add(center).toWorldEditBlockVector()
                )
                        .stream()
                        .filter(position -> this.intersects(
                                        Colliders.aabb(
                                                world,
                                                ImmutableVector.ZERO,
                                                ImmutableVector.ONE
                                        ).at(ImmutableVector.of(position))
                                ) && this.contains(ImmutableVector.of(position))
                        ).map(position -> AtomChain.of(
                                        BukkitAdapter.adapt(
                                                world,
                                                position
                                        )
                                )
                        )
        );
        return this;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public ImmutableVector getCenter() {
        return center;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrientedBoundingBoxCollider that)) return false;
        return Objects.equal(world, that.world) && Objects.equal(center, that.center) && Objects.equal(rotation, that.rotation) && Objects.equal(halfExtents, that.halfExtents);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(world, center, rotation, halfExtents);
    }

    @Override
    public String toString() {
        return "OrientedBoundingBoxCollider{" +
                "world=" + world.getName() +
                ", center=" + center +
                ", rotation=" + rotation +
                ", halfExtents=" + halfExtents +
                '}';
    }
}
