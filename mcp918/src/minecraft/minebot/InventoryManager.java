/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minebot;

import java.util.HashMap;
import minebot.mining.MickeyMine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 *
 * @author leijurv
 */
public class InventoryManager {
    static HashMap<String, Integer> maximumAmounts = null;
    public static void initMax() {
        maximumAmounts = new HashMap();
        add("cobblestone", 128);
        add("coal", 128);
        add("redstone_dust", 64);
        add("stone", 64);
        add("dirt", 128);
    }
    public static void add(String itemName, int amount) {
        Item item = Item.getByNameOrId("minecraft:" + itemName);
        if (item == null) {
            GuiScreen.sendChatMessage(itemName + " doesn't exist", true);
            throw new NullPointerException(itemName + " doesn't exist");
        }
        maximumAmounts.put(itemName, amount);
    }
    public static void onTick() {
        if (maximumAmounts == null) {
            initMax();
        }
        HashMap<Item, Integer> amounts = countItems();
        boolean openedInvYet = false;
        for (String itemName : maximumAmounts.keySet()) {
            Item item = Item.getByNameOrId("minecraft:" + itemName);
            if (amounts.get(item) == null) {
                continue;
            }
            int toThrowAway = amounts.get(item) - maximumAmounts.get(item);
            MickeyMine.notifyFullness(itemName, toThrowAway >= 0);
            if (toThrowAway <= 0) {
                continue;
            }
            if (!openedInvYet) {
                MineBot.openInventory();
                openedInvYet = true;
            }
            GuiContainer c = (GuiContainer) Minecraft.theMinecraft.currentScreen;
            for (int i = 0; i < c.inventorySlots.inventorySlots.size(); i++) {
                Slot slot = c.inventorySlots.inventorySlots.get(i);
                if (slot == null) {
                    continue;
                }
                ItemStack is = slot.getStack();
                if (is == null) {
                    continue;
                }
                if (item.equals(is.getItem())) {
                    c.sketchyMouseClick(i, 0, 0);
                    if (is.stackSize <= toThrowAway) {
                        toThrowAway -= is.stackSize;
                        c.sketchyMouseClick(-999, 0, 0);
                    } else {
                        for (int j = 0; j < toThrowAway; j++) {
                            c.sketchyMouseClick(-999, 1, 0);
                        }
                        c.sketchyMouseClick(i, 0, 0);
                        toThrowAway = 0;
                    }
                    if (toThrowAway <= 0) {
                        break;
                    }
                }
            }
        }
        if (openedInvYet) {
            Minecraft.theMinecraft.thePlayer.closeScreen();
        }
    }
    public static HashMap<Item, Integer> countItems() {
        HashMap<Item, Integer> amounts = new HashMap();
        for (ItemStack is : Minecraft.theMinecraft.thePlayer.inventory.mainInventory) {
            if (is != null && is.getItem() != null) {
                if (amounts.get(is.getItem()) == null) {
                    amounts.put(is.getItem(), is.stackSize);
                } else {
                    amounts.put(is.getItem(), is.stackSize + amounts.get(is.getItem()));
                }
            }
        }
        return amounts;
    }
}
