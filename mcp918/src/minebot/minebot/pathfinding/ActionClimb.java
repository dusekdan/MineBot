/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minebot.pathfinding;

import minebot.MineBot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;

/**
 *
 * @author leijurv
 */
public class ActionClimb extends ActionPlaceOrBreak {
    public ActionClimb(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{new BlockPos(start.getX(), start.getY() + 2, start.getZ()), end, new BlockPos(end.getX(), end.getY() + 1, end.getZ())}, new BlockPos[]{new BlockPos(end.getX(), end.getY() - 1, end.getZ())});
    }
    @Override
    protected double calculateCost() {
        if (!canWalkOn(blocksToPlace[0])) {
            return 10000;
        }
        return JUMP_ONE_BLOCK_COST + WALK_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak() * HARDNESS_MULTIPLIER;
    }
    @Override
    protected boolean tick0() {//basically just hold down W and space until we are where we want to be
        MineBot.lookAtBlock(new BlockPos(to.getX(), to.getY() + 1, to.getZ()), false);
        MineBot.forward = true;
        MineBot.jumping = true;
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        BlockPos whereAmI = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (whereAmI.equals(to)) {
            System.out.println("Done climbing to " + to);
            MineBot.forward = false;
            MineBot.jumping = false;
            return true;
        }
        return false;
    }
}