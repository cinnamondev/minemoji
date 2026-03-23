package com.github.cinnamondev.minemoji;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;


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

    //@Override
    //public @NotNull Component render(@NonNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
    //    return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, p.getEmoteManager().emojize(message), viewer);
    //}
}
