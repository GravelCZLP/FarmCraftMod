package cz.grossik.farmcraft2.bottling;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface IBottlingRecipeManager
{
  /**
   * Register an Alloy Mixer recipe.
   * @param out Output.
   * @param in_a Input A, can be an {@link ItemStack} or an {@link OreStack}.
   * @param in_b Input B, can be an {@link ItemStack} or an {@link OreStack}.
   */
  public void addRecipe(ItemStack out,IItemMatcher in_a, IItemMatcher in_b);

  /**
   * Register an Alloy Mixer recipe using combination of items.
   * @param in_a Inputs A, elements can be an {@link ItemStack} or an {@link OreStack}.
   * @param in_b Inputs B, elements can be an {@link ItemStack} or an {@link OreStack}.
   */
  public void addRecipe(ItemStack out,IItemMatcher[] in_a, IItemMatcher[] in_b);

  /**
   * Get a list of all the recipes
   * @return List of all the recipes
   */
  public List<IBottlingRecipe> getRecipes();
  
  /**
   * Find a valid recipe that contains the given inputs.
   * A recipe is found if the recipe's inputs contains the fluid in the parameters.
   * @param in_a FluidStack for the first input.
   * @param in_b FluidStack for the second input.
   * @param order [Output] Order in which the input fluids are matched.
   * @return
   */
  public IBottlingRecipe findRecipe(ItemStack in_a,ItemStack in_b);

  
  /**
   * Removes a recipe.
   * @param The recipe to remove.
   */
  public void removeRecipe(IBottlingRecipe recipe);
}