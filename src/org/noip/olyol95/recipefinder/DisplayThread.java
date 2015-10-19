package org.noip.olyol95.recipefinder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Finder plugin for Bukkit/Spigot
 * Copyright (C) 2015 Oliver Youle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Oliver Youle
 */
public class DisplayThread extends Thread {

    Inventory inventory;
    List<Recipe> recipes;
    Player player;
    InventoryView inventoryView;

    public DisplayThread(Player player) {

        this.player = player;

    }

    public void showRecipes(List<Recipe> recipes) {

        if (recipes.get(0) instanceof FurnaceRecipe) {

            inventory = Bukkit.createInventory(player, InventoryType.FURNACE, "recipe");
            inventory.setItem(2, sanitiseItemStack(recipes.get(0).getResult()));

        } else {

            inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, "recipe");
            inventory.setItem(0, sanitiseItemStack(recipes.get(0).getResult()));

        }

        this.recipes = recipes;

        inventoryView = player.openInventory(inventory);

        start();

    }

    @Override
    public void run() {

        int fuelCounter = 0;
        int recipeCounter = 0;

        while (player.getOpenInventory().equals(inventoryView)) {

            Recipe recipe = recipes.get(recipeCounter);

            if (recipe instanceof FurnaceRecipe) {

                if (inventory.getType() != InventoryType.FURNACE) {

                    player.closeInventory();
                    inventory = Bukkit.createInventory(player, InventoryType.FURNACE, "recipe");
                    inventoryView = player.openInventory(inventory);

                }

                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;

                inventoryView.setItem(0, sanitiseItemStack(furnaceRecipe.getInput()));
                inventoryView.setItem(1, new ItemStack(RecipeFinder.getPlugin().getFuels().get(fuelCounter)));
                inventory.setItem(2, sanitiseItemStack(furnaceRecipe.getResult()));

                if (fuelCounter + 1 == RecipeFinder.getPlugin().getFuels().size()) {

                    fuelCounter = 0;

                } else {

                    fuelCounter++;

                }

            } else {

                if (inventory.getType() != InventoryType.WORKBENCH) {

                    player.closeInventory();
                    inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, "recipe");
                    inventoryView = player.openInventory(inventory);

                }

                for (int i = 1; i < 10; i++) {

                    inventory.setItem(i,new ItemStack(Material.AIR));

                }

                if (recipe instanceof ShapedRecipe) {

                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

                    String[] shape = shapedRecipe.getShape();

                    for (int i = 0; i < shape.length; i++) {

                        for (int x = 0; x < shape[i].length(); x++) {

                            ItemStack ingredient = sanitiseItemStack(shapedRecipe.getIngredientMap().get(shape[i].toCharArray()[x]));

                            inventoryView.setItem(i * 3 + x + 1, ingredient);

                        }

                    }

                    inventoryView.setItem(0, sanitiseItemStack(shapedRecipe.getResult()));

                } else if (recipe instanceof ShapelessRecipe) {

                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

                    for (int i = 0; i < shapelessRecipe.getIngredientList().size(); i++) {

                        ItemStack ingredient = sanitiseItemStack(shapelessRecipe.getIngredientList().get(i));

                        inventoryView.setItem(i+1, ingredient);

                    }

                    inventoryView.setItem(0,sanitiseItemStack(shapelessRecipe.getResult()));

                }

            }

            player.updateInventory();

            if (recipeCounter + 1 == recipes.size()) {

                recipeCounter = 0;

            } else {

                recipeCounter++;

            }

            try {

                Thread.sleep(2000);

            } catch (InterruptedException e) {

                break;

            }

        }

    }

    public void kill() {

        inventoryView = null;

    }

    public ItemStack sanitiseItemStack(ItemStack brokenStack) {

        if (brokenStack != null && brokenStack.getType() != null) {

            if (brokenStack.getData() != null && brokenStack.getData().getData() < 0) {

                ItemStack newItem = new ItemStack(brokenStack.getType());
                newItem.setData(brokenStack.getData());
                newItem.setAmount(brokenStack.getAmount());

                return newItem;

            }

            return brokenStack;

        } else {

            return new ItemStack(Material.AIR);

        }

    }

}
