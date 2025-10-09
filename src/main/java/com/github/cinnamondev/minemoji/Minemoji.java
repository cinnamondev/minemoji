package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.DiscordSRV;
import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackInfoLike;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public final class Minemoji extends JavaPlugin {
    private static final URI DEFAULT_URI = URI.create("https://cinnamondev.github.io/minemoji/latest.zip");
    public DiscordIntegration discord = null;
    public SpriteEmojiManager emojiManager;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
        this.emojiManager = new SpriteEmojiManager(this);
        RequestPacks.requestPacks(emojiManager).thenAccept(listener -> getServer().getPluginManager()
                .registerEvents(listener, this));

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            discord = new DiscordIntegration(emojiManager);
            DiscordSRV.api.subscribe(discord);
        }
        getServer().getPluginManager().registerEvents(new EmojiRenderer(this), this);
    }

    public List<ResourcePackInfoLike> discoverResourcePacks() {
        //ResourcePackInfo.resourcePackInfo().
        //getConfig().getConfigurationSection("packs")
        return Collections.emptyList();
    }
    @Override
    public void onDisable() {
        DiscordSRV.api.unsubscribe(discord);
        // Plugin shutdown logic
    }
}
