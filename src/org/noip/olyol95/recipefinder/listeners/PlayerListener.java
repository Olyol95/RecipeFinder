package org.noip.olyol95.recipefinder.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.noip.olyol95.recipefinder.RecipeFinder;

/**
 * Created by Ollie on 19/05/15.
 */
public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {

        RecipeFinder.getPlugin().removeUsersDisplayThread(event.getPlayer().getUniqueId());

    }

}
