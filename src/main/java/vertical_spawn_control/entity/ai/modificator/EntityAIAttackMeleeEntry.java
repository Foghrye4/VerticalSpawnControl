package vertical_spawn_control.entity.ai.modificator;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.nbt.NBTTagCompound;
import vertical_spawn_control.entity.ai.EntityAIEntry;
import vertical_spawn_control.entity.ai.EntityAIEntryInstance;

public class EntityAIAttackMeleeEntry extends EntityAIEntry {

	public EntityAIAttackMeleeEntry() {
		super(EntityAIAttackMelee.class);
	}

	@Override
	public EntityAIEntryInstance getInstance(NBTTagCompound tag) {
		return new Instance(tag.getInteger("priority"), this, tag.getDouble("speed"), tag.getBoolean("use_long_memory"));
	}
	
	public class Instance extends EntityAIEntryInstance {

		private double speed;
		private boolean useLongMemory;

		public Instance(int priorityIn, EntityAIEntry entryIn, double speedIn, boolean useLongMemoryIn) {
			super(priorityIn, entryIn);
			 speed = speedIn;
			 useLongMemory = useLongMemoryIn;
		}

		@Override
		public void addTaskTo(EntityLiving entity) {
			EntityAIAttackMelee aiInstance = new EntityAIAttackMelee((EntityCreature) entity, speed, useLongMemory);
			entity.tasks.addTask(priority, aiInstance);
		}
		
	}

}
