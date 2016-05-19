package cz.grossik.farmcraft2.spigot;

import java.util.List;

import cz.grossik.farmcraft2.bottling.IItemMatcher;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface ISpigotRecipeManager
{

  /**
   * Register a Metal Caster recipe.
   * Note: the mold must be registered with {@link RegisterMold}.
   * @param result Item produced.
   * @param in_fluid Fluid required (fluid type and amount).
   * @param in_mold Mold required.
   * @param in_extra Extra item required (null, if no extra item is required).
   */
  public void addRecipe(IItemMatcher result,FluidStack in_fluid,ItemStack in_mold,IItemMatcher in_extra);

  /**
   * Register a Metal Caster recipe.
   * Note: the mold must be registered with {@link RegisterMold}.
   * @param result Item produced.
   * @param in_fluid Fluid required (fluid type and amount).
   * @param in_mold Mold required.
   * @param in_extra Extra item required (null, if no extra item is required).
   */
  public void addRecipe(IItemMatcher result,FluidStack in_fluid,ItemStack in_mold,IItemMatcher in_extra,int speed);

  /**
   * Register an item as a mold. Only registered items are accepted in the Metal Caster's mold slot.
   * @param mold Item to be registered.
   */
  public void addMold(ItemStack mold);
  
  /**
   * Get a list of all the recipes.
   * @return List of all the recipes.
   */
  public List<ISpigotRecipe> getRecipes();
  
  
  /**
   * Get a list of all registered molds.
   * @return List of all registered molds.
   */
  public List<ItemStack> getMolds();  

  /**
   * Find a casting recipe given a FluidStack and a mold.
   * @param fluid FluidStack that contains the recipe's required fluid.
   * @param mold Mold used by the recipe.
   * @return The casting recipe, or null if no matching recipe.
   */
  public ISpigotRecipe findRecipe(FluidStack fluid,ItemStack mold,ItemStack extra);

  /**
   * Check if an item is registered as a mold.
   * @param stack Item to check.
   * @return true if an item is registered, false if not.
   */
  public boolean isItemMold(ItemStack stack);

  /**
   * Removes a recipe.
   * @param The recipe to remove.
   */
  public void removeRecipe(ISpigotRecipe recipe);
}