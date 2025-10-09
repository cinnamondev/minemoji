package com.github.cinnamondev.minemoji;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class EmojiRenderer implements Listener, io.papermc.paper.chat.ChatRenderer {
    private final Map<String, Component> codepointLookup = new HashMap<>();
    private final Map<String, Component> identfierLookup = new HashMap<>();
    //private final Pattern pattern;

    private final Minemoji p;
    public EmojiRenderer(Minemoji p) {
        this.p = p;
        //pattern = Pattern.compile(":(sob|):");
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        e.renderer(this);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        var newComponent = p.emojiManager.emojize(message);
        p.emojiManager.demojize(newComponent);
        return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, newComponent, viewer);
    }
}
