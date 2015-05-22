package org.noip.olyol95.recipefinder;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.noip.olyol95.recipefinder.listeners.InventoryListener;
import org.noip.olyol95.recipefinder.listeners.PlayerListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

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
public class RecipeFinder extends JavaPlugin {

    private static RecipeFinder plugin;

    Hashtable<UUID,DisplayThread> usersThreads;

    @Override
    public void onEnable() {

        plugin = this;

        usersThreads = new Hashtable<>();

        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new InventoryListener(), this);
        pluginManager.registerEvents(new PlayerListener(), this);

    }

    @Override
    public void onDisable() {

        for (UUID user: usersThreads.keySet()) {

            removeUsersDisplayThread(user);

        }

        usersThreads = null;

        plugin = null;

    }

    public static RecipeFinder getPlugin() {

        return plugin;

    }

    public DisplayThread getUsersDisplayThread(UUID user) {

        if (usersThreads.keySet().contains(user)) {

            return usersThreads.get(user);

        } else {

            return null;

        }

    }

    public void setUsersDisplayThread(UUID user, DisplayThread displayThread) {

        removeUsersDisplayThread(user);

        usersThreads.put(user,displayThread);

    }

    public void removeUsersDisplayThread(UUID user) {

        DisplayThread thread;

        if ((thread = getUsersDisplayThread(user)) != null) {

            thread.kill();
            usersThreads.remove(user);

        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("recipe")) {

            if (sender.equals(Bukkit.getConsoleSender())) {

                sender.sendMessage("Must be a player to access this command.");
                return true;

            } else {

                if (args.length < 1) {

                    Player player = sender.getServer().getPlayer(sender.getName());
                    ItemStack itemStack = player.getItemInHand();

                    if (itemStack != null) {

                        List<Recipe> recipes = Bukkit.getRecipesFor(sender.getServer().getPlayer(sender.getName()).getItemInHand());

                        if (recipes.size() > 0) {

                            showRecipesToPlayer(player, recipes);
                            return true;

                        } else {

                            sender.sendMessage(ChatColor.RED+"No recipe found for: "+itemStack.getType().toString().toLowerCase().replace("_"," "));
                            return true;

                        }

                    } else {

                        sender.sendMessage(ChatColor.RED+"Insufficient arguments!");
                        return false;

                    }

                } else {

                    String itemName = args[0];

                    for (int i = 1; i < args.length; i++) {

                        itemName = itemName+" "+args[i];

                    }

                    List<Recipe> recipes = getRecipesForItem(itemName);

                    if (recipes.size() > 0) {

                        showRecipesToPlayer(sender.getServer().getPlayer(sender.getName()), recipes);

                        return true;

                    } else {

                        sender.sendMessage(ChatColor.RED+"No recipe found for: "+itemName);
                        return true;

                    }


                }

            }

        }

        return false;

    }

    public void showRecipesToPlayer(Player player, List<Recipe> recipes) {

        if (recipes.size() > 0) {

            DisplayThread displayThread = new DisplayThread(player);
            setUsersDisplayThread(player.getUniqueId(), displayThread);
            displayThread.showRecipes(recipes);

        }

    }

    public List<Recipe> getRecipesForItem(String itemName) {

        ArrayList<Recipe> recipes = new ArrayList<>();

        itemName = itemName.toLowerCase();

        for (Material material : Material.values()) {

            String materialName = material.toString().toLowerCase().replace("item", "").replace("_", " ");

            String[] itemWords = itemName.split(" ");
            String[] materialWords = materialName.split(" ");

            double degree = 0;

            for (String itemWord : itemWords) {

                for (String materialWord : materialWords) {

                    if (materialWord.contains(itemWord)) {

                        degree += ((double) itemWord.length() / materialWord.length()) * (100 / itemWords.length);

                    } else if (itemWord.contains(materialWord)) {

                        degree += ((double) materialWord.length() / itemWord.length()) * (100 / itemWords.length);

                    }

                }

            }

            if (degree > 75.0) {

                List<Recipe> materialRecipes = Bukkit.getRecipesFor(new ItemStack(material));

                for (Recipe recipe : materialRecipes) {

                    recipes.add(recipe);

                }

            }

        }

        for (DyeColor colour: DyeColor.values()) {

            String[] itemWords = itemName.split(" ");
            String[] materialWords;

            for (int i = 0; i < 2; i++) {

                if (i == 0) {

                    materialWords = (colour.toString().toLowerCase().replace("_"," ") + " wool").split(" ");

                } else {

                    materialWords = (colour.toString().toLowerCase().replace("_"," ") + " dye").split(" ");

                }

                double degree = 0;

                for (String itemWord : itemWords) {

                    for (String materialWord : materialWords) {

                        if (materialWord.contains(itemWord)) {

                            degree += ((double) itemWord.length() / materialWord.length()) * (100 / itemWords.length);

                        } else if (itemWord.contains(materialWord)) {

                            degree += ((double) materialWord.length() / itemWord.length()) * (100 / itemWords.length);

                        }

                    }

                }

                if (degree > 75.0) {

                    List<Recipe> materialRecipes;

                    if (i == 0) {

                        materialRecipes = Bukkit.getRecipesFor(new ItemStack(Material.WOOL, 1, colour.getData()));

                    } else {

                        materialRecipes = Bukkit.getRecipesFor(new ItemStack(Material.INK_SACK, 1, (byte) (15 - colour.getData())));

                    }

                    for (Recipe recipe : materialRecipes) {

                        recipes.add(recipe);

                    }

                }

            }

        }

        return recipes;

    }

}
