package cz.grossik.farmcraft2.bottling;

import cz.grossik.farmcraft2.TileEntityFC;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidTank;

public class TileEntityBottling extends TileEntityFC implements ISidedInventory/*,IExoflameHeatable*/
{
  public static final int SLOT_INPUT_A = 0;
  public static final int SLOT_INPUT_B = 1;
  public static final int SLOT_OUTPUT = 2;
  public static final int SLOT_FUEL = 3;

  public int burn_time;

  public int item_burn_time;

  public static int progress;

  private boolean update_burn_times;

  private static final int[] SLOTS_TOP = new int[] { SLOT_INPUT_A, SLOT_INPUT_B };
  private static final int[] SLOTS_BOTTOM = new int[] { SLOT_OUTPUT, SLOT_FUEL };
  private static final int[] SLOTS_SIDES = new int[] { SLOT_FUEL };

  public TileEntityBottling()
  {

  }

  @Override
  public int getSizeInventory()
  {
    return 4;
  }

  @Override
  public void readFromNBT(NBTTagCompound tag)
  {
    super.readFromNBT(tag);
    if(tag.hasKey("BurnTime"))
    {
      burn_time = tag.getInteger("BurnTime");
    }
    if(tag.hasKey("CookTime"))
    {
      progress = tag.getInteger("CookTime");
    }
    if(tag.hasKey("ItemBurnTime"))
    {
      item_burn_time = tag.getInteger("ItemBurnTime");
    }
    if(worldObj != null && !worldObj.isRemote)
    {
      ((Block_Bottling)getBlockType()).setMachineState(worldObj, getPos(), worldObj.getBlockState(getPos()), burn_time > 0);
    }
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
  {
    return oldState.getBlock() != newSate.getBlock();
  }

  @Override
  public void writeToNBT(NBTTagCompound tag)
  {
    super.writeToNBT(tag);
    tag.setInteger("BurnTime", burn_time);
    tag.setInteger("CookTime", progress);
    tag.setInteger("ItemBurnTime", item_burn_time);
  }

  @Override
  public int getInventoryStackLimit()
  {
    return 64;
  }

  public boolean isBurning()
  {
      return this.burn_time > 0;
  }

  @Override
  public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
  {
    return this.worldObj.getTileEntity(getPos()) != this ? false : par1EntityPlayer.getDistanceSq(getPos()) <= 64.0D;
  }

  @Override
  public void openInventory(EntityPlayer player){}

  @Override
  public void closeInventory(EntityPlayer player){}
  
  public static int getItemBurnTime(ItemStack p_145952_0_)
  {
      if (p_145952_0_ == null)
      {
          return 150;
      }
      return net.minecraftforge.fml.common.registry.GameRegistry.getFuelValue(p_145952_0_);   
  }

  
  @Override
  public boolean isItemValidForSlot(int slot, ItemStack stack)
  {
    switch(slot)
    {
      case SLOT_OUTPUT:
        return false;
      case SLOT_FUEL:
        return this.isItemFuel(stack);
    }
    return true;
  }

  @Override
  public int[] getSlotsForFace(EnumFacing side)
  {
    switch(side)
    {
      case DOWN:
        return SLOTS_BOTTOM;
      case UP:
        return SLOTS_TOP;
      default:
        return SLOTS_SIDES;
    }
  }

  /**
   * Returns true if automation can insert the given item in the given slot from
   * the given side. Args: Slot, item, side
   */
  public boolean canInsertItem(int par1, ItemStack par2ItemStack, EnumFacing side)
  {
    return this.isItemValidForSlot(par1, par2ItemStack);
  }

  @Override
  public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side)
  {
    return side != EnumFacing.UP || slot != SLOT_INPUT_A || slot != SLOT_INPUT_B || stack.getItem() == Items.bucket;
  }

  @Override
  protected void updateClient()
  {

  }
  
  private boolean canOutput(IBottlingRecipe recipe)
  {
    ItemStack output = recipe.getOutput();
    ItemStack inv_output = inventory[SLOT_OUTPUT];
    return inv_output == null || (inv_output.isItemEqual(output) && inv_output.stackSize - output.stackSize <= inv_output.getMaxStackSize());
  }

  private void doSmelt(IBottlingRecipe recipe,boolean reversed)
  {
    ItemStack output = recipe.getOutput();
    if(!canOutput(recipe))
    {
      progress = 0;
      return;
    }

    if(++progress == 100)
    {
      progress = 0;
      if(reversed)
      {
        decrStackSize(SLOT_INPUT_B, recipe.getInputA().getAmount());
        decrStackSize(SLOT_INPUT_A, recipe.getInputB().getAmount());
      } else
      {
        decrStackSize(SLOT_INPUT_A, recipe.getInputA().getAmount());
        decrStackSize(SLOT_INPUT_B, recipe.getInputB().getAmount());
      }
      if(inventory[SLOT_OUTPUT] == null)
      {
        inventory[SLOT_OUTPUT] = output.copy();
      } else
      {
        inventory[SLOT_OUTPUT].stackSize += output.stackSize;
      }
      updateInventoryItem(SLOT_OUTPUT);
      markDirty();
    }
  }
  
  @Override
  protected void updateServer()
  {
    int last_burn_time = burn_time;
    int last_progress = progress;
    int last_item_burn_time = item_burn_time;
    
    if(burn_time > 0)
    {
      --burn_time;
    }

    boolean reversed = false;
    IBottlingRecipe recipe = null;
    if(inventory[SLOT_INPUT_A] != null && inventory[SLOT_INPUT_B] != null)
    {
      recipe = BottlingRecipeManager.instance.findRecipe(inventory[SLOT_INPUT_A], inventory[SLOT_INPUT_B]);
      if(recipe == null)
      {
        recipe = BottlingRecipeManager.instance.findRecipe(inventory[SLOT_INPUT_B], inventory[SLOT_INPUT_A]);
        if(recipe != null)
        {
          reversed = true;
        }
      }
    }

    if(burn_time == 0 && recipe != null && canOutput(recipe))
    {
      item_burn_time = burn_time = getItemBurnTime(inventory[SLOT_FUEL]);
      if(burn_time > 0)
      {
        if(inventory[SLOT_FUEL] != null)
        {
          if(--inventory[SLOT_FUEL].stackSize == 0)
          {
            inventory[SLOT_FUEL] = inventory[SLOT_FUEL].getItem().getContainerItem(inventory[SLOT_FUEL]);
          }
          updateInventoryItem(SLOT_FUEL);
        }
      }
    }

    if(burn_time > 0)
    {
      if(recipe != null)
      {
        doSmelt(recipe,reversed);
      } else
      {
        progress = 0;
      }
    } else
    {
      progress = 0;
    }
    
    if(last_burn_time != burn_time || update_burn_times)
    {
      if(last_burn_time*burn_time == 0)
      {
        ((Block_Bottling)getBlockType()).setMachineState(worldObj, getPos(), worldObj.getBlockState(getPos()), burn_time > 0);
      }
      updateValue("BurnTime",burn_time);
    }

    if(last_item_burn_time != item_burn_time || update_burn_times)
    {
      updateValue("ItemBurnTime",item_burn_time);
    }
    update_burn_times = false;

    if(last_progress != progress)
    {
      updateValue("CookTime",progress);
    }  
  }

  
  public static boolean isItemFuel(ItemStack p_145954_0_)
  {
      return getItemBurnTime(p_145954_0_) > 0;
  }
  
  @Override
  public FluidTank getTank(int slot)
  {
    return null;
  }

  @Override
  public int getTankCount()
  {
    return 0;
  }

  @Override
  protected void onInitialize()
  {
  }
}