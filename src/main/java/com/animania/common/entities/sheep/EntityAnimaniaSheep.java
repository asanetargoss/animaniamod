package com.animania.common.entities.sheep;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.animania.Animania;
import com.animania.api.data.AnimalContainer;
import com.animania.api.data.EntityGender;
import com.animania.api.interfaces.AnimaniaType;
import com.animania.api.interfaces.IAnimaniaAnimalBase;
import com.animania.common.entities.generic.GenericBehavior;
import com.animania.common.entities.generic.ai.GenericAIAvoidEntity;
import com.animania.common.entities.generic.ai.GenericAIEatGrass;
import com.animania.common.entities.generic.ai.GenericAIFindFood;
import com.animania.common.entities.generic.ai.GenericAIFindSaltLick;
import com.animania.common.entities.generic.ai.GenericAIFindWater;
import com.animania.common.entities.generic.ai.GenericAILookIdle;
import com.animania.common.entities.generic.ai.GenericAIPanic;
import com.animania.common.entities.generic.ai.GenericAISleep;
import com.animania.common.entities.generic.ai.GenericAITempt;
import com.animania.common.entities.generic.ai.GenericAIWanderAvoidWater;
import com.animania.common.entities.generic.ai.GenericAIWatchClosest;
import com.animania.common.helper.AnimaniaHelper;
import com.animania.common.items.ItemEntityEgg;
import com.animania.config.AnimaniaConfig;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityAnimaniaSheep extends EntitySheep implements IShearable, IAnimaniaAnimalBase
{

	public static final Set<Item> TEMPTATION_ITEMS = Sets.newHashSet(AnimaniaHelper.getItemArray(AnimaniaConfig.careAndFeeding.sheepFood));
	protected static final DataParameter<Boolean> WATERED = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> FED = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Optional<UUID>> RIVAL_UNIQUE_ID = EntityDataManager.<Optional<UUID>>createKey(EntityAnimaniaSheep.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	protected static final DataParameter<Boolean> SHEARED = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Integer> SHEARED_TIMER = EntityDataManager.<Integer>createKey(EntityAnimaniaSheep.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> COLOR_NUM = EntityDataManager.<Integer>createKey(EntityAnimaniaSheep.class, DataSerializers.VARINT);
	protected static final DataParameter<Integer> AGE = EntityDataManager.<Integer>createKey(EntityAnimaniaSheep.class, DataSerializers.VARINT);
	protected static final DataParameter<Boolean> HANDFED = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> SLEEPING = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Float> SLEEPTIMER = EntityDataManager.<Float>createKey(EntityAnimaniaSheep.class, DataSerializers.FLOAT);
	protected static final DataParameter<Integer> DYE_COLOR = EntityDataManager.<Integer>createKey(EntityAnimaniaSheep.class, DataSerializers.VARINT);
	protected static final DataParameter<Boolean> INTERACTED = EntityDataManager.<Boolean>createKey(EntityAnimaniaSheep.class, DataSerializers.BOOLEAN);

	private static final String[] SHEEP_TEXTURES = new String[] { "black", "white", "brown" };

	protected int happyTimer;
	public int blinkTimer;
	public int eatTimer;
	protected int fedTimer;
	protected int wateredTimer;
	protected int damageTimer;
	public SheepType sheepType;
	public GenericAIEatGrass<EntityAnimaniaSheep> entityAIEatGrass;
	protected boolean mateable = false;
	protected boolean headbutting = false;
	protected EntityGender gender;
	protected EnumDyeColor color;
	protected boolean hasRemovedBOP = false;

	public EntityAnimaniaSheep(World worldIn)
	{
		super(worldIn);
		this.tasks.taskEntries.clear();
		this.entityAIEatGrass = new GenericAIEatGrass<EntityAnimaniaSheep>(this);
		if (!AnimaniaConfig.gameRules.ambianceMode)
		{
			this.tasks.addTask(2, new GenericAIFindWater<EntityAnimaniaSheep>(this, 1.0D, entityAIEatGrass, EntityAnimaniaSheep.class));
			this.tasks.addTask(3, new GenericAIFindFood<EntityAnimaniaSheep>(this, 1.0D, entityAIEatGrass, true));
		}
		this.tasks.addTask(4, new GenericAIWanderAvoidWater(this, 1.0D));
		this.tasks.addTask(5, new EntityAISwimming(this));
		this.tasks.addTask(6, new GenericAIPanic<EntityAnimaniaSheep>(this, 2.2D));
		this.tasks.addTask(7, new GenericAITempt<EntityAnimaniaSheep>(this, 1.25D, false, EntityAnimaniaSheep.TEMPTATION_ITEMS));
		this.tasks.addTask(6, new GenericAITempt<EntityAnimaniaSheep>(this, 1.25D, Item.getItemFromBlock(Blocks.YELLOW_FLOWER), false));
		this.tasks.addTask(6, new GenericAITempt<EntityAnimaniaSheep>(this, 1.25D, Item.getItemFromBlock(Blocks.RED_FLOWER), false));
		this.tasks.addTask(8, this.entityAIEatGrass);
		this.tasks.addTask(9, new GenericAIAvoidEntity<EntityWolf>(this, EntityWolf.class, 24.0F, 2.0D, 2.2D));
		this.tasks.addTask(10, new GenericAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(11, new GenericAILookIdle<EntityAnimaniaSheep>(this));
		this.tasks.addTask(12, new GenericAIFindSaltLick<EntityAnimaniaSheep>(this, 1.0, entityAIEatGrass));
		if (AnimaniaConfig.gameRules.animalsSleep)
		{
			this.tasks.addTask(11, new GenericAISleep<EntityAnimaniaSheep>(this, 0.8, AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.sheepBed), AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.sheepBed2), EntityAnimaniaSheep.class));
		}
		this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, false, new Class[0]));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPlayer.class));
		this.fedTimer = AnimaniaConfig.careAndFeeding.feedTimer + this.rand.nextInt(100);
		this.wateredTimer = AnimaniaConfig.careAndFeeding.waterTimer + this.rand.nextInt(100);
		this.happyTimer = 60;
		this.blinkTimer = 100 + this.rand.nextInt(100);
		this.enablePersistence();

	}
	
	@Override
	protected void initEntityAI()
    {
    }

	@Override
	protected boolean canDespawn()
	{
		return false;
	}

	@Override
	public void setPosition(double x, double y, double z)
	{
		super.setPosition(x, y, z);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataManager.register(EntityAnimaniaSheep.FED, true);
		this.dataManager.register(EntityAnimaniaSheep.HANDFED, false);
		this.dataManager.register(EntityAnimaniaSheep.WATERED, true);
		this.dataManager.register(EntityAnimaniaSheep.RIVAL_UNIQUE_ID, Optional.<UUID>absent());
		this.dataManager.register(EntityAnimaniaSheep.SHEARED, false);
		this.dataManager.register(EntityAnimaniaSheep.SHEARED_TIMER, Integer.valueOf(AnimaniaConfig.careAndFeeding.woolRegrowthTimer + this.rand.nextInt(500)));
		this.dataManager.register(EntityAnimaniaSheep.SLEEPING, false);
		this.dataManager.register(EntityAnimaniaSheep.SLEEPTIMER, Float.valueOf(0.0F));
		this.dataManager.register(EntityAnimaniaSheep.DYE_COLOR, Integer.valueOf(EnumDyeColor.WHITE.getMetadata()));
		this.dataManager.register(INTERACTED, false);

		if (this.sheepType == SheepType.FRIESIAN)
		{
			this.dataManager.register(EntityAnimaniaSheep.COLOR_NUM, Integer.valueOf(rand.nextInt(3)));
		}
		else if (this.sheepType == SheepType.DORSET)
		{
			this.dataManager.register(EntityAnimaniaSheep.COLOR_NUM, Integer.valueOf(rand.nextInt(2)));
		}
		else if (this.sheepType == SheepType.MERINO)
		{
			this.dataManager.register(EntityAnimaniaSheep.COLOR_NUM, Integer.valueOf(rand.nextInt(2)));
		}
		else if (this.sheepType == SheepType.SUFFOLK)
		{
			this.dataManager.register(EntityAnimaniaSheep.COLOR_NUM, Integer.valueOf(rand.nextInt(2)));
		}
		else
		{
			this.dataManager.register(EntityAnimaniaSheep.COLOR_NUM, 0);
		}

		this.dataManager.register(EntityAnimaniaSheep.AGE, Integer.valueOf(0));

	}

	@Override
	public EntityAnimaniaSheep createChild(EntityAgeable ageable)
	{
		return null;
	}

	@Override
	protected ResourceLocation getLootTable()
	{
		return this instanceof EntityLambBase ? null : this.sheepType.isPrime ? new ResourceLocation(Animania.MODID, "sheep_prime") : new ResourceLocation(Animania.MODID, "sheep_regular");
	}

	public boolean getSheared()
	{
		return this.getBoolFromDataManager(SHEARED);
	}

	public void setSheared(boolean sheared)
	{
		if (sheared)
		{
			this.dataManager.set(EntityAnimaniaSheep.SHEARED, true);
			this.setWoolRegrowthTimer(AnimaniaConfig.careAndFeeding.woolRegrowthTimer + this.rand.nextInt(500));
		}
		else
			this.dataManager.set(EntityAnimaniaSheep.SHEARED, false);
	}

	@Override
	public DataParameter<Boolean> getSleepingParam()
	{
		return SLEEPING;
	}

	@Override
	public DataParameter<Float> getSleepTimerParam()
	{
		return SLEEPTIMER;
	}

	@Override
	public DataParameter<Boolean> getFedParam()
	{
		return FED;
	}

	@Override
	public DataParameter<Boolean> getHandFedParam()
	{
		return HANDFED;
	}

	@Override
	public DataParameter<Boolean> getWateredParam()
	{
		return WATERED;
	}

	public int getWoolRegrowthTimer()
	{
		return this.getIntFromDataManager(SHEARED_TIMER);
	}

	public void setWoolRegrowthTimer(int time)
	{
		this.dataManager.set(EntityAnimaniaSheep.SHEARED_TIMER, Integer.valueOf(time));
	}

	public int getDyeColorNum()
	{
		return this.getIntFromDataManager(EntityAnimaniaSheep.DYE_COLOR);
	}

	public void setDyeColorNum(int col)
	{
		this.dataManager.set(EntityAnimaniaSheep.DYE_COLOR, new Integer(col));
	}

	@Override
	protected void updateAITasks()
	{
		this.eatTimer = this.entityAIEatGrass.getEatingGrassTimer();
	}

	@Override
	protected float getSoundVolume()
	{
		return 0.4F;
	}

	@Override
	protected Item getDropItem()
	{
		return Items.LEATHER;
	}

	@Override
	public void eatGrassBonus()
	{

	}

	public boolean isDyeable()
	{
		return false;
	}

	public EnumDyeColor getDyeColor()
	{
		if (color == null)
		{
			color = EnumDyeColor.byMetadata(this.getDyeColorNum());
		}

		return color;
	}

	@Override
	public void onLivingUpdate()
	{
		if (!hasRemovedBOP)
		{
			if (Loader.isModLoaded("biomesoplenty"))
			{
				Iterator<EntityAITaskEntry> it = this.tasks.taskEntries.iterator();
				while (it.hasNext())
				{
					EntityAITaskEntry entry = it.next();
					EntityAIBase ai = entry.action;
					try
					{
						if (Class.forName("biomesoplenty.common.entities.ai.EntityAIEatBOPGrass").isInstance(ai))
						{
							entry.using = false;
							ai.resetTask();
							it.remove();
						}
					}
					catch (Exception e)
					{
					}
				}

				hasRemovedBOP = true;
			}
		}

		GenericBehavior.livingUpdateCommon(this);

		boolean sheared = this.getSheared();
		if (sheared)
		{
			int shearedTimer = this.getWoolRegrowthTimer();
			shearedTimer--;
			this.setWoolRegrowthTimer(shearedTimer);
			if (shearedTimer < 0)
			{
				this.setSheared(false);

			}
		}

		super.onLivingUpdate();
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		EntityPlayer entityplayer = player;

		if (stack.getItem() instanceof ItemShears && !this.getSheared() && !this.isChild())
		{
			if (!this.world.isRemote)
			{
				this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
			}
			player.swingArm(hand);
			if (this.getSleeping())
			{
				this.setSleeping(false);
			}
		}

		if (stack.getItem() instanceof ItemDye && !this.isChild() && !this.getSheared())
		{
			if (isDyeable())
			{
				EnumDyeColor col = EnumDyeColor.byDyeDamage(stack.getItemDamage());
				if (this.getDyeColor() != col)
				{
					if (!player.isCreative())
						stack.shrink(1);

					this.color = col;
					this.setDyeColorNum(col.getMetadata());
					return true;
				}
				return true;
			}
			else
				return false;
		}

		return GenericBehavior.interactCommon(this, player, hand, this.entityAIEatGrass) ? true : super.processInteract(player, hand);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleStatusUpdate(byte id)
	{
		if (id == 10)
			this.eatTimer = 80;
		else
			super.handleStatusUpdate(id);
	}

	@Override
	public void setInLove(EntityPlayer player)
	{
		if (!this.getSleeping())
			this.world.setEntityState(this, (byte) 18);
	}

	@Override
	public boolean isBreedingItem(@Nullable ItemStack stack)
	{
		return mateable && (stack != ItemStack.EMPTY && this.isSheepBreedingItem(stack.getItem()));
	}

	private boolean isSheepBreedingItem(Item itemIn)
	{
		return TEMPTATION_ITEMS.contains(itemIn) || itemIn == Item.getItemFromBlock(Blocks.YELLOW_FLOWER) || itemIn == Item.getItemFromBlock(Blocks.RED_FLOWER);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		super.writeEntityToNBT(compound);

		compound.setBoolean("Sheared", this.getSheared());
		compound.setInteger("ColorNumber", getColorNumber());
		compound.setInteger("DyeColor", this.getDyeColorNum());

		GenericBehavior.writeCommonNBT(compound, this);

	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound)
	{
		super.readEntityFromNBT(compound);

		this.setColorNumber(compound.getInteger("ColorNumber"));
		this.setSheared(compound.getBoolean("Sheared"));
		this.setDyeColorNum(compound.getInteger("DyeColor"));

		GenericBehavior.readCommonNBT(compound, this);
	}

	@Override
	public DataParameter<Integer> getAgeParam()
	{
		return AGE;
	}

	public int getColorNumber()
	{
		return this.getIntFromDataManager(COLOR_NUM);
	}

	public void setColorNumber(int color)
	{
		this.dataManager.set(COLOR_NUM, Integer.valueOf(color));
	}

	@Override
	public Item getSpawnEgg()
	{
		return ItemEntityEgg.ANIMAL_EGGS.get(new AnimalContainer(this.sheepType, this.gender));
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target)
	{
		return new ItemStack(getSpawnEgg());
	}

	@Override
	public int getPrimaryEggColor()
	{
		return 0;
	}

	@Override
	public int getSecondaryEggColor()
	{
		return 0;
	}

	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos)
	{
		if (!this.getSheared() && !this.isChild())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune)
	{
		return null;
	}

	@Override
	public EntityGender getEntityGender()
	{
		return this.gender;
	}

	@Override
	public Set<Item> getFoodItems()
	{
		return TEMPTATION_ITEMS;
	}

	@Override
	public void setSleepingPos(BlockPos pos)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public BlockPos getSleepingPos()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getBlinkTimer()
	{
		return blinkTimer;
	}

	@Override
	public void setBlinkTimer(int i)
	{
		blinkTimer = i;
	}

	@Override
	public int getEatTimer()
	{
		return eatTimer;
	}

	@Override
	public void setEatTimer(int i)
	{
		eatTimer = i;
	}

	@Override
	public int getFedTimer()
	{
		return fedTimer;
	}

	@Override
	public void setFedTimer(int i)
	{
		fedTimer = i;
	}

	@Override
	public DataParameter<Boolean> getInteractedParam()
	{
		return INTERACTED;
	}

	@Override
	public int getWaterTimer()
	{
		return wateredTimer;
	}

	@Override
	public void setWaterTimer(int i)
	{
		wateredTimer = i;
	}

	@Override
	public int getDamageTimer()
	{
		return damageTimer;
	}

	@Override
	public void setDamageTimer(int i)
	{
		damageTimer = i;
	}

	@Override
	public int getHappyTimer()
	{
		return happyTimer;
	}

	@Override
	public void setHappyTimer(int i)
	{
		happyTimer = i;
	}

	@Override
	public AnimaniaType getAnimalType()
	{
		return sheepType;
	}

}
