package org.noip.olyol95.recipefinder;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.noip.olyol95.recipefinder.listeners.InventoryListener;
import org.noip.olyol95.recipefinder.listeners.PlayerListener;

import java.util.*;

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

    private static final char[] capitals = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};

    Hashtable<UUID,DisplayThread> usersThreads;
    Hashtable<String,String> replacements;

    @Override
    public void onEnable() {

        plugin = this;

        usersThreads = new Hashtable<>();
        replacements = setupReplacements();

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
        replacements = null;

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

        Iterator<Recipe> recipeIterator = getServer().recipeIterator();

        while (recipeIterator.hasNext()) {

            Recipe recipe = recipeIterator.next();

            String name = CraftItemStack.asNMSCopy(recipe.getResult()).a();

            for (String occurance: replacements.keySet()) {

                name = name.replaceAll(occurance,replacements.get(occurance));

            }

            String[] words = name.split("\\.");

            ArrayList<String> newWords = new ArrayList<>();

            for (int i = 1; i < words.length; i++) {

                String word = "";

                for (char c: words[i].toCharArray()) {

                    if (isCapitalChar(c)) {

                        newWords.add(word.toLowerCase());
                        word = "";

                    }

                    word = word + c;

                }

                newWords.add(word.toLowerCase());

            }

            String[] itemWords = itemName.split(" ");

            double degree = 0;

            for (String itemWord : itemWords) {

                for (String materialWord : newWords) {

                    if (materialWord.contains(itemWord)) {

                        degree += ((double) itemWord.length() / materialWord.length()) * (100 / itemWords.length);

                    } else if (itemWord.contains(materialWord)) {

                        degree += ((double) materialWord.length() / itemWord.length()) * (100 / itemWords.length);

                    }

                }

            }

            if (degree > 75.0) {

               recipes.add(recipe);

            }

        }

        return recipes;

    }

    private boolean isCapitalChar(char c) {

        for (char capital: capitals) {

            if (c == capital) return true;

        }

        return false;

    }

    public Hashtable<String,String> setupReplacements() {

        Hashtable<String,String> hashtable = new Hashtable<>();

        hashtable.put("Light","Lamp");
        hashtable.put("chestplateCloth","chestplateLeather");
        hashtable.put("leggingsCloth","leggingsLeather");
        hashtable.put("helmetCloth","helmetLeather");
        hashtable.put("bootsCloth","bootsLeather");
        hashtable.put("musicBlock","noteBlock");
        hashtable.put("sparkling","glistering");
        hashtable.put("hatchet","axe");
        hashtable.put("lightgem","glowstone");
        hashtable.put("cloth","wool");
        hashtable.put("stoneSlab2","stoneSlab");
        hashtable.put("red_sandstone","redSandstone");
        hashtable.put("big_oak","darkOak");
        hashtable.put("stonebricksmooth","stoneBrickSmooth");
        hashtable.put("notGate","redstoneTorch");
        hashtable.put("weightedPlate_heavy","ironPressurePlate");
        hashtable.put("weightedPlate_light","goldPressurePlate");
        hashtable.put("seeds_melon","melonSeeds");
        hashtable.put("seeds_pumpkin","pumpkinSeeds");
        hashtable.put("stoneMoss","mossyCobblestone");
        hashtable.put("netherquartz","netherQuartz");
        hashtable.put("fireball","fireCharge");
        hashtable.put("litpumpkin","jackOLantern");
        hashtable.put("writingBook","bookAndQuill");

        return hashtable;

    }

}
