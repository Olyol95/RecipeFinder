package org.noip.olyol95.recipefinder;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class RecipeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {

        if (command.getName().equalsIgnoreCase("recipe")) {

            if (commandSender.equals(Bukkit.getConsoleSender())) {

                commandSender.sendMessage("Must be a player to access this command.");
                return null;

            }

            Player player = (Player) commandSender;
            RecipeFinder.getPlugin().setPlayerLanguage(player, RecipeFinder.getPlugin().fetchLocaleFromHandle(player));

            ArrayList<String> argList = new ArrayList<String>(Arrays.asList(strings));

            RecipeFinder.getPlugin().showMatchingItemNames(player, RecipeFinder.getPlugin().parseQuery(argList));

        }

        return null;

    }

}
