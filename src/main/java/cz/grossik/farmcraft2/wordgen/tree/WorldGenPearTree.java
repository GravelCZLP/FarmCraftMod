package cz.grossik.farmcraft2.wordgen.tree;

import java.util.Random;

import cz.grossik.farmcraft2.handler.BlockHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class WorldGenPearTree extends WorldGenAbstractTree
{
    private static final IBlockState field_181653_a = Blocks.log.getDefaultState();
    private static final IBlockState field_181654_b = BlockHandler.leavesPearNormal.getDefaultState();
    /** The minimum height of a generated tree. */
    private final int minTreeHeight;
    /** True if this tree should grow Vines. */
    private final boolean vinesGrow;
    /** The metadata value of the wood to use in tree generation. */
    private final IBlockState metaWood;
    /** The metadata value of the leaves to use in tree generation. */
    private final IBlockState metaLeaves;

    public WorldGenPearTree(boolean p_i2027_1_)
    {
        this(p_i2027_1_, 4, field_181653_a, field_181654_b, false);
    }

    public WorldGenPearTree(boolean p_i46446_1_, int p_i46446_2_, IBlockState p_i46446_3_, IBlockState p_i46446_4_, boolean p_i46446_5_)
    {
        super(p_i46446_1_);
        this.minTreeHeight = p_i46446_2_;
        this.metaWood = p_i46446_3_;
        this.metaLeaves = p_i46446_4_;
        this.vinesGrow = p_i46446_5_;
    }

    public boolean generate(World worldIn, Random rand, BlockPos position)
    {
        int i = rand.nextInt(3) + this.minTreeHeight;
        boolean flag = true;

        if (position.getY() >= 1 && position.getY() + i + 1 <= worldIn.getHeight())
        {
            for (int j = position.getY(); j <= position.getY() + 1 + i; ++j)
            {
                int k = 1;

                if (j == position.getY())
                {
                    k = 0;
                }

                if (j >= position.getY() + 1 + i - 2)
                {
                    k = 2;
                }

                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int l = position.getX() - k; l <= position.getX() + k && flag; ++l)
                {
                    for (int i1 = position.getZ() - k; i1 <= position.getZ() + k && flag; ++i1)
                    {
                        if (j >= 0 && j < worldIn.getHeight())
                        {
                            if (!this.isReplaceable(worldIn,blockpos$mutableblockpos.set(l, j, i1)))
                            {
                                flag = false;
                            }
                        }
                        else
                        {
                            flag = false;
                        }
                    }
                }
            }

            if (!flag)
            {
                return false;
            }
            else
            {
                IBlockState state = worldIn.getBlockState(position.down());

                if (state.getBlock().canSustainPlant(state, worldIn, position.down(), net.minecraft.util.EnumFacing.UP, (cz.grossik.farmcraft2.block.sapling.Block_SaplingPear)BlockHandler.SaplingPear) && position.getY() < worldIn.getHeight() - i - 1)
                {
                    this.setDirtAt(worldIn, position.down());
                    int k2 = 3;
                    int l2 = 0;

                    for (int i3 = position.getY() - k2 + i; i3 <= position.getY() + i; ++i3)
                    {
                        int i4 = i3 - (position.getY() + i);
                        int j1 = l2 + 1 - i4 / 2;

                        for (int k1 = position.getX() - j1; k1 <= position.getX() + j1; ++k1)
                        {
                            int l1 = k1 - position.getX();

                            for (int i2 = position.getZ() - j1; i2 <= position.getZ() + j1; ++i2)
                            {
                                int j2 = i2 - position.getZ();

                                if (Math.abs(l1) != j1 || Math.abs(j2) != j1 || rand.nextInt(2) != 0 && i4 != 0)
                                {
                                    BlockPos blockpos = new BlockPos(k1, i3, i2);
                                    state = worldIn.getBlockState(blockpos);

                                    if (state.getBlock().isAir(state, worldIn, blockpos) || state.getBlock().isLeaves(state, worldIn, blockpos) || state.getMaterial() == Material.vine)
                                    {
                                        this.setBlockAndNotifyAdequately(worldIn, blockpos, this.metaLeaves);
                                    }
                                }
                            }
                        }
                    }

                    for (int j3 = 0; j3 < i; ++j3)
                    {
                        BlockPos upN = position.up(j3);
                        state = worldIn.getBlockState(upN);

                        if (state.getBlock().isAir(state, worldIn, upN) || state.getBlock().isLeaves(state, worldIn, upN) || state.getMaterial() == Material.vine)
                        {
                            this.setBlockAndNotifyAdequately(worldIn, position.up(j3), this.metaWood);

                            if (this.vinesGrow && j3 > 0)
                            {
                                if (rand.nextInt(3) > 0 && worldIn.isAirBlock(position.add(-1, j3, 0)))
                                {
                                    this.func_181651_a(worldIn, position.add(-1, j3, 0), BlockVine.EAST);
                                }

                                if (rand.nextInt(3) > 0 && worldIn.isAirBlock(position.add(1, j3, 0)))
                                {
                                    this.func_181651_a(worldIn, position.add(1, j3, 0), BlockVine.WEST);
                                }

                                if (rand.nextInt(3) > 0 && worldIn.isAirBlock(position.add(0, j3, -1)))
                                {
                                    this.func_181651_a(worldIn, position.add(0, j3, -1), BlockVine.SOUTH);
                                }

                                if (rand.nextInt(3) > 0 && worldIn.isAirBlock(position.add(0, j3, 1)))
                                {
                                    this.func_181651_a(worldIn, position.add(0, j3, 1), BlockVine.NORTH);
                                }
                            }
                        }
                    }

                    if (this.vinesGrow)
                    {
                        for (int k3 = position.getY() - 3 + i; k3 <= position.getY() + i; ++k3)
                        {
                            int j4 = k3 - (position.getY() + i);
                            int k4 = 2 - j4 / 2;
                            BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

                            for (int l4 = position.getX() - k4; l4 <= position.getX() + k4; ++l4)
                            {
                                for (int i5 = position.getZ() - k4; i5 <= position.getZ() + k4; ++i5)
                                {
                                    blockpos$mutableblockpos1.set(l4, k3, i5);

                                    state = worldIn.getBlockState(blockpos$mutableblockpos1);
                                    if (state.getBlock().isLeaves(state, worldIn, blockpos$mutableblockpos1))
                                    {
                                        BlockPos blockpos2 = blockpos$mutableblockpos1.west();
                                        BlockPos blockpos3 = blockpos$mutableblockpos1.east();
                                        BlockPos blockpos4 = blockpos$mutableblockpos1.north();
                                        BlockPos blockpos1 = blockpos$mutableblockpos1.south();

                                        if (rand.nextInt(4) == 0 && worldIn.isAirBlock(blockpos2))
                                        {
                                            this.func_181650_b(worldIn, blockpos2, BlockVine.EAST);
                                        }

                                        if (rand.nextInt(4) == 0 && worldIn.isAirBlock(blockpos3))
                                        {
                                            this.func_181650_b(worldIn, blockpos3, BlockVine.WEST);
                                        }

                                        if (rand.nextInt(4) == 0 && worldIn.isAirBlock(blockpos4))
                                        {
                                            this.func_181650_b(worldIn, blockpos4, BlockVine.SOUTH);
                                        }

                                        if (rand.nextInt(4) == 0 && worldIn.isAirBlock(blockpos1))
                                        {
                                            this.func_181650_b(worldIn, blockpos1, BlockVine.NORTH);
                                        }
                                    }
                                }
                            }
                        }

                        if (rand.nextInt(5) == 0 && i > 5)
                        {
                            for (int l3 = 0; l3 < 2; ++l3)
                            {
                                for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
                                {
                                    if (rand.nextInt(4 - l3) == 0)
                                    {
                                        EnumFacing enumfacing1 = enumfacing.getOpposite();
                                        this.func_181652_a(worldIn, rand.nextInt(3), position.add(enumfacing1.getFrontOffsetX(), i - 5 + l3, enumfacing1.getFrontOffsetZ()), enumfacing);
                                    }
                                }
                            }
                        }
                    }

                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }

    private void func_181652_a(World p_181652_1_, int p_181652_2_, BlockPos p_181652_3_, EnumFacing p_181652_4_)
    {
        this.setBlockAndNotifyAdequately(p_181652_1_, p_181652_3_, Blocks.cocoa.getDefaultState().withProperty(BlockCocoa.AGE, Integer.valueOf(p_181652_2_)).withProperty(BlockCocoa.FACING, p_181652_4_));
    }

    private void func_181651_a(World p_181651_1_, BlockPos p_181651_2_, PropertyBool p_181651_3_)
    {
        this.setBlockAndNotifyAdequately(p_181651_1_, p_181651_2_, Blocks.vine.getDefaultState().withProperty(p_181651_3_, Boolean.valueOf(true)));
    }

    private void func_181650_b(World p_181650_1_, BlockPos p_181650_2_, PropertyBool p_181650_3_)
    {
        this.func_181651_a(p_181650_1_, p_181650_2_, p_181650_3_);
        int i = 4;

        for (p_181650_2_ = p_181650_2_.down(); p_181650_1_.isAirBlock(p_181650_2_) && i > 0; --i)
        {
            this.func_181651_a(p_181650_1_, p_181650_2_, p_181650_3_);
            p_181650_2_ = p_181650_2_.down();
        }
    }
}