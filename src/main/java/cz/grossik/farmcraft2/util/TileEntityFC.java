package cz.grossik.farmcraft2.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import cz.grossik.farmcraft2.Main;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankPropertiesWrapper;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public abstract class TileEntityFC extends TileEntity implements ITickable,IInventory
{
  
  protected class FluidHandler implements IFluidHandler
  {
    private int fill_tank;
    private int drain_tank;
    private IFluidTankProperties[] props;
    
    public FluidHandler(int fill_tank,int drain_tank)
    {
      this.fill_tank = fill_tank;
      this.drain_tank = drain_tank;
      props = new IFluidTankProperties[getTankCount()];
      for(int i = 0; i < props.length; i++)
      {
        props[i] = new FluidTankPropertiesWrapper(getTank(i));
      }
    }

    @Override
    public IFluidTankProperties[] getTankProperties()
    {
      return props;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill)
    {
      if(fill_tank < 0)
      {
        return 0;
      }
      return fillTank(fill_tank,resource,doFill);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain)
    {
      if(drain_tank < 0)
      {
        return null;
      }
      return drainTank(drain_tank,resource,doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
      if(drain_tank < 0)
      {
        return null;
      }
      return drainTank(drain_tank,maxDrain,doDrain);
    }    
  }
  
  public class ItemHandler implements IItemHandler
  {
    protected final int slots;
    protected final ImmutableSet<Integer> insert_slots;
    protected final ImmutableSet<Integer> extract_slots;

    protected boolean canInsert(int slot,ItemStack stack)
    {
      return isItemValidForSlot(slot, stack);
    }

    protected boolean canExtract(int slot)
    {
      return true;
    }

    public ItemHandler(int slots,Set<Integer> insert_slots,Set<Integer> extract_slots)
    {
      this.slots = slots;
      this.insert_slots = ImmutableSet.copyOf(insert_slots);
      this.extract_slots = ImmutableSet.copyOf(extract_slots);
    }
    
    @Override
    public final int getSlots()
    {
      return slots;
    }

    @Override
    public final ItemStack getStackInSlot(int slot)
    {
      return inventory[slot];
    }

    @Override
    public final ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
      if(!insert_slots.contains(slot) || !canInsert(slot,stack))
      {
        return stack;
      }
      ItemStack is = inventory[slot];
      if(is == null)
      {
        if(!simulate)
        {
          inventory[slot] = stack.copy();
          updateInventoryItem(slot);
          markDirty();
        }
        return null;
      } else if(is.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(is, stack))
      {
        if(stack.stackSize + is.stackSize > is.getMaxStackSize())
        {
          stack = stack.copy();
          stack.stackSize = stack.stackSize - is.getMaxStackSize() + is.stackSize;
          if(!simulate)
          {
            is.stackSize = is.getMaxStackSize();
          }
        } else
        {
          if(!simulate)
          {
            is.stackSize += stack.stackSize;
          }
          stack = null;
        }
        if(!simulate)
        {
          updateInventoryItem(slot);
          markDirty();
        }
        return stack;
      }
      return stack;
    }

    @Override
    public final ItemStack extractItem(int slot, int amount, boolean simulate)
    {
      if(!extract_slots.contains(slot) || !canExtract(slot))
      {
        return null;
      }
      ItemStack is = inventory[slot];
      if(is == null)
      {
        return null;
      }
      if(amount > is.stackSize)
      {
        amount = is.stackSize;
      }
      if(!simulate)
      {
        is.stackSize -= amount;
        if(is.stackSize == 0)
        {
          inventory[slot] = null;
        }
        updateInventoryItem(slot);
        markDirty();
      }
      is = is.copy();
      is.stackSize = amount;
      return is;
    }
  }
  
  private int bucket_timer;

  public class ContainerSlot
  {
    public final boolean fill;
    public final int tank_slot;
    public final int slot;
    
    public final Fluid fluid;
    
    public ContainerSlot(int container_tank,int container_slot,boolean container_fill)
    {
      this(container_tank,container_slot,container_fill,null);
    }

    public ContainerSlot(int container_tank,int container_slot,boolean container_fill,Fluid container_fluid)
    {
      tank_slot = container_tank;
      slot = container_slot;
      fill = container_fill;
      fluid = container_fluid;
    }
    
    public void update()
    {
      ItemStack stack = getStackInSlot(slot);
      if(stack == null || stack.stackSize > 1)
      {
        return;
      }
      
      FluidTank tank = getTank(tank_slot);
      if(fill)
      {
        if(stack.getItem() instanceof IFluidContainerItem)
        {
          IFluidContainerItem fluid_cont = (IFluidContainerItem)stack.getItem();
          FluidStack drained = tank.drain(25, false);
          if(drained == null || drained.amount == 0)
          {
            return;
          }
          int filled = fluid_cont.fill(stack, drained, false);
          if(filled == 0)
          {
            return;
          }
          drained = tank.drain(filled, true);
          fluid_cont.fill(stack, drained, true);
          updateTank(tank_slot);
          updateInventoryItem(slot);
        } else if(bucket_timer == 0)
        {
          ItemStack filled = FluidContainerRegistry.fillFluidContainer(tank.getFluid(),stack);
          if(filled != null)
          {
            setInventorySlotContents(slot,filled);
            int drain = FluidContainerRegistry.getContainerCapacity(filled);
            tank.drain(drain, true);
            bucket_timer = drain / 25;
            updateTank(tank_slot);
            updateInventoryItem(slot);
          }
        }
      } else
      {
        if(stack.getItem() instanceof IFluidContainerItem)
        {
          IFluidContainerItem fluid_cont = (IFluidContainerItem) stack.getItem();
          FluidStack drained = fluid_cont.drain(stack, 25, false);
          if(drained == null || drained.amount == 0 || (fluid != null && drained.getFluid() != fluid))
          {
            return;
          }

          int filled = tank.fill(drained, false);
          if(filled == 0)
          {
            return;
          }
          drained = fluid_cont.drain(stack, filled, true);
          tank.fill(drained, true);
          updateTank(tank_slot);
          updateInventoryItem(slot);
        } else if(bucket_timer == 0)
        {
          FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
          ItemStack empty = FluidContainerRegistry.drainFluidContainer(stack);
          if(fluid != null && empty != null)
          {
            int filled = tank.fill(fluid, false);
            if(filled == fluid.amount)
            {
              setInventorySlotContents(slot,empty);
              tank.fill(fluid, true);
              bucket_timer = fluid.amount / 25;
              updateTank(tank_slot);
              updateInventoryItem(slot);
            }
          }
        }
      }
    }
  }
  
  private List<ContainerSlot> conatiner_slots;
  private NBTTagCompound update_packet;
  private boolean initialized;
  
  protected boolean last_redstone_signal;
  protected boolean redstone_signal;
  protected final ItemStack[] inventory;

  private int container_timer;
  
  protected final void addContainerSlot(ContainerSlot cs)
  {
    conatiner_slots.add(cs);
  }

  protected abstract void updateClient();

  protected abstract void updateServer();

  public abstract FluidTank getTank(int slot);
  
  public abstract int getTankCount();
  
  protected abstract void onInitialize();

  public TileEntityFC()
  {
    conatiner_slots = new ArrayList<ContainerSlot>();
    last_redstone_signal = false;
    redstone_signal = false;
    initialized = false;
    inventory = new ItemStack[getSizeInventory()];
    container_timer = 0;
  }
  

  @Override
  public final ItemStack getStackInSlot(int slot)
  {
    return inventory[slot];
  }

  @Override
  public final ItemStack decrStackSize(int slot, int amount)
  {
    if(inventory[slot] != null)
    {
      ItemStack is;

      if(inventory[slot].stackSize <= amount)
      {
        is = inventory[slot];
        inventory[slot] = null;
        updateInventoryItem(slot);
        markDirty();
        return is;
      } else
      {
        is = inventory[slot].splitStack(amount);

        if(inventory[slot].stackSize == 0)
        {
          inventory[slot] = null;
        }
        updateInventoryItem(slot);
        markDirty();
        return is;
      }
    } else
    {
      return null;
    }
  }

  @Override
  public ItemStack removeStackFromSlot(int slot)
  {
    if(inventory[slot] != null)
    {
      ItemStack is = inventory[slot];
      inventory[slot] = null;
      updateInventoryItem(slot);
      markDirty();
      return is;
    } else
    {
      return null;
    }
  }

  @Override
  public final void setInventorySlotContents(int slot, ItemStack stack)
  {
    inventory[slot] = stack;

    if(stack != null && stack.stackSize > this.getInventoryStackLimit())
    {
      stack.stackSize = this.getInventoryStackLimit();
    }
    updateInventoryItem(slot);
    markDirty();
  }
  
  @Override
  public int getInventoryStackLimit()
  {
    return 64;
  }

  @Override
  public void invalidate()
  {
    super.invalidate();
    initialized = false;
    onChunkUnload();
  }


  @Override
  public final SPacketUpdateTileEntity getUpdatePacket()
  {
    NBTTagCompound nbt = new NBTTagCompound();
    writeToNBT(nbt);    
    return new SPacketUpdateTileEntity(getPos(), 0, nbt);
  }
  
  @Override
  public NBTTagCompound getUpdateTag()
  {
    return writeToNBT(null);
  }
  
  protected final void updateTank(int slot)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    writeTankToNBT(update_packet,slot);
  }
  
  protected final void updateInventoryItem(int slot)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    writeInventoryItemToNBT(update_packet,slot);
  }

  protected final void writeTankToNBT(NBTTagCompound compound,int slot)
  {
    NBTTagCompound tag = new NBTTagCompound();
    getTank(slot).writeToNBT(tag);
    compound.setTag("Tank_" + String.valueOf(slot), tag);
  }

  protected final void writeInventoryItemToNBT(NBTTagCompound compound,int slot)
  {
    ItemStack is = inventory[slot];
    NBTTagCompound tag = new NBTTagCompound();
    if(is != null)
    {
      tag.setBoolean("empty", false);
      is.writeToNBT(tag);
    } else
    {
      tag.setBoolean("empty", true);
    }
    compound.setTag("Item_" + String.valueOf(slot), tag);
  }


  @Override
  public void readFromNBT(NBTTagCompound compound)
  {
    super.readFromNBT(compound);

    int i;
    for(i = 0; i < getTankCount(); i++)
    {
      NBTTagCompound tag = (NBTTagCompound)compound.getTag("Tank_" + String.valueOf(i));
      if(tag != null)
      {
        FluidTank tank = getTank(i);
        tank.setFluid(null);
        tank.readFromNBT(tag);
      }
    }
    
    for(i = 0; i < getSizeInventory(); i++)
    {
      NBTTagCompound tag = (NBTTagCompound)compound.getTag("Item_" + String.valueOf(i));
      if(tag != null)
      {
        ItemStack stack = null;
        if(!tag.getBoolean("empty"))
        {
          stack = ItemStack.loadItemStackFromNBT(tag);
        }
        inventory[i] = stack;
      }
    }
    if(compound.hasKey("bucket_timer"))
    {
      container_timer = compound.getInteger("container_timer");
    }
  }
  
  protected void writeTileToNBT(NBTTagCompound compound)
  {
    super.writeToNBT(compound);
  }

  
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound)
  {
    if(compound == null)
    {
      compound = new NBTTagCompound();
    }
    int i;
    super.writeToNBT(compound);
    for(i = 0; i < getTankCount(); i++)
    {
      writeTankToNBT(compound,i);
    }
    for(i = 0; i < getSizeInventory(); i++)
    {
      writeInventoryItemToNBT(compound,i);
    }
    compound.setInteger("container_timer", container_timer);
    return compound;
  }

  protected final void updateValue(String name,int value)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    update_packet.setInteger(name, value);
  }

  protected final void updateValue(String name,long value)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    update_packet.setLong(name, value);
  }

  protected final void updateValue(String name,boolean value)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    update_packet.setBoolean(name, value);
  }

  protected final void updateNBTTag(String name,NBTTagCompound compound)
  {
    if(worldObj.isRemote)
    {
      return;
    }
    if(update_packet == null)
    {
      update_packet = new NBTTagCompound();
      super.writeToNBT(update_packet);
    }
    update_packet.setTag(name, compound);
  }

  protected void sendPacketToNearbyPlayers(NBTTagCompound data)
  {
    data.setInteger("dim", worldObj.provider.getDimension());
    Main.network_channel.sendToAllAround(new MessageTileEntitySync(data),
        new TargetPoint(worldObj.provider.getDimension(),pos.getX(),pos.getY(),pos.getZ(),192));
  }
   
  protected void sendPacketToPlayer(NBTTagCompound data,EntityPlayerMP player)
  {
    data.setInteger("dim", worldObj.provider.getDimension());
    Main.network_channel.sendTo(new MessageTileEntitySync(data),player);
  }

  @Override
  public void update()
  {
    if(!(initialized || isInvalid()))
    {
      onInitialize();
      initialized = true;
    }
    
    if(!worldObj.isRemote)
    {
      if(update_packet == null)
      {
        update_packet = new NBTTagCompound();
        super.writeToNBT(update_packet);
      }
      for(ContainerSlot cs:conatiner_slots)
      {
        cs.update();
      }
      if(container_timer > 0)
      {
        container_timer--;
      }
      updateServer();
      
      if(update_packet != null)
      {
        sendPacketToNearbyPlayers(update_packet);
      }
      update_packet = null;
    } else
    {
      updateClient();
    }
    last_redstone_signal = redstone_signal;
  }

  @Override
  public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
  {
    super.onDataPacket(net, pkt);
    if(worldObj.isRemote)
    {
      readFromNBT(pkt.getNbtCompound());
    }
    //worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
  }
    
  @Override
  public boolean isUseableByPlayer(EntityPlayer player)
  {
    return worldObj.getTileEntity(getPos()) != this ? false : player.getDistanceSq(getPos()) <= 64.0D;
  }

  @Override
  public int getField(int id)
  {
    return 0;
  }

  @Override
  public void setField(int id, int value)
  {

  }

  @Override
  public int getFieldCount()
  {
    return 0;
  }

  @Override
  public void clear()
  {

  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public boolean hasCustomName()
  {
    return false;
  }

  @Override
  public ITextComponent getDisplayName()
  {
    return null;
  }
  
  protected void sendToServer(NBTTagCompound tag)
  {
    if(worldObj.isRemote)
    {
      super.writeToNBT(tag);
      tag.setInteger("dim", worldObj.provider.getDimension());
      Main.network_channel.sendToServer(new MessageTileEntitySync(tag));
    }
  }

  @Override
  public void openInventory(EntityPlayer player)
  {
    if(!worldObj.isRemote && player instanceof EntityPlayerMP)
    {
      NBTTagCompound tag = writeToNBT(null);
      sendPacketToPlayer(tag,(EntityPlayerMP)player);
    }
  }

  @Override
  public void closeInventory(EntityPlayer player)
  {
    if(!worldObj.isRemote && player instanceof EntityPlayerMP)
    {
      NBTTagCompound tag = new NBTTagCompound();
      super.writeToNBT(tag);
      sendPacketToPlayer(tag,(EntityPlayerMP)player);
    }
  }
  
  protected final FluidStack drainTank(int slot, FluidStack resource, boolean doDrain)
  {
    FluidTank tank = getTank(slot);
    if(resource.isFluidEqual(tank.getFluid()))
    {
      FluidStack drained = tank.drain(resource.amount, doDrain);
      if( doDrain && drained != null && drained.amount > 0)
      {
        updateTank(slot);
        markDirty();
      }
      return drained;
    }
    return null;
  }

  protected final FluidStack drainTank(int slot, int maxDrain, boolean doDrain)
  {
    FluidTank tank = getTank(slot);
    FluidStack drained = tank.drain(maxDrain, doDrain);
    if( doDrain && drained != null && drained.amount > 0)
    {
      updateTank(slot);
      markDirty();
    }
    return drained;
  }
  
  protected final int fillTank(int slot, FluidStack resource, boolean doFill)
  {
    FluidTank tank = getTank(slot);
    int filled = tank.fill(resource, doFill);
    if( doFill && filled > 0)
    {
      updateTank(slot);
      markDirty();
    }
    return filled;
  }
  
  protected IFluidHandler getFluidHandler(EnumFacing facing)
  {
    return null;
  }

  protected IItemHandler getItemHandler(EnumFacing facing)
  {
    return null;
  }

  @Override
  public boolean hasCapability(Capability<?> cap,EnumFacing facing)
  {
    if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
    {
      return getFluidHandler(facing) != null;
    } else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
    {
      return getItemHandler(facing) != null;
    } else
    {
      return super.hasCapability(cap, facing);
    }
  }
  
  @Override
  public <T> T getCapability(Capability<T> cap, EnumFacing facing)
  {
    if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
    {
      IFluidHandler fluid_handler = getFluidHandler(facing);
      if(fluid_handler != null)
      {
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(getFluidHandler(facing));
      } else
      {
        return super.getCapability(cap, facing);
      }
    } else if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
    {
      IItemHandler item_handler = getItemHandler(facing);
      if(item_handler != null)
      {
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getItemHandler(facing));
      } else
      {
        return super.getCapability(cap, facing);
      }
    } else
    {
      return super.getCapability(cap, facing);
    }
  }
}