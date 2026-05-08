package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordIntegration {
    private final Minemoji p;
    public DiscordIntegration(Minemoji p) {
        this.p = p;
    }

    // HATE HATE!!!!

    private static Component fromShaded(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component discordComponent) {
        String jsonComponent = github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(discordComponent);
        return GsonComponentSerializer.gson().deserialize(jsonComponent);
    }

    private static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component toShaded(Component component) {
        String jsonComponent = GsonComponentSerializer.gson().serialize(component);
        return github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .deserialize(jsonComponent);
    }

    @Subscribe
    public void emojizeDiscordMessage(DiscordGuildMessagePostProcessEvent e) {
        if (!p.getConfig().getBoolean("discord.disable-override", false)) {
            p.getServer().sendMessage(
                    p.getEmoteManager().emojize(fromShaded(e.getMinecraftMessage()))
            );
            e.setCancelled(true);
        } else {
            e.setMinecraftMessage(toShaded(
                    p.getEmoteManager().emojize(fromShaded(e.getMinecraftMessage()))
            ));
        }
    }

    @Subscribe
    public void demojizeMinecraftMessage(GameChatMessagePostProcessEvent e) {
        Pattern pattern = Pattern.compile("\\[((\\w+)/([a-z0-9._-]+))" + Pattern.quote("@paintings]"));
        Matcher m = pattern.matcher(e.getProcessedMessage());

        String message = m.replaceAll(mr -> {
            Key key = Key.key(mr.group(1));
            String family = mr.group(2);
            String emote = mr.group(3);

            return p.getEmoteManager().findEmojiSet(family)
                    .flatMap(set -> set instanceof UnicodeEmojiSet ues
                            ? ues.tryFindByKey(key) // try to use "unicode/hjjh3433" or whatever processor.
                            : set.tryFindByText(emote) // try to find by bog standard means.
                    )
                    .map(EmojiSet.SpriteMeta::toDiscordString)
                    .orElse(mr.group());
        });
        e.setProcessedMessage(message);
    }
}
