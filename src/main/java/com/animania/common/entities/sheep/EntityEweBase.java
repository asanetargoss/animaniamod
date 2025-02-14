package com.animania.common.entities.sheep;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.animania.Animania;
import com.animania.api.data.EntityGender;
import com.animania.api.interfaces.IImpregnable;
import com.animania.api.interfaces.IMateable;
import com.animania.common.ModSoundEvents;
import com.animania.common.entities.generic.GenericBehavior;
import com.animania.common.handler.BlockHandler;
import com.animania.common.helper.AnimaniaHelper;
import com.animania.compat.top.providers.entity.TOPInfoProviderMateable;
import com.animania.config.AnimaniaConfig;
import com.google.common.base.Optional;

import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityEweBase extends EntityAnimaniaSheep implements TOPInfoProviderMateable, IMateable, IImpregnable
{
	protected ItemStack milk = UniversalBucket.getFilledBucket(ForgeModContainer.getInstance().universalBucket, BlockHandler.fluidMilkSheep);
	public int dryTimer;
	protected static final DataParameter<Boolean> PREGNANT = EntityDataManager.<Boolean>createKey(EntityEweBase.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> HAS_KIDS = EntityDataManager.<Boolean>createKey(EntityEweBase.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> FERTILE = EntityDataManager.<Boolean>createKey(EntityEweBase.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Integer> GESTATION_TIMER = EntityDataManager.<Integer>createKey(EntityEweBase.class, DataSerializers.VARINT);
	protected static final DataParameter<Optional<UUID>> MATE_UNIQUE_ID = EntityDataManager.<Optional<UUID>>createKey(EntityEweBase.class, DataSerializers.OPTIONAL_UNIQUE_ID);

	public EntityEweBase(World worldIn)
	{
		super(worldIn);
		this.setSize(1.0F, 1.0F);
		this.width = 1.0F;
		this.height = 1.0F;
		this.stepHeight = 1.1F;
		this.gender = EntityGender.FEMALE;
		this.mateable = true;
	}

	@Override
	@Nullable
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
	{
		GenericBehavior.initialSpawnFemale(this, EntityAnimaniaSheep.class);

		return livingdata;
	}

	@Override
	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(15.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.265D);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataManager.register(MATE_UNIQUE_ID, Optional.<UUID>absent());
		this.dataManager.register(EntityEweBase.PREGNANT, false);
		this.dataManager.register(EntityEweBase.HAS_KIDS, false);
		this.dataManager.register(EntityEweBase.FERTILE, true);
		this.dataManager.register(EntityEweBase.GESTATION_TIMER, Integer.valueOf(AnimaniaConfig.careAndFeeding.gestationTimer + this.rand.nextInt(200)));
	}

	
	@Override
	public DataParameter<Integer> getGestationParam()
	{
		return GESTATION_TIMER;
	}

	@Override
	public DataParameter<Boolean> getFertileParam()
	{
		return FERTILE;
	}

	@Override
	public DataParameter<Boolean> getHasKidsParam()
	{
		return HAS_KIDS;
	}

	@Override
	protected SoundEvent getAmbientSound()
	{
		return GenericBehavior.getAmbientSound(this, ModSoundEvents.sheepLiving1, ModSoundEvents.sheepLiving2, ModSoundEvents.sheepLiving3, ModSoundEvents.sheepLiving4, ModSoundEvents.sheepLiving5, ModSoundEvents.sheepLiving6, ModSoundEvents.sheepLiving7);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source)
	{
		return GenericBehavior.getRandomSound(ModSoundEvents.sheepHurt1, ModSoundEvents.sheepLiving7);
	}

	@Override
	protected SoundEvent getDeathSound()
	{
		return GenericBehavior.getRandomSound(ModSoundEvents.sheepHurt1, ModSoundEvents.sheepLiving7);
	}

	@Override
	public void playLivingSound()
	{
		SoundEvent soundevent = this.getAmbientSound();

		if (soundevent != null && !this.getSleeping())
			this.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch());
	}

	@Override
	public void onLivingUpdate()
	{
		
		GenericBehavior.livingUpdateFemale(this, EntityRamBase.class);
		
		super.onLivingUpdate();
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		EntityPlayer entityplayer = player;

		if (this.getFed() && this.getWatered() && stack != ItemStack.EMPTY && AnimaniaHelper.isEmptyFluidContainer(stack) && this.getHasKids())
		{
			player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);

			ItemStack one = stack.copy();
			one.setCount(1);
			FluidActionResult result;
			result = FluidUtil.tryFillContainer(one, FluidUtil.getFluidHandler(milk.copy()), 1000, player, true);

			ItemStack filled;
			;
			if (!result.success)
			{
				Item item = stack.getItem();
				if (item == Items.BUCKET)
					filled = milk.copy();
				else if (Loader.isModLoaded("ceramics") && item == Item.getByNameOrId("ceramics:clay_bucket"))
					filled = new ItemStack(Item.getByNameOrId("ceramics:clay_bucket"), 1, 1);
				else
					return false;
			}
			else
				filled = result.result;
			stack.shrink(1);
			AnimaniaHelper.addItem(player, filled);
			this.setWatered(false);

			return true;
		}
		else
			return super.processInteract(player, hand);
	}

	

	@Override
	public boolean isBreedingItem(@Nullable ItemStack stack)
	{
		return stack != ItemStack.EMPTY && (EntityAnimaniaSheep.TEMPTATION_ITEMS.contains(stack.getItem()));
	}

	@Override
	public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, Entity entity, IProbeHitEntityData data)
	{
		if (player.isSneaking())
		{

			if (this.getMateUniqueId() != null)
				probeInfo.text(I18n.translateToLocal("text.waila.mated"));

			if (this.getHasKids())
				probeInfo.text(I18n.translateToLocal("text.waila.milkable"));

			if (this.getFertile() && !this.getPregnant())
			{
				probeInfo.text(I18n.translateToLocal("text.waila.fertile1"));
			}

			if (this.getPregnant())
			{
				if (this.getGestation() > 0)
				{
					int bob = this.getGestation();
					probeInfo.text(I18n.translateToLocal("text.waila.pregnant1") + " (" + bob + " " + I18n.translateToLocal("text.waila.pregnant2") + ")");
				}
				else
				{
					probeInfo.text(I18n.translateToLocal("text.waila.pregnant1"));
				}
			}

			if (this.getSheared())
			{
				if (this.getWoolRegrowthTimer() > 1)
				{
					int bob = this.getWoolRegrowthTimer();
					probeInfo.text(I18n.translateToLocal("text.waila.wool1") + " (" + bob + " " + I18n.translateToLocal("text.waila.wool2") + ")");
				}
			}
			else if (!this.getSheared())
			{
				probeInfo.text(I18n.translateToLocal("text.waila.wool3"));
			}

		}

		TOPInfoProviderMateable.super.addProbeInfo(mode, probeInfo, player, world, entity, data);
	}

	@Override
	public DataParameter<Boolean> getPregnantParam()
	{
		return PREGNANT;
	}
	
	@Override
	public int getDryTimer()
	{
		return dryTimer;
	}
	
	@Override
	public void setDryTimer(int i)
	{
		this.dryTimer = i;
	}

	@Override
	public DataParameter<Optional<UUID>> getMateUniqueIdParam()
	{
		return MATE_UNIQUE_ID;
	}
}
