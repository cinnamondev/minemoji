package com.github.cinnamondev.minemoji;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class EmojiRenderer implements Listener, io.papermc.paper.chat.ChatRenderer {
    private final SpriteEmojiManager manager;
    public EmojiRenderer(SpriteEmojiManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        e.renderer(this);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        if (!source.hasPermission("minemoji.emoji")) {
            return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, message, viewer);
        }
        return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, manager.emojize(message), viewer);
    }
}
