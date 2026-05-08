package com.github.cinnamondev.minemoji;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;



public class EmojiRenderer implements Listener {//, io.papermc.paper.chat.ChatRenderer {
    private final Minemoji p;
    public EmojiRenderer(Minemoji p) { this.p = p; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        //if (discordSrv) {
        //    // makes reverse emoji lookup simpler
        //    e.message(
        //            p.getEmoteManager().emojize(e.message())
        //    );
        //} else {
        if (e.getPlayer().hasPermission("minemoji.emoji")) {
            //e.renderer(this);
            e.message(
                    p.getEmoteManager().emojize(e.message())
            );
        }
        //}
        //
    }
}
