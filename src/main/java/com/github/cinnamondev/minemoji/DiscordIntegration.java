package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;

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
    public void onMessageSent(DiscordGuildMessagePreBroadcastEvent e) {
        e.setMessage(toShaded(
                p.getEmoteManager().emojize(fromShaded(e.getMessage()))
        ));
        // This is janky,, we are stopping discordsrv from broadcasting so we can do it instead. prevents weirdness with
        // components.
        //e.setCancelled(true);
        //p.getServer().sendMessage(manager.emojize(fromShaded(e.getMinecraftMessage())));
    }
    ///  Text replacer that replaces prefixed emotes :identifier--sprite: with their corresponding emoji
    ///  (has to search rather than map lookup)
    //private final TextReplacementConfig PREFIXED_EMOTE_REPLACER = TextReplacementConfig.builder()
    //        .match(Pattern.compile(":[a-zA-Z0-9_]*--([a-zA-Z0-9_~]*):"))
    //        .replacement((result, b) -> Component.text(result.group(1)))
    //        .build();
    @Subscribe
    public void onMessageReceived(GameChatMessagePreProcessEvent e) {
        Component demojized = p.getEmoteManager().demojize(fromShaded(e.getMessageComponent()), true);
        //demojized = demojized.replaceText(PREFIXED_EMOTE_REPLACER); //  MAKE QUADRUPLE SURE!

        e.setMessageComponent(toShaded(demojized));
    }
}
