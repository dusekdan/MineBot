/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minebot;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import minebot.pathfinding.Action;
import minebot.pathfinding.GoalBlock;
import minebot.pathfinding.Path;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import minebot.pathfinding.PathFinder;
import minebot.mining.Miner;
import minebot.pathfinding.Goal;
import minebot.pathfinding.GoalXZ;
import minebot.pathfinding.GoalYLevel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 *
 * @author leijurv
 */
public class MineBot {
    static boolean isThereAnythingInProgress = false;
    static boolean plsCancel = false;
    public static void main(String[] args) throws IOException, InterruptedException {
        String s = Autorun.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(5) + "../../autorun/runmc.command";
        if (s.contains("jar")) {
            Autorun.start(args);
            return;
        }
        Autorun.runprocess("/usr/local/bin/ant jar");
        Autorun.runprocess("java -Djava.library.path=jars/versions/1.8.8/1.8.8-natives/ -jar dist/MineBot.jar");
    }
    public static void onTick() {
        long start = System.currentTimeMillis();
        onTick1();
        long end = System.currentTimeMillis();
        long time = end - start;
        if (time > 3) {
            System.out.println("Tick took " + time + "ms");
        }
    }
    /**
     * Called by minecraft.java
     */
    public static void onTick1() {
        if (Minecraft.theMinecraft.theWorld == null || Minecraft.theMinecraft.thePlayer == null) {
            cancelPath();
            return;
        }
        if (Minecraft.theMinecraft.currentScreen != null) {
            wasScreen = true;
        } else {
            if (isLeftClick) {
                pressTime = 5;
            }
            if (wasScreen) {
                wasScreen = false;
                pressTime = -10;
            }
        }
        lookingYaw = false;
        lookingPitch = false;
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        World theWorld = Minecraft.theMinecraft.theWorld;
        BlockPos playerFeet = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (currentPath != null) {
            if (currentPath.tick()) {
                if (currentPath != null && currentPath.failed) {
                    clearPath();
                    GuiScreen.sendChatMessage("Recalculating because path failed", true);
                    nextPath = null;
                    findPathInNewThread(playerFeet);
                } else {
                    clearPath();
                }
                currentPath = null;
                if (goal.isInGoal(playerFeet)) {
                    GuiScreen.sendChatMessage("All done. At " + goal, true);
                    nextPath = null;
                } else {
                    GuiScreen.sendChatMessage("Done with segment", true);
                    if (nextPath != null || calculatingNext) {
                        if (calculatingNext) {
                            calculatingNext = false;
                            GuiScreen.sendChatMessage("Patiently waiting to finish", true);
                        } else {
                            currentPath = nextPath;
                            nextPath = null;
                            if (!currentPath.start.equals(playerFeet)) {
                                GuiScreen.sendChatMessage("The next path starts at " + currentPath.start + " but I'm at " + playerFeet + ". not doing it", true);
                                currentPath = null;
                                findPathInNewThread(playerFeet);
                            } else {
                                GuiScreen.sendChatMessage("Going onto next", true);
                                if (!currentPath.goal.isInGoal(currentPath.end)) {
                                    planAhead();
                                }
                            }
                        }
                    } else {
                        GuiScreen.sendChatMessage("Hmm. I'm not actually at the goal. Recalculating.", true);
                        findPathInNewThread(playerFeet);
                    }
                }
            } else {
                if (Action.isWater(theWorld.getBlockState(playerFeet).getBlock())) {
                    System.out.println("Jumping because in water");
                    jumping = true;
                }
                if (!lookingPitch) {
                    if (thePlayer.rotationPitch < -20) {
                        thePlayer.rotationPitch++;
                    } else if (thePlayer.rotationPitch > 20) {
                        thePlayer.rotationPitch--;
                    }
                }
            }
        }
        if (isThereAnythingInProgress && Action.isWater(theWorld.getBlockState(playerFeet).getBlock())) {
            System.out.println("Jumping because in water and pathfinding");
            jumping = true;
        }
        if (lookingYaw) {
            System.out.println("desired yaw: " + desiredYaw);
            desiredYaw += 360;
            desiredYaw %= 360;
            float yawDistance = Minecraft.theMinecraft.thePlayer.rotationYaw - desiredYaw;
            if (yawDistance > 180) {
                yawDistance -= 360;
            } else if (yawDistance < -180) {
                yawDistance += 360;
            }
            if (Math.abs(yawDistance) > MAX_YAW_CHANGE_PER_TICK) {
                yawDistance = Math.signum(yawDistance) * MAX_YAW_CHANGE_PER_TICK;
            }
            Minecraft.theMinecraft.thePlayer.rotationYaw -= yawDistance;
        }
        if (lookingPitch) {
            float pitchDistance = Minecraft.theMinecraft.thePlayer.rotationPitch - desiredPitch;
            if (pitchDistance > 180) {
                pitchDistance -= 360;
            } else if (pitchDistance < -180) {
                pitchDistance += 360;
            }
            if (Math.abs(pitchDistance) > MAX_PITCH_CHANGE_PER_TICK) {
                pitchDistance = Math.signum(pitchDistance) * MAX_PITCH_CHANGE_PER_TICK;
            }
            Minecraft.theMinecraft.thePlayer.rotationPitch -= pitchDistance;
        }
    }
    static final float MAX_YAW_CHANGE_PER_TICK = 360 / 20;
    static final float MAX_PITCH_CHANGE_PER_TICK = 360 / 20;
    public static boolean wasScreen = false;
    public static boolean calculatingNext = false;
    public static Path currentPath = null;
    public static Path nextPath = null;
    public static Goal goal = null;
    public static int pressTime = 0;
    public static boolean isLeftClick = false;
    public static boolean jumping = false;
    public static boolean forward = false;
    public static boolean backward = false;
    public static boolean left = false;
    public static boolean right = false;
    public static boolean sneak = false;
    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean getIsPressed() {
        return isLeftClick && Minecraft.theMinecraft.currentScreen == null && pressTime >= -1;
    }
    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean isPressed() {
        if (pressTime <= 0) {
            return false;
        } else {
            --pressTime;
            return true;
        }
    }
    /**
     * Called by our code
     */
    public static void letGoOfLeftClick() {
        pressTime = 0;
        isLeftClick = false;
    }
    /**
     * Clears movement, but nothing else. Includes jumping and sneaking, but not
     * left clicking.
     */
    public static void clearMovement() {
        jumping = false;
        forward = false;
        left = false;
        right = false;
        backward = false;
        sneak = false;
    }
    /**
     * Clears movement, clears the current path, and lets go of left click. It
     * purposefully does NOT clear nextPath.
     */
    public static void clearPath() {
        currentPath = null;
        letGoOfLeftClick();
        clearMovement();
    }
    public static String therewasachatmessage(String message) {
        try {
            return therewasachatmessage1(message);
        } catch (Exception e) {
            return message;
        }
    }
    /**
     * Called by GuiScreen.java
     *
     * @param message the message that was sent in chat to trigger this
     * @return what message should actually be sent. can be null to send nothing
     */
    public static String therewasachatmessage1(String message) {
        Minecraft mc = Minecraft.theMinecraft;
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        World theWorld = Minecraft.theMinecraft.theWorld;
        BlockPos playerFeet = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        System.out.println("MSG: " + message);
        String text = (message.charAt(0) == '/' ? message.substring(1) : message).trim();
        if (text.equals("look")) {
            lookAtBlock(new BlockPos(0, 0, 0), true);
            return null;
        }
        if (text.equals("cancel")) {
            cancelPath();
            plsCancel = true;
            return isThereAnythingInProgress ? "Cancelled it, but btw I'm pathfinding right now" : "Cancelled it";
        }
        if (text.equals("st")) {
            System.out.println(theWorld.getBlockState(playerFeet).getBlock());
            System.out.println(theWorld.getBlockState(new BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)).getBlock());
            System.out.println(theWorld.getBlockState(new BlockPos(thePlayer.posX, thePlayer.posY - 2, thePlayer.posZ)).getBlock());
        }
        if (text.equals("lac")) {
            BlockPos pos = closestBlock();
            lookAtBlock(pos, true);
            return pos.toString();
        }
        if (text.startsWith("goal") || text.startsWith("setgoal")) {
            plsCancel = false;
            int ind = text.indexOf(' ') + 1;
            if (ind == 0) {
                return "Set goal to " + (goal = new GoalBlock(playerFeet));
            }
            String[] strs = text.substring(ind).split(" ");
            int[] coords = new int[strs.length];
            for (int i = 0; i < strs.length; i++) {
                try {
                    coords[i] = Integer.parseInt(strs[i]);
                } catch (NumberFormatException nfe) {
                    goal = new GoalBlock(playerFeet);
                    return strs[i] + ". yup. A+ coordinate";//A+? you might even say A*
                }
            }
            switch (strs.length) {
                case 3:
                    goal = new GoalBlock(coords[0], coords[1], coords[2]);
                    break;
                case 2:
                    goal = new GoalXZ(coords[0], coords[1]);
                    break;
                case 1:
                    goal = new GoalYLevel(coords[0]);
                    break;
                default:
                    goal = new GoalBlock(playerFeet);
                    if (strs.length != 0) {
                        return strs.length + " coordinates. Nice.";
                    }
                    break;
            }
            return "Set goal to " + goal;
        }
        if (text.startsWith("path")) {
            plsCancel = false;
            findPathInNewThread(playerFeet);
            return null;
        }
        if (text.startsWith("hardness")) {
            BlockPos bp = MineBot.whatAreYouLookingAt();
            return bp == null ? "0" : (1 / theWorld.getBlockState(bp).getBlock().getPlayerRelativeBlockHardness(Minecraft.theMinecraft.thePlayer, Minecraft.theMinecraft.theWorld, MineBot.whatAreYouLookingAt())) + "";
        }
        if (text.startsWith("info")) {
            BlockPos bp = MineBot.whatAreYouLookingAt();
            Block block = theWorld.getBlockState(bp).getBlock();
            return block + " can walk on: " + Action.canWalkOn(bp) + " can walk through: " + Action.canWalkThrough(bp) + " is full block: " + block.isFullBlock() + " is full cube: " + block.isFullCube();
        }
        if (text.startsWith("mine")) {
            plsCancel = false;
            goMiningInNewThread();
            return null;
        }
        return message;
    }
    /**
     * Cancel the path
     *
     */
    public static void cancelPath() {
        nextPath = null;
        clearPath();
        Miner.stopMining();
    }
    /**
     * Go mining in a new thread. Literally starts a new thread that calls
     * Miner.goMining()
     */
    public static void goMiningInNewThread() {
        new Thread() {
            @Override
            public void run() {
                Miner.goMining();
            }
        }.start();
    }
    /**
     * Go to the specified Y coordinate. Methods blocks via Thread.sleep until
     * currentPath is null. Note: does NOT actually check Y coordinate at the
     * end (so it could have been canceled)
     *
     * @param y
     */
    public static void getToY(int y) {
        if (currentPath != null) {
            cancelPath();
        }
        if (isThereAnythingInProgress) {
            GuiScreen.sendChatMessage("Nope. I'm busy", true);
            return;
        }
        EntityPlayer p = Minecraft.theMinecraft.thePlayer;
        MineBot.goal = new GoalYLevel(y);
        MineBot.findPathInNewThread(new BlockPos(p.posX, p.posY, p.posZ));
        try {
            do {
                Thread.sleep(50);
            } while (currentPath != null);
        } catch (InterruptedException ex) {
            Logger.getLogger(MineBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param target
     */
    public static void findPathInNewThread(BlockPos target) {
        new Thread() {
            @Override
            public void run() {
                if (isThereAnythingInProgress) {
                    return;
                }
                isThereAnythingInProgress = true;
                GuiScreen.sendChatMessage("Starting to search for path from " + target + " to " + goal, true);
                currentPath = findPath(target);
                if (!currentPath.goal.isInGoal(currentPath.end)) {
                    GuiScreen.sendChatMessage("I couldn't get all the way to " + goal + ", but I'm going to get as close as I can", true);
                    isThereAnythingInProgress = false;
                    planAhead();
                } else {
                    GuiScreen.sendChatMessage("Finished finding a path from " + target + " to " + goal, true);
                    isThereAnythingInProgress = false;
                }
            }
        }.start();
    }
    /**
     * In a new thread, pathfind from currentPath.end to goal. Store resulting
     * path in nextPath (or in currentPath if calculatingNext was set to false
     * in the meantime).
     */
    public static void planAhead() {
        new Thread() {
            @Override
            public void run() {
                if (isThereAnythingInProgress) {
                    return;
                }
                isThereAnythingInProgress = true;
                GuiScreen.sendChatMessage("Planning ahead", true);
                calculatingNext = true;
                Path path = findPath(currentPath.end);
                GuiScreen.sendChatMessage("Done planning ahead " + calculatingNext, true);
                if (calculatingNext) {
                    nextPath = path;
                } else {
                    currentPath = path;
                    if (!plsCancel) {
                        planAhead();
                    }
                }
                calculatingNext = false;
                isThereAnythingInProgress = false;
            }
        }.start();
    }
    /**
     * Actually do the pathfinding
     *
     * @param start
     * @return
     */
    public static Path findPath(BlockPos start) {
        PathFinder pf = new PathFinder(start, goal);
        Path path = pf.calculatePath();
        GuiScreen.sendChatMessage("calculated " + start + " to " + path.end, true);
        return path;
        /* if (stone) {
         path.showPathInStone();
         return;
         }*/
    }
    /**
     * Give a block that's sorta close to the player, at foot level
     *
     * @return
     */
    public static BlockPos closestBlock() {
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        World theWorld = Minecraft.theMinecraft.theWorld;
        Block air = Block.getBlockById(0);
        for (int x = (int) (thePlayer.posX - 5); x <= thePlayer.posX + 5; x++) {
            for (int z = (int) (thePlayer.posZ - 5); z <= thePlayer.posZ + 5; z++) {
                BlockPos pos = new BlockPos(x, thePlayer.posY, z);
                Block b = theWorld.getBlockState(pos).getBlock();
                if (!b.equals(air)) {
                    return pos;
                }
            }
        }
        return null;
    }
    /**
     * The desired yaw, as set by whatever action is happening. Remember to also
     * set lookingYaw to true if you really want the yaw to change
     *
     */
    static float desiredYaw;
    /**
     * The desired pitch, as set by whatever action is happening. Remember to
     * also set lookingPitch to true if you really want the yaw to change
     *
     */
    static float desiredPitch;
    /**
     * Set to true if the action wants the player's yaw to be moved towards
     * desiredYaw
     */
    static boolean lookingYaw = false;
    /**
     * Set to true if the action wants the player's pitch to be moved towards
     * desiredPitch
     */
    static boolean lookingPitch = false;
    /**
     * Called by our code in order to look in the direction of the center of a
     * block
     *
     * @param p the position to look at
     * @param alsoDoPitch whether to set desired pitch or just yaw
     * @return is the actual player yaw (and actual player pitch, if alsoDoPitch
     * is true) within ANGLE_THRESHOLD (currently 7°) of looking straight at
     * this block?
     */
    public static boolean lookAtBlock(BlockPos p, boolean alsoDoPitch) {
        Block b = Minecraft.theMinecraft.theWorld.getBlockState(p).getBlock();
        double xDiff = (b.getBlockBoundsMinX() + b.getBlockBoundsMaxX()) / 2;
        double yolo = (b.getBlockBoundsMinY() + b.getBlockBoundsMaxY()) / 2;
        double zDiff = (b.getBlockBoundsMinZ() + b.getBlockBoundsMaxZ()) / 2;
        /*System.out.println("min X: " + b.getBlockBoundsMinX());
         System.out.println("max X: " + b.getBlockBoundsMaxX());
         System.out.println("xdiff: " + xDiff);
         System.out.println("min Y: " + b.getBlockBoundsMinY());
         System.out.println("max Y: " + b.getBlockBoundsMaxY());
         System.out.println("ydiff: " + yolo);
         System.out.println("min Z: " + b.getBlockBoundsMinZ());
         System.out.println("max Z: " + b.getBlockBoundsMaxZ());
         System.out.println("zdiff: " + zDiff);*/
        double x = p.getX() + xDiff;
        double y = p.getY() + yolo;
        double z = p.getZ() + zDiff;
        //System.out.println("Trying to look at " + p + " actually looking at " + whatAreYouLookingAt() + " xyz is " + x + "," + y + "," + z);
        return lookAtCoords(x, y, z, alsoDoPitch);
    }
    /**
     * The threshold for how close it tries to get to looking straight at things
     */
    public static final float ANGLE_THRESHOLD = 7;
    /**
     * Look at coordinates
     *
     * @param x
     * @param y
     * @param z
     * @param alsoDoPitch also adjust the pitch? if false, y is ignored
     * @return is the actual player yaw (and actual player pitch, if alsoDoPitch
     * is true) within ANGLE_THRESHOLD (currently 7°) of looking straight at
     * these coordinates?
     */
    public static boolean lookAtCoords(double x, double y, double z, boolean alsoDoPitch) {
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        double yDiff = (thePlayer.posY + 1.62) - y;
        double yaw = Math.atan2(thePlayer.posX - x, -thePlayer.posZ + z);
        double dist = Math.sqrt((thePlayer.posX - x) * (thePlayer.posX - x) + (-thePlayer.posZ + z) * (-thePlayer.posZ + z));
        double pitch = Math.atan2(yDiff, dist);
        desiredYaw = (float) (yaw * 180 / Math.PI);
        lookingYaw = true;
        float yawDist = Math.abs(desiredYaw - thePlayer.rotationYaw);
        boolean withinRange = yawDist < ANGLE_THRESHOLD || yawDist > 360 - ANGLE_THRESHOLD;
        if (alsoDoPitch) {
            lookingPitch = true;
            desiredPitch = (float) (pitch * 180 / Math.PI);
            float pitchDist = Math.abs(desiredPitch - thePlayer.rotationPitch);
            withinRange = withinRange && (pitchDist < ANGLE_THRESHOLD || pitchDist > 360 - ANGLE_THRESHOLD);
        }
        return withinRange;
    }
    /**
     * calls moveTowardsCoords on the center of this block
     *
     * @param p
     * @return am I moving, or am I still rotating
     */
    public static boolean moveTowardsBlock(BlockPos p) {
        Block b = Minecraft.theMinecraft.theWorld.getBlockState(p).getBlock();
        double xDiff = (b.getBlockBoundsMinX() + b.getBlockBoundsMaxX()) / 2;
        double yolo = (b.getBlockBoundsMinY() + b.getBlockBoundsMaxY()) / 2;
        double zDiff = (b.getBlockBoundsMinZ() + b.getBlockBoundsMaxZ()) / 2;
        /*System.out.println("min X: " + b.getBlockBoundsMinX());
         System.out.println("max X: " + b.getBlockBoundsMaxX());
         System.out.println("xdiff: " + xDiff);
         System.out.println("min Y: " + b.getBlockBoundsMinY());
         System.out.println("max Y: " + b.getBlockBoundsMaxY());
         System.out.println("ydiff: " + yolo);
         System.out.println("min Z: " + b.getBlockBoundsMinZ());
         System.out.println("max Z: " + b.getBlockBoundsMaxZ());
         System.out.println("zdiff: " + zDiff);*/
        double x = p.getX() + xDiff;
        double y = p.getY() + yolo;
        double z = p.getZ() + zDiff;
        //System.out.println("Trying to look at " + p + " actually looking at " + whatAreYouLookingAt() + " xyz is " + x + "," + y + "," + z);
        return moveTowardsCoords(x, y, z);
    }
    /**
     * Move towards coordinates, not necesarily forwards. e.g. if coordinates
     * are closest to being directly behind us, go backwards. This minimizes
     * time spent waiting for rotating
     *
     * @param x
     * @param y
     * @param z
     * @return true if we are moving, false if we are still rotating. we will
     * rotate until within ANGLE_THRESHOLD (currently 7°) of moving in correct
     * direction
     */
    public static boolean moveTowardsCoords(double x, double y, double z) {
        EntityPlayerSP thePlayer = Minecraft.theMinecraft.thePlayer;
        float currentYaw = thePlayer.rotationYaw;
        float yaw = (float) (Math.atan2(thePlayer.posX - x, -thePlayer.posZ + z) * 180 / Math.PI);
        float diff = yaw - currentYaw;
        if (diff < 0) {
            diff += 360;
        }
        float distanceToForward = Math.min(Math.abs(diff - 0), Math.abs(diff - 360)) % 360;
        float distanceToBackward = Math.abs(diff - 180) % 360;
        float distanceToRight = Math.abs(diff - 90) % 360;
        float distanceToLeft = Math.abs(diff - 270) % 360;
        float tmp = Math.round(diff / 90) * 90;
        if (tmp > 359) {
            tmp -= 360;
        }
        desiredYaw = yaw - tmp;
        System.out.println(currentYaw + " " + yaw + " " + diff + " " + tmp + " " + desiredYaw);
        System.out.println(distanceToForward + " " + distanceToLeft + " " + distanceToRight + " " + distanceToBackward);
        lookingYaw = true;
        if (distanceToForward < ANGLE_THRESHOLD || distanceToForward > 360 - ANGLE_THRESHOLD) {
            forward = true;
            return true;
        }
        if (distanceToBackward < ANGLE_THRESHOLD || distanceToBackward > 360 - ANGLE_THRESHOLD) {
            backward = true;
            return true;
        }
        if (distanceToLeft < ANGLE_THRESHOLD || distanceToLeft > 360 - ANGLE_THRESHOLD) {
            left = true;
            return true;
        }
        if (distanceToRight < ANGLE_THRESHOLD || distanceToRight > 360 - ANGLE_THRESHOLD) {
            right = true;
            return true;
        }
        return false;
    }
    /**
     * What block is the player looking at
     *
     * @return the position of it
     */
    public static BlockPos whatAreYouLookingAt() {
        Minecraft mc = Minecraft.theMinecraft;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return mc.objectMouseOver.getBlockPos();
        }
        return null;
    }
}
