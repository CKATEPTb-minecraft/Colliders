package dev.ckateptb.minecraft.colliders.geometry;

import dev.ckateptb.minecraft.colliders.Collider;
import dev.ckateptb.minecraft.colliders.Colliders;
import dev.ckateptb.minecraft.colliders.math.ImmutableVector;
import lombok.Getter;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.*;
import reactor.core.publisher.ParallelFlux;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RayTraceCollider implements Collider {
    @Getter
    protected final World world;
    @Getter
    private final ImmutableVector center;
    private final ImmutableVector direction;
    private final double distance;
    private final double size;
    private final OrientedBoundingBoxCollider orientedBoundingBoxCollider;

    public RayTraceCollider(World world, ImmutableVector center, ImmutableVector direction, double distance, double size) {
        this.world = world;
        this.center = center;
        this.direction = direction.normalize();
        this.distance = distance;
        this.size = size;
        this.orientedBoundingBoxCollider = this.toOrientedBoundingBox();
    }

    @Override
    public Collider at(Vector center) {
        return new RayTraceCollider(world, ImmutableVector.of(center), direction, distance, size);
    }

    @Override
    public Collider scale(double amount) {
        return null;
    }

    @Override
    public ImmutableVector getHalfExtents() {
        return this.orientedBoundingBoxCollider.getHalfExtents();
    }

    @Override
    public <RT extends Collider> boolean intersects(RT collider) {
        return this.orientedBoundingBoxCollider.intersects(collider);
    }

    @Override
    public boolean contains(Vector vector) {
        return this.orientedBoundingBoxCollider.contains(vector);
    }

    @Override
    public Collider affectEntities(Consumer<ParallelFlux<Entity>> consumer) {
        return this.orientedBoundingBoxCollider.affectEntities(consumer);
    }

    @Override
    public Collider affectBlocks(Consumer<ParallelFlux<Block>> consumer) {
        return this.orientedBoundingBoxCollider.affectBlocks(consumer);
    }

    @Override
    public Collider affectLocations(Consumer<ParallelFlux<Location>> consumer) {
        return this.orientedBoundingBoxCollider.affectLocations(consumer);
    }

    private OrientedBoundingBoxCollider toOrientedBoundingBox() {
        ImmutableVector immutableVector = new ImmutableVector(size, size, distance);
        final double _2PI = 2 * Math.PI;
        final double x = direction.getX();
        final double z = direction.getZ();
        float pitch, yaw;
        if (x == 0 && z == 0) {
            pitch = direction.getY() > 0 ? -90 : 90;
            yaw = 0;
        } else {
            double theta = Math.atan2(-x, z);
            yaw = (float) Math.toDegrees((theta + _2PI) % _2PI);
            double x2 = NumberConversions.square(x);
            double z2 = NumberConversions.square(z);
            double xz = Math.sqrt(x2 + z2);
            pitch = (float) Math.toDegrees(Math.atan(-direction.getY() / xz));
        }
        float roll = 0;
        EulerAngle eulerAngle = new ImmutableVector(pitch, yaw, roll).radians().toEulerAngle();
        return Colliders.obb(world, center.add(direction.multiply(distance)), immutableVector, eulerAngle);
    }

    public Optional<Map.Entry<Block, BlockFace>> getFirstBlock(boolean ignoreLiquids, boolean ignorePassable) {
        RayTraceResult traceResult = world.rayTraceBlocks(center.toLocation(world), direction, distance, ignoreLiquids ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS, ignorePassable);
        if (traceResult == null) return Optional.empty();
        Block block = traceResult.getHitBlock();
        BlockFace blockFace = traceResult.getHitBlockFace();
        return block == null || blockFace == null ? Optional.empty() : Optional.of(Map.entry(block, blockFace));
    }

    public Optional<Block> getBlock(boolean ignoreLiquids, boolean ignorePassable, Predicate<Block> filter) {
        return this.getBlock(ignoreLiquids, ignorePassable, true, filter);
    }

    public Optional<Block> getBlock(boolean ignoreLiquids, boolean ignorePassable, boolean ignoreObstacles, Predicate<Block> filter) {
        BlockIterator it = new BlockIterator(world, center, direction, size, Math.min(100, (int) Math.ceil(distance)));
        while (it.hasNext()) {
            Block block = it.next();
            boolean passable = block.isPassable();
            if (passable) {
                if (block.isLiquid()) {
                    if (ignoreLiquids) {
                        continue;
                    }
                } else if (ignorePassable) {
                    continue;
                }
            }
            if (filter.test(block)) {
                return Optional.of(block);
            }
            if (!ignoreObstacles && !passable) {
                break;
            }
        }
        return Optional.empty();
    }

    private Optional<Entity> getEntity(Predicate<Entity> filter) {
        return this.getEntity(filter);
    }

    private Optional<Entity> getEntity(Predicate<Entity> filter, double distance) {
        RayTraceResult traceResult = world.rayTraceEntities(center.toLocation(world), direction, distance, size, filter);
        if (traceResult == null) return Optional.empty();
        return Optional.ofNullable(traceResult.getHitEntity());
    }

    public Optional<Vector> getPosition(boolean ignoreEntity, boolean ignoreBlock, boolean ignoreLiquid, boolean ignorePassable, Predicate<Entity> entityFilter, Predicate<Block> blockFilter) {
        double distance = this.distance;
        Vector blockPosition = null;
        Vector entityPosition = null;
        Vector position = center.add(direction.normalize().multiply(distance));

        if (!ignoreBlock) {
            Optional<Block> optional = getBlock(ignoreLiquid, ignorePassable, true, blockFilter);
            if (optional.isPresent()) {
                Block block = optional.get();
                ImmutableVector immutableVector = ImmutableVector.of(block.getLocation().toCenterLocation());
                blockPosition = center.add(direction.normalize().multiply(center.distance(immutableVector) - 0.5));
                distance = center.distance(blockPosition);
            }
        }

        if (!ignoreEntity) {
            Optional<Entity> optional = getEntity(entityFilter, distance);
            if (optional.isPresent()) {
                Entity entity = optional.get();
                entityPosition = ImmutableVector.of(entity.getLocation()).add(new ImmutableVector(0, entity.getHeight() / 2, 0));
            }
        }
        return Optional.of(ImmutableVector.of(entityPosition == null ? blockPosition == null ? position : blockPosition : entityPosition));
    }
}
