package com.github.cinnamondev.minemoji;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


public class EmojiRenderer implements Listener, ChatRenderer {//, io.papermc.paper.chat.ChatRenderer {
    private final Minemoji p;
    public EmojiRenderer(Minemoji p) { this.p = p; }

    public void onChat(AsyncChatEvent e) {
        if (e.getPlayer().hasPermission("minemoji.emoji")) {
            e.renderer(this);

            //e.message(em.emojize(e.message()));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorChat(AsyncChatEvent e) {
        p.getLogger().info("monitor" + e.message().toString());
        onChat(e);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        SpriteEmojiManager em = p.getEmoteManager();
        if (em == null) { return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, message, viewer); }

        return ChatRenderer.defaultRenderer().render(
                source,
                sourceDisplayName,
                em.emojize(message),
                viewer
        );
    }
}
