package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;

public class DiscordIntegration {
    private final SpriteEmojiManager manager;
    private final Minemoji p;
    public DiscordIntegration(Minemoji p, SpriteEmojiManager emojiManager) {
        this.p = p;
        this.manager = emojiManager;
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
    public void onMessageSent(DiscordGuildMessagePostProcessEvent e) {
        //e.setMessage(toShaded(
        //        manager.emojize(fromShaded(e.getMessage()))
        //));
        // This is janky,, we are stopping discordsrv from broadcasting so we can do it instead. prevents weirdness with
        // components.
        e.setCancelled(true);
        p.getServer().sendMessage(manager.emojize(fromShaded(e.getMinecraftMessage())));
    }

    @Subscribe
    public void onMessageReceived(GameChatMessagePreProcessEvent e) {
        e.setMessageComponent(toShaded(
                manager.demojize(fromShaded(e.getMessageComponent()))
        ));
    }
}
