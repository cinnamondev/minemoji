package com.github.cinnamondev.minemoji;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RequestPacks implements Listener {
    @EventHandler
    public void onConfigurationPhase(AsyncPlayerConnectionConfigureEvent event) {
        //event.getConnection().getAudience()
        //        .sendResourcePacks()
    }
}
