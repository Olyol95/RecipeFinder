package org.noip.olyol95.recipefinder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.noip.olyol95.recipefinder.util.FileManager;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

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

    private String langFile = FileManager.DEFAULT_LANG_FILE;

    private static final char[] capitals = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};

    private Hashtable<UUID,DisplayThread> usersThreads;
    private Hashtable<String,String> synonyms;
    private ArrayList<Material> fuels;

    private boolean languageEnabled = true;

    private Method asNMSCopy;
    private Method a;

    @Override
    public void onEnable() {

        plugin = this;

        if (!FileManager.onEnable()) {

            getLogger().log(Level.SEVERE,"Error locating binaries/config, is this plugin up to date?");
            setEnabled(false);
            return;

        }

        fuels = new ArrayList<Material>();

        for (Material m: Material.values()) {

            if (m.isBurnable()) fuels.add(m);

        }

        usersThreads = new Hashtable<UUID,DisplayThread>();

        if (languageEnabled) {

            synonyms = FileManager.parseLangToSynonyms();

        } else {

            synonyms = new Hashtable<String,String>();

        }

        try {

            String classPackage = "";
            String packageVersion = "";

            String[] serverPackage = getServer().getClass().getCanonicalName().split("\\.");

            for (int i = 0; i < serverPackage.length-1; i++) {

                if (serverPackage[i].equalsIgnoreCase("craftbukkit")) {

                    packageVersion = serverPackage[i+1];

                }

                classPackage = classPackage + serverPackage[i] + ".";

            }

            Class<?> craftItemStack = Class.forName(classPackage+"inventory.CraftItemStack");
            asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);

            a = Class.forName("net.minecraft.server."+packageVersion+".ItemStack").getDeclaredMethod("a");

        } catch (Exception e) {

            e.printStackTrace();

            getLogger().log(Level.SEVERE, "This plugin is not compatible with your version of Bukkit/Spigot.");
            setEnabled(false);
            return;

        }

        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new InventoryListener(), this);
        pluginManager.registerEvents(new PlayerListener(), this);

    }

    @Override
    public void onDisable() {

        for (UUID user: usersThreads.keySet()) {

            removeUsersDisplayThread(user);

        }

        usersThreads = null;
        synonyms = null;
        fuels = null;

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

                if (sender.hasPermission("recipe.lookup")) {

                    if (args.length < 1) {

                        Player player = sender.getServer().getPlayer(sender.getName());
                        ItemStack itemStack = player.getInventory().getItemInMainHand();

                        if (itemStack != null) {

                            List<Recipe> recipes = Bukkit.getRecipesFor(sender.getServer().getPlayer(sender.getName()).getInventory().getItemInMainHand());

                            if (recipes.size() > 0) {

                                showRecipesToPlayer(player, recipes);
                                return true;

                            } else {

                                sender.sendMessage(ChatColor.RED + "No recipe found for: " + itemStack.getType().toString().toLowerCase().replace("_", " "));
                                return true;

                            }

                        } else {

                            sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
                            return false;

                        }

                    } else {

                        String itemName = args[0];

                        for (int i = 1; i < args.length; i++) {

                            itemName = itemName + " " + args[i];

                        }

                        List<Recipe> recipes = getRecipesForItem(itemName);

                        if (recipes.size() > 0) {

                            showRecipesToPlayer(sender.getServer().getPlayer(sender.getName()), recipes);

                            return true;

                        } else {

                            sender.sendMessage(ChatColor.RED + "No recipe found for: " + itemName);
                            return true;

                        }


                    }

                } else {

                    sender.sendMessage(ChatColor.RED+"You do not have permission to do that!");
                    return true;

                }

            }

        } else if (command.getName().equalsIgnoreCase("recipereload")) {

            if (sender.equals(Bukkit.getConsoleSender()) || sender.hasPermission("recipe.reload")) {

                onDisable();
                onEnable();
                sender.sendMessage(ChatColor.GREEN+"Recipe Finder reloaded!");
                return true;

            } else {

                sender.sendMessage(ChatColor.RED+"You do not have permission to do that!");
                return true;

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

        ArrayList<Recipe> recipes = new ArrayList<Recipe>();

        itemName = itemName.toLowerCase();

        Iterator<Recipe> recipeIterator = getServer().recipeIterator();

        while (recipeIterator.hasNext()) {

            Recipe recipe = recipeIterator.next();

            String name;

            try {

                name = (String) a.invoke(asNMSCopy.invoke(null, recipe.getResult()));

            }catch (Exception e) {

                getLogger().log(Level.SEVERE,"Recipes may not be working properly with this version of Bukkit/Spigot!");
                return recipes;

            }

            String[] itemWords = itemName.split(" ");
            String[] newWords;

            if (languageEnabled && synonyms.containsKey(name)) {

                newWords = synonyms.get(name).split(" ");

            } else {

                String[] words = name.split("\\.");

                ArrayList<String> nw = new ArrayList<String>();

                for (int i = 1; i < words.length; i++) {

                    String word = "";

                    for (char c: words[i].toCharArray()) {

                        if (isCapitalChar(c)) {

                            nw.add(word.toLowerCase());
                            word = "";

                        }

                        word = word + c;

                    }

                    nw.add(word.toLowerCase());

                }

                newWords = new String[nw.size()];

                for (int i = 0; i < nw.size(); i++) {

                    newWords[i] = nw.get(i);

                }

            }

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

    public void setLanguage(String langFile) {

        getLogger().log(Level.INFO,"Language file detected: "+langFile);

        this.langFile = langFile;

    }

    public String getLanguage() {

        return langFile;

    }

    private boolean isCapitalChar(char c) {

        for (char capital: capitals) {

            if (c == capital) return true;

        }

        return false;

    }

    public void setLanguageEnabled(boolean languageEnabled) {

        this.languageEnabled = languageEnabled;

    }

    public ArrayList<Material> getFuels() {

        return fuels;

    }

}
