package org.noip.olyol95.recipefinder.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Recipe;
import org.noip.olyol95.recipefinder.RecipeFinder;

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
public class InventoryListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getTitle().contains("recipe") && event.getCurrentItem() != null) {

            List<Recipe> recipes = Bukkit.getRecipesFor(event.getCurrentItem());

            if (recipes.size() > 0) {

                RecipeFinder.getPlugin().showRecipesToPlayer(player,recipes);

            }

            event.setCancelled(true);

        }

    }

}
