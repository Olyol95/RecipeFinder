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
 * Created by Ollie on 18/05/15.
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
