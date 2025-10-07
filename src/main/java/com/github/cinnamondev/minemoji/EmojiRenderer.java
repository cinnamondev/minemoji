package com.github.cinnamondev.minemoji;

import com.google.common.base.Joiner;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        var newComponent = p.emojiManager.replaceAllPossibleEmotesInComponent(message);
        return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, newComponent, viewer);
    }
    //@Override
    //public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
    //    if (!(message instanceof TextComponent)) {
    //        p.getLogger().warning("Emoji renderer only supports TextComponent");
    //        return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, message, viewer);
    //    }
    //    TextComponent newMessage = Component.empty();
    //    Pattern pattern = Pattern.compile(":([a-zA-Z0-9_]*):");
    //    String contents = ((TextComponent) message).content();
    //    int textStartPoint = 0;
    //    var results = pattern.matcher(((TextComponent) message).content()).results().toList();
    //    if (results.isEmpty()) { newMessage = (TextComponent) message; }
    //    for (var result : results) {
    //        Component remainingText = Component.text(contents.substring(textStartPoint, result.start()));
    //        Component wholeText = Component.text(contents.substring(textStartPoint, result.start()));
//
    //        var oEmoji = EmojiManager.getByAlias(result.group(1));
    //        if (oEmoji.isPresent()) {
    //            newMessage = newMessage.append(
    //                    EmojiManager.getByAlias(result.group(1))
    //                            .map(List::getFirst)
    //                            .map(Emoji::getEmoji)
    //                            .map(Component::text)
    //                            .map(remainingText::append)
    //                            .orElse(wholeText)
    //            );
    //        } else { // test for custom emojis
    //            newMessage = newMessage.append(wholeText);
    //        }
//
    //        textStartPoint = result.end();
    //    }
    //    newMessage = newMessage.append(Component.text(contents.substring(textStartPoint)));
//
    //    return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, newMessage, viewer);
    //}
}
