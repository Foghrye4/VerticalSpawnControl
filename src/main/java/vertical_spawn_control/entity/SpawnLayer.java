package vertical_spawn_control.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.gson.stream.JsonReader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import vertical_spawn_control.VSCMod;

public class SpawnLayer {
	int fromX = -100000;
	int toX = 100000;
	int fromY;
	int toY;
	int fromZ = -100000;
	int toZ = 100000;
	private int priority = 0;
	public boolean blockNaturalSpawn = true;
	public final List<EntitySpawnDefinition> spawnList = new ArrayList<EntitySpawnDefinition>();
	public final List<Class<? extends Entity>> blackList = new ArrayList<Class<? extends Entity>>();
	public final Set<Biome> biomeBlackList = new HashSet<Biome>();
	public final Set<Biome> biomeWhiteList = new HashSet<Biome>();

	public SpawnLayer(JsonReader reader) throws IOException, NBTException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("from") || name.equals("fromY")) {
				fromY = reader.nextInt();
			} else if (name.equals("to") || name.equals("toY")) {
				toY = reader.nextInt();
			} else if (name.equals("fromX")) {
				fromX = reader.nextInt();
			} else if (name.equals("toX")) {
				toX = reader.nextInt();
			} else if (name.equals("fromZ")) {
				fromZ = reader.nextInt();
			} else if (name.equals("toZ")) {
				toZ = reader.nextInt();
			} else if (name.equals("priority")) {
				setPriority(reader.nextInt());
			} else if (name.equals("exclude_biomes")) {
				reader.beginArray();
				while (reader.hasNext()) {
					Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(reader.nextString()));
					biomeBlackList.add(biome);
				}
				reader.endArray();
			} else if (name.equals("only_in_biomes")) {
				reader.beginArray();
				while (reader.hasNext()) {
					Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(reader.nextString()));
					biomeWhiteList.add(biome);
				}
				reader.endArray();
			} else if (name.equals("block_natural_spawn")) {
				blockNaturalSpawn = reader.nextBoolean();
			} else if (name.equals("black_list")) {
				reader.beginArray();
				while (reader.hasNext()) {
					String ename = reader.nextString();
					Class<? extends Entity> entityClass = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(ename))
							.getEntityClass();
					if (entityClass == null)
						throw new NullPointerException("No such entity registered: " + ename);
					blackList.add(entityClass);
				}
				reader.endArray();
			} else if (name.equals("spawn_list")) {
				reader.beginArray();
				while (reader.hasNext()) {
					spawnList.add(new EntitySpawnDefinition(reader));
				}
				reader.endArray();
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		if (fromY > toY) {
			int a = fromY;
			fromY = toY;
			toY = a;
		}
		if (fromX > toX) {
			int a = fromX;
			fromX = toX;
			toX = a;
		}
		if (fromZ > toZ) {
			int a = fromZ;
			fromZ = toZ;
			toZ = a;
		}
	}

	public void onCubeLoad(World world, BlockPos blockPos) {
		EntityPlayer player = world.getClosestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 24, false);
		if (player != null) {
			return;
		}
		if (spawnList.size() == 0)
			return;
		Random rand = world.rand;
		EntitySpawnDefinition def = spawnList.get(rand.nextInt(spawnList.size()));
		if (def.limitReached && --def.nextlimitCheck > 0)
			return;
		
		int fromX1 = Math.max(blockPos.getX(), fromX);
		int toX1 = Math.min(blockPos.getX() + ICube.SIZE - 1, toX);

		int fromZ1 = Math.max(blockPos.getZ(), fromZ);
		int toZ1 = Math.min(blockPos.getZ() + ICube.SIZE - 1, toZ);

		int fromY1 = Math.max(blockPos.getY(), fromY);
		int toY1 = Math.min(blockPos.getY() + ICube.SIZE - 1, toY);
		if (rand.nextFloat() > def.chance) {
			return;
		}
		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		IEntityLivingData data = null;
		int groupSize1 = def.groupSize;
		if (--def.nextlimitCheck <= 0) {
			def.nextlimitCheck = EntitySpawnDefinition.LIMITS_CHECK_FREQUENCY;
			int amountOfSpawned = this.calculateEntitiesOfType(world, def, CubePos.fromBlockCoords(blockPos));
			if (amountOfSpawned > def.spawnLimit) {
				def.limitReached = true;
				return;
			}
			else {
				def.limitReached = false;
			}
		}

		for (int ix = fromX1 + rand.nextInt(5); ix <= toX1; ix += rand.nextInt(5) + 1)
			for (int iz = fromZ1 + rand.nextInt(5); iz <= toZ1; iz += rand.nextInt(5) + 1)
				for (int iy = fromY1 + rand.nextInt(ICube.SIZE - 1); iy <= toY1; iy++) {
					pos.setPos(ix, iy, iz);
					SpawnPlacementType placementCodition = EntitySpawnPlacementRegistry
							.getPlacementForEntity(def.entityClass);
					if (placementCodition == null)
						placementCodition = SpawnPlacementType.ON_GROUND;
					if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placementCodition, world, pos)) {
						continue;
					}
					int light = world.getLight(pos, false);
					if (light < def.minLightLevel)
						continue;
					if (light > def.maxLightLevel)
						continue;
					if (def.spawn(world, pos, data)) {
						def.nextlimitCheck = 1; // Force limits check next call
						if(--groupSize1 <= 0)
							return;
					}
					fromY1 = iy;
					toY1 = iy + 1;
					break;
				}
	}

	private int calculateEntitiesOfType(World world, EntitySpawnDefinition def, CubePos cubePos) {
		int fromX1 = Math.max(fromX, cubePos.getMinBlockX() - 256);
		int fromY1 = Math.max(fromY, cubePos.getMinBlockY() - 256);
		int fromZ1 = Math.max(fromZ, cubePos.getMinBlockZ() - 256);
		int toX1 = Math.min(toX, cubePos.getMaxBlockX() + 256);
		int toY1 = Math.min(toY, cubePos.getMaxBlockY() + 256);
		int toZ1 = Math.min(toZ, cubePos.getMaxBlockZ() + 256);
		
		AxisAlignedBB bb = new AxisAlignedBB(fromX1,fromY1,fromZ1,toX1,toY1,toZ1);
		List<? extends Entity> entities = world.getEntitiesWithinAABB(def.entityClass, bb);
		return entities.size();
	}

	public boolean isPosInside(BlockPos position) {
		return position.getX() > fromX && position.getX() < toX && position.getY() > fromY && position.getY() < toY
				&& position.getZ() > fromZ && position.getZ() < toZ;
	}

	public boolean isIntersectsY(int posYMin, int posYMax) {
		return posYMin >= fromY && posYMin <= toY || posYMax >= fromY && posYMax <= toY
				|| posYMin >= fromY && posYMax <= toY || posYMin <= fromY && posYMax >= toY;
	}

	public boolean isIntersectsX(int posXMin, int posXMax) {
		return posXMin >= fromX && posXMin <= toX || posXMax >= fromX && posXMax <= toX
				|| posXMin >= fromX && posXMax <= toX || posXMin <= fromX && posXMax >= toX;
	}

	public boolean isIntersectsZ(int posZMin, int posZMax) {
		return posZMin >= fromZ && posZMin <= toZ || posZMax >= fromZ && posZMax <= toZ
				|| posZMin >= fromZ && posZMax <= toZ || posZMin <= fromZ && posZMax >= toZ;
	}

	public boolean isEffectiveAtBiomeAtPos(World world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if (biomeBlackList.contains(biome))
			return false;
		if (!biomeWhiteList.isEmpty() && !biomeWhiteList.contains(biome))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
        return "spawnLayer[" + this.fromX + ", " + this.fromY + ", " + this.fromZ + " -> " + this.toX + ", " + this.toY + ", " + this.toZ + "]";
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
