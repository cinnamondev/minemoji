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


public class EmojiRenderer implements Listener, io.papermc.paper.chat.ChatRenderer {
    private final Minemoji p;
    private final boolean discordSrv;
    public EmojiRenderer(Minemoji p, boolean discordSrvEnabled) {
        this.p = p;
        this.discordSrv = discordSrvEnabled;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent e) {
        if (discordSrv) {
            // makes reverse emoji lookup simpler
            e.message(
                    p.getEmoteManager().emojize(e.message())
            );
        } else {
            e.renderer(this);
        }
        //
    }

    @Override
    public @NotNull Component render(Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        if (!source.hasPermission("minemoji.emoji")) {
            return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, message, viewer);
        }
        return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, p.getEmoteManager().emojize(message), viewer);
    }
}
