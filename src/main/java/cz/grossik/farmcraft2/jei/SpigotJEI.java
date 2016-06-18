package cz.grossik.farmcraft2.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import cz.grossik.farmcraft2.bottling.IItemMatcher;
import cz.grossik.farmcraft2.spigot.GuiSpigot;
import cz.grossik.farmcraft2.spigot.ISpigotRecipe;
import cz.grossik.farmcraft2.spigot.SpigotRecipeManager;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IStackHelper;
import mezz.jei.util.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;


public class SpigotJEI
{

  static public class Wrapper implements IRecipeWrapper
  {
    @Nonnull
    private final List<FluidStack> input_fluid;
    @Nonnull
    private final List<ItemStack> output;
    @Nonnull
    private final List<List<ItemStack>> input;

    @SuppressWarnings("unchecked")
    public Wrapper(@Nonnull ItemStack output, FluidStack input, ItemStack mold, List<ItemStack> extra)
    {
      this.input_fluid = Collections.singletonList(input);
      this.input = Lists.newArrayList(Collections.singletonList(mold),extra);
          Collections.singletonList(input);
      this.output = Collections.singletonList(output);
    }

    @Nonnull
    public List<List<ItemStack>> getInputs()
    {
      return input;
    }

    @Nonnull
    public List<ItemStack> getOutputs()
    {
      return output;
    }

    @Override
    public List<FluidStack> getFluidInputs()
    {
      return input_fluid;
    }

    @Override
    public List<FluidStack> getFluidOutputs()
    {
      return Collections.emptyList();
    }

    @Override
    public void drawAnimations(Minecraft minecraft, int recipeWidth, int recipeHeight)
    {

    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY)
    {
      return null;
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY)
    {
      
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton)
    {
      return false;
    }
  }

  static public class Category implements IRecipeCategory
  {

    protected final ResourceLocation backgroundLocation;
    @Nonnull
    protected final IDrawableAnimated arrow;
    @Nonnull
    private final IDrawable background;
    @Nonnull
    private final String localizedName;
    @Nonnull
    private final IDrawable tank_overlay;
    
    private IJeiHelpers helpers;

    public Category(IJeiHelpers helpers)
    {
      this.helpers = helpers;
      IGuiHelper guiHelper = helpers.getGuiHelper();
      backgroundLocation = new ResourceLocation("farmcraft2", "textures/gui/container/spigot.png");


      IDrawableStatic arrowDrawable = guiHelper.createDrawable(backgroundLocation, 176, 53, 24, 17);
      arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);

      ResourceLocation location = new ResourceLocation("farmcraft2", "textures/gui/container/spigot.png");
      background = guiHelper.createDrawable(location, 38, 16, 68, 54);
      tank_overlay = guiHelper.createDrawable(location, 176, 0, 16, 47);
      localizedName = Translator.translateToLocal("Spigot");

    }

    @Override
    @Nonnull
    public IDrawable getBackground()
    {
      return background;
    }

    @Override
    public void drawExtras(Minecraft minecraft)
    {

    }

    @Override
    public void drawAnimations(Minecraft minecraft)
    {
      arrow.draw(minecraft, 22, 35);
    }

    @Nonnull
    @Override
    public String getTitle()
    {
      return localizedName;
    }

    @Nonnull
    @Override
    public String getUid()
    {
      return "spigot";
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull IRecipeWrapper recipeWrapper)
    {
      IGuiItemStackGroup gui_items = recipeLayout.getItemStacks();
      IGuiFluidStackGroup gui_fluids = recipeLayout.getFluidStacks();
      IStackHelper stack_helper = helpers.getStackHelper();

      gui_items.init(0, false, 47, 34);
      gui_items.init(1, true, 47, 4);
      gui_items.init(2, true, 47, 4);
      gui_fluids.init(3, true, 1, 5, 16, GuiSpigot.TANK_HEIGHT, 1000,false,tank_overlay);
      gui_items.setFromRecipe(0, stack_helper.toItemStackList(recipeWrapper.getOutputs().get(0)));
      gui_items.setFromRecipe(1, stack_helper.toItemStackList(recipeWrapper.getInputs().get(0)));
      gui_items.setFromRecipe(2, stack_helper.toItemStackList(recipeWrapper.getInputs().get(1)));
      gui_fluids.set(3, recipeWrapper.getFluidInputs().get(0));
    }
  }

  static public class Handler implements IRecipeHandler<Wrapper>
  {
    @Override
    @Nonnull
    public Class<Wrapper> getRecipeClass()
    {
      return Wrapper.class;
    }

    @Nonnull
    @Override
    public String getRecipeCategoryUid()
    {
      return "spigot";
    }

    @Override
    @Nonnull
    public IRecipeWrapper getRecipeWrapper(@Nonnull Wrapper recipe)
    {
      return recipe;
    }

    @Override
    public boolean isRecipeValid(@Nonnull Wrapper recipe)
    {
      return true;
    }
  }

  static public List<Wrapper> getRecipes()
  {
    List<Wrapper> recipes = new ArrayList<Wrapper>();

    for(ISpigotRecipe recipe : SpigotRecipeManager.instance.getRecipes())
    {
      ItemStack output = recipe.getOutput();

      if(output != null)
      {
        IItemMatcher extra = recipe.getInputExtra();
        recipes.add(new Wrapper(
            output,recipe.getInput(),
            recipe.getMold(),
            extra == null?Collections.<ItemStack>emptyList():extra.getItems()));
      }
    }

    return recipes;
  }
}