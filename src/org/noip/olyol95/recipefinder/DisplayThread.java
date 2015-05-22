package org.noip.olyol95.recipefinder;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.List;

/**
 * Created by Ollie on 18/05/15.
 */
public class DisplayThread extends Thread {

    private final static Material[] fuels = {Material.LAVA_BUCKET, Material.COAL_BLOCK, Material.BLAZE_ROD, Material.COAL, Material.LOG, Material.WOOD, Material.DAYLIGHT_DETECTOR, Material.BANNER, Material.WOOD_SWORD, Material.WOOD_STEP, Material.STICK, Material.SAPLING};

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
            inventory.setItem(2,generateItemStack(recipes.get(0).getResult()));

        } else {

            inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, "recipe");
            inventory.setItem(0,generateItemStack(recipes.get(0).getResult()));

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

                inventoryView.setItem(0, generateItemStack(furnaceRecipe.getInput()));
                inventoryView.setItem(1, new ItemStack(fuels[fuelCounter]));

                ItemStack result = generateItemStack(furnaceRecipe.getResult());

                if (result.getType().toString().equalsIgnoreCase("INK_SACK")) {

                    String dyeType = result.getData().toString().split(" ")[0];
                    byte data = (byte) (15 - DyeColor.valueOf(dyeType).getData());
                    result = new ItemStack(Material.INK_SACK,result.getAmount(),data);

                }

                inventory.setItem(2, result);

                if (fuelCounter + 1 == fuels.length) {

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

                            ItemStack ingredient = generateItemStack(shapedRecipe.getIngredientMap().get(shape[i].toCharArray()[x]));

                            if (ingredient.getType().toString().equalsIgnoreCase("INK_SACK")) {

                                String dyeType = ingredient.getData().toString().split(" ")[0];
                                byte data = (byte) (15 - DyeColor.valueOf(dyeType).getData());
                                ingredient = new ItemStack(Material.INK_SACK,ingredient.getAmount(),data);

                            }

                            inventoryView.setItem(i * 3 + x + 1, ingredient);

                        }

                    }

                    inventoryView.setItem(0, shapedRecipe.getResult());

                } else if (recipe instanceof ShapelessRecipe) {

                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

                    for (ItemStack itemStack : shapelessRecipe.getIngredientList()) {

                        ItemStack ingredient = generateItemStack(itemStack);

                        if (ingredient.getType().toString().equalsIgnoreCase("INK_SACK")) {

                            String dyeType = ingredient.getData().toString().split(" ")[0];
                            byte data = (byte) (15 - DyeColor.valueOf(dyeType).getData());
                            ingredient = new ItemStack(Material.INK_SACK,ingredient.getAmount(),data);

                        }

                        inventoryView.setItem(shapelessRecipe.getIngredientList().indexOf(itemStack)+1, ingredient);

                    }

                    inventoryView.setItem(0,shapelessRecipe.getResult());

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

    public ItemStack generateItemStack(ItemStack brokenStack) {

        if (brokenStack != null && brokenStack.getType() != null) {

            ItemStack newItem = new ItemStack(brokenStack.getType());
            newItem.setData(brokenStack.getData());
            newItem.setAmount(brokenStack.getAmount());
            return newItem;

        } else {

            return new ItemStack(Material.AIR);

        }

    }

}
