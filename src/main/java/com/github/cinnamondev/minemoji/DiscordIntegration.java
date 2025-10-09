package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;
import net.kyori.adventure.text.Component;

public class DiscordIntegration {
    private final SpriteEmojiManager manager;
    public DiscordIntegration(SpriteEmojiManager emojiManager) {
        this.manager = emojiManager;
    }
    @Subscribe
    public void onMessageReceived(DiscordGuildMessagePostProcessEvent e) {
        e.setMinecraftMessage((github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component)
                manager.emojize((Component) e.getMessage())
        );
    }

    @Subscribe
    public void onMessageReceived(GameChatMessagePreProcessEvent e) {
        e.setMessageComponent((github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component)
                manager.demojize((Component) e.getMessageComponent())
        );
    }
}
