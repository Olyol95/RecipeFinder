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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Recipe Finder plugin for Bukkit/Spigot
 * Copyright (C) 2016 Oliver Youle
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

    private Hashtable<UUID, DisplayThread> usersThreads;
    private Hashtable<String, Hashtable<String, List<String>>> translations;
    private Hashtable<UUID, List<String>> playerLanguageMap;
    private List<Material> fuels;
    private List<String> languageFiles;

    private String[] validFlags = { "-a", "-r", "-e", "-p" };
    private String serverLocale;

    private boolean languageEnabled = true;

    private Method asNMSCopy;
    private Method a;

    @Override
    public void onEnable() {

        plugin = this;

        languageFiles = new ArrayList<String>();

        if (!FileManager.onEnable()) {

            getLogger().warning("Disabling languages due to errors. Is this plugin up to date?");
            languageEnabled = false;

        }

        fuels = new ArrayList<Material>();

        for (Material m: Material.values()) {

            if (m.isBurnable()) fuels.add(m);

        }

        usersThreads = new Hashtable<UUID,DisplayThread>();
        playerLanguageMap = new Hashtable<UUID, List<String>>();

        if (languageEnabled) {

            translations = FileManager.loadTranslations();

        } else {

            translations = new Hashtable<String, Hashtable<String, List<String>>>();

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

        getCommand("recipe").setTabCompleter(new RecipeTabCompleter());

    }

    @Override
    public void onDisable() {

        for (UUID user: usersThreads.keySet()) {

            removeUsersDisplayThread(user);

        }

        usersThreads = null;
        translations = null;
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

                    Player player = sender.getServer().getPlayer(sender.getName());
                    setPlayerLanguage(player, fetchLocaleFromHandle(player));

                    if (args.length < 1) {

                        ItemStack itemStack = player.getInventory().getItemInMainHand();

                        if (itemStack != null) {

                            List<Recipe> recipes = Bukkit.getRecipesFor(
                                    sender.getServer().getPlayer(sender.getName()).getInventory().getItemInMainHand()
                            );

                            if (recipes.size() > 0) {

                                showRecipesToPlayer(player, recipes);
                                return true;

                            } else {

                                sender.sendMessage(ChatColor.RED + "No recipe found for: " +
                                        itemStack.getType().toString().toLowerCase().replace("_", " "));
                                return true;

                            }

                        } else {

                            sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
                            return false;

                        }

                    } else {

                        ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));

                        Hashtable<String, String> query = parseQuery(argList);

                        List<Recipe> recipes = matchItemName(query, getPlayerLanguages(player)).get("recipes");

                        if (recipes.size() > 0) {

                            showRecipesToPlayer(player, recipes);
                            return true;

                        } else {

                            player.sendMessage(ChatColor.RED + "No recipes found!");

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

    public Hashtable<String, List> matchItemName(Hashtable<String, String> query, List<String> languages) {

        ArrayList<Recipe> recipes = new ArrayList<Recipe>();
        List<String> itemNames = new ArrayList<String>();

        Iterator<Recipe> recipeIterator = getServer().recipeIterator();

        while (recipeIterator.hasNext()) {

            Recipe recipe = recipeIterator.next();
            List<String> possibleItemNames = new ArrayList<String>();

            String name;

            try {

                name = (String) a.invoke(asNMSCopy.invoke(null, recipe.getResult()));

            } catch (Exception e) {

                getLogger().log(Level.SEVERE, "Recipes may not be working properly with this version of Bukkit/Spigot!");
                return new Hashtable<String, List>();

            }

            if (languageEnabled) {

                for (String lang : languages) {

                    if (translations.containsKey(lang) && translations.get(lang).containsKey(name)) {

                        List<String> synonyms = translations.get(lang).get(name);
                        if (synonyms != null && synonyms.size() > 0) {

                            for (String synonym : synonyms) {

                                possibleItemNames.add(synonym);

                            }

                        }

                    }

                }

            } else {

                name = name.replaceAll("^((item|tile)\\.)|[\\._]", "");
                List<String> words = Arrays.asList(name.split("(?=[A-Z])"));
                Collections.reverse(words);
                name = String.join(" ", words);

                possibleItemNames.add(name.toLowerCase());

            }

            for (String possibleMatch : possibleItemNames) {

                boolean matches = true;

                if (query.containsKey("-a")) {
                    matches = matches && possibleMatch.contains(query.get("-a"));
                }
                if (query.containsKey("-e")) {
                    matches = matches && possibleMatch.equals(query.get("-e"));
                }
                if (query.containsKey("-r")) {
                    try {
                        matches = matches && possibleMatch.matches(query.get("-r"));
                    } catch (Exception e) {
                        // swallow syntax exception
                    }
                }
                if (query.containsKey("default")) {
                    matches = matches && possibleMatch.startsWith(query.get("default"));
                }

                if (matches && !recipes.contains(recipe)) recipes.add(recipe);
                if (matches && !itemNames.contains(possibleMatch)) itemNames.add(possibleMatch);

            }

        }

        Hashtable<String, List> data = new Hashtable<String, List>();
        data.put("recipes", recipes);
        data.put("items", itemNames);

        return data;

    }

    public Set<String> getLanguages() {

        return translations.keySet();

    }

    public String getServerLocale() {

        return serverLocale;

    }

    public void setServerLocale(String serverLocale) {

        this.serverLocale = serverLocale;

    }

    public void setLanguageEnabled(boolean languageEnabled) {

        this.languageEnabled = languageEnabled;

    }

    public List<Material> getFuels() {

        return fuels;

    }

    public List<String> getPlayerLanguages(Player player) {

        if (playerLanguageMap.get(player.getUniqueId()).size() > 0) {
            return playerLanguageMap.get(player.getUniqueId());
        } else {
            ArrayList<String> languages = new ArrayList<String>();
            languages.add(getServerLocale());
            return languages;
        }

    }

    public void addPlayerLanguage(Player player, String language) {

        getLogger().info(language);

        UUID playerID = player.getUniqueId();

        if (!playerLanguageMap.containsKey(playerID)) playerLanguageMap.put(playerID, new ArrayList<String>());

        if (!playerLanguageMap.get(playerID).contains(language) && translations.containsKey(language)) {
            playerLanguageMap.get(playerID).add(language);
        }

    }

    public void setPlayerLanguage(Player player, String language) {

        UUID playerID = player.getUniqueId();

        if (playerLanguageMap.containsKey(playerID) && playerLanguageMap.get(playerID).contains(language)) {
            return;
        }

        getLogger().info(language);

        ArrayList<String> languageList = new ArrayList<String>();
        languageList.add(language);
        playerLanguageMap.put(playerID, languageList);

    }

    public void removePlayerLanguage(Player player, String language) {

        UUID playerID = player.getUniqueId();

        if (playerLanguageMap.contains(playerID) && playerLanguageMap.get(playerID).contains(language))
            playerLanguageMap.get(playerID).remove(language);

    }

    public void showMatchingItemNames(Player player, Hashtable<String, String> query) {

        List<String> results = matchItemName(
                query,
                getPlayerLanguages(player)
        ).get("items");

        if (results.size() > 0) {

            int maxWidth = 65;
            ArrayList<String> lines = new ArrayList<String>();
            String line = "   ";

            for (int i = 0; i < results.size(); i++) {

                if (line.length() + results.get(i).length() + 2 > maxWidth) {
                    lines.add(line);
                    line = "   ";
                }

                if (i % 2 == 0) {
                    results.set(i, ChatColor.DARK_AQUA + results.get(i));
                } else {
                    results.set(i, ChatColor.AQUA + results.get(i));
                }

                line += results.get(i) + "  ";

            }

            if (!line.equals("   ")) lines.add(line);

            try {

                int page = 0;
                int linesPerPage = 7;
                if (query.containsKey("-p")) {
                    page = Integer.parseInt(query.get("-p")) - 1;
                }
                int pages = lines.size() / linesPerPage;

                String titleBanner = ChatColor.GREEN + "matching recipes page " + (page + 1) + "/" + (pages + 1);

                ArrayList<String> body;
                if (linesPerPage * (page + 1) > lines.size()) {
                    body = new ArrayList<String>(lines.subList(linesPerPage * page, lines.size()));
                } else {
                    body = new ArrayList<String>(lines.subList(linesPerPage * page, linesPerPage * (page + 1)));
                }
                body.add(0, titleBanner);

                player.sendMessage(String.join("\n", body));

            } catch (Exception e) {

                player.sendMessage(ChatColor.RED + "Illegal page number: " + query.get("-p"));

            }

        } else {

            player.sendMessage(ChatColor.RED + "No recipes found!");

        }

    }

    public boolean isValidFlag(String flag) {

        for (String s : validFlags) {

            if (flag.equals(s)) return true;

        }

        return false;

    }

    public Hashtable<String, String> parseQuery(ArrayList<String> argList) {

        Hashtable<String, String> query = new Hashtable<String, String>();

        for (String flag : validFlags) {

            if (argList.contains(flag)) {

                int index = argList.indexOf(flag);
                argList.remove(index);

                ArrayList<String> term = new ArrayList<String>();

                while (index < argList.size() && !isValidFlag(argList.get(index))) {

                    term.add(argList.get(index));
                    argList.remove(index);

                }

                query.put(flag, String.join(" ", term));

            }

        }

        if (argList.size() > 0) {
            query.put("default", String.join(" ", argList));
        }

        return query;

    }

    public String fetchLocaleFromHandle(Player player) {

        try {
            Object playerHandle = player.getClass().getDeclaredMethod("getHandle").invoke(player, (Object[]) null);
            Field f = playerHandle.getClass().getDeclaredField("locale");
            f.setAccessible(true);
            return (String) f.get(playerHandle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}
