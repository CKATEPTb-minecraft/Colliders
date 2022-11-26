package dev.ckateptb.minecraft.colliders.geometry;

import com.google.common.base.Objects;
import dev.ckateptb.minecraft.atom.async.AsyncService;
import dev.ckateptb.minecraft.atom.async.block.ThreadSafeBlock;
import dev.ckateptb.minecraft.atom.chain.AtomChain;
import dev.ckateptb.minecraft.atom.chain.CurrentThreadAtomChain;
import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import lombok.Getter;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public class SphereBoundingBoxCollider implements Collider {
    protected final World world;
    protected final ImmutableVector center;
    protected final double radius;

    public SphereBoundingBoxCollider(World world, Vector center, double radius) {
        this.world = world;
        this.center = ImmutableVector.of(center);
        this.radius = radius;
    }

    @Override
    public SphereBoundingBoxCollider at(Vector center) {
        return new SphereBoundingBoxCollider(world, center, radius);
    }

    @Override
    public SphereBoundingBoxCollider scale(double amount) {
        return new SphereBoundingBoxCollider(world, center, radius * amount);
    }

    @Override
    public ImmutableVector getHalfExtents() {
        return new ImmutableVector(radius, radius, radius);
    }

    @Override
    public boolean intersects(Collider other) {
        World otherWorld = other.getWorld();
        if (!otherWorld.equals(world)) return false;
        if (other instanceof SphereBoundingBoxCollider sphere) {
            return sphere.center.isInSphere(center, radius + sphere.radius);
        }
        if (other instanceof AxisAlignedBoundingBoxCollider aabb) {
            ImmutableVector min = aabb.min;
            ImmutableVector max = aabb.max;
            double x = FastMath.max(min.getX(), FastMath.min(center.getX(), max.getX()));
            double y = FastMath.max(min.getY(), FastMath.min(center.getY(), max.getY()));
            double z = FastMath.max(min.getZ(), FastMath.min(center.getZ(), max.getZ()));
            return contains(new ImmutableVector(x, y, z));
        }
        if (other instanceof OrientedBoundingBoxCollider obb) {
            return obb.intersects(this);
        }
        return false;
    }

    @Override
    public boolean contains(Vector vector) {
        return vector.isInSphere(center, radius);
    }

    @Override
    public SphereBoundingBoxCollider affectEntities(Consumer<Stream<CurrentThreadAtomChain<Entity>>> consumer) {
        AsyncService asyncService = Colliders.getAsyncService();
        consumer.accept(asyncService.getNearbyEntities(this.getCenter().toLocation(world), radius, radius, radius)
                .stream().parallel().filter(entity -> this.intersects(Colliders.aabb(entity))).map(AtomChain::of));
        return this;
    }

    @Override
    public SphereBoundingBoxCollider affectBlocks(Consumer<Stream<CurrentThreadAtomChain<ThreadSafeBlock>>> consumer) {
        ImmutableVector halfExtents = getHalfExtents();
        new AxisAlignedBoundingBoxCollider(world, halfExtents.negative(), halfExtents)
                .at(center)
                .affectBlocks(currentThreadAtomChainStream ->
                        consumer.accept(currentThreadAtomChainStream.filter(chain ->
                                        this.intersects(new AxisAlignedBoundingBoxCollider(world,
                                                ImmutableVector.ZERO,
                                                ImmutableVector.ONE)
                                                .at(chain.get().getLocation().toCenterLocation().toVector())
                                        )
                                )
                        )
                );
        return this;
    }

    @Override
    public SphereBoundingBoxCollider affectPositions(Consumer<Stream<CurrentThreadAtomChain<Location>>> consumer) {
        ImmutableVector halfExtents = getHalfExtents();
        new AxisAlignedBoundingBoxCollider(world, halfExtents.negative(), halfExtents)
                .at(center)
                .affectPositions(currentThreadAtomChainStream ->
                        consumer.accept(currentThreadAtomChainStream.filter(chain -> {
                                    Location location = chain.get().toCenterLocation();
                                    return this.intersects(new AxisAlignedBoundingBoxCollider(world,
                                            ImmutableVector.ZERO,
                                            ImmutableVector.ONE).at(location.toVector()));
                                })
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
        if (!(o instanceof SphereBoundingBoxCollider that)) return false;
        return Double.compare(that.radius, radius) == 0 && Objects.equal(world, that.world) && Objects.equal(center, that.center);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(world, radius, center);
    }

    @Override
    public String toString() {
        return "SphereCollider{" + "world=" + world.getName() + ", radius=" + radius + ", center=" + center + '}';
    }
}
