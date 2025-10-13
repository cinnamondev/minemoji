package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;

public final class Minemoji extends JavaPlugin {
    public static final URI DEFAULT_URI = URI.create("https://cinnamondev.github.io/minemoji/packs/twemoji-latest.zip");
    private static boolean DO_DISCORD_SRV = false;
    public DiscordIntegration discord = null;
    public SpriteEmojiManager emojiManager;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
        this.emojiManager = new SpriteEmojiManager(this);
        try {
            if (getConfig().getBoolean("serve-packs", true)) {
                RequestPacks.requestPacks(this, emojiManager).thenAccept(listener -> getServer().getPluginManager()
                        .registerEvents(listener, this));
            }
        } catch (URISyntaxException e) {
            getLogger().warning(e.getMessage());
            getLogger().warning("malformed uri for resource pack, there may be issues serving resource packs now.");
        }


        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            DO_DISCORD_SRV = true;
            discord = new DiscordIntegration(this,emojiManager);
            DiscordSRV.api.subscribe(discord);
        }
        getServer().getPluginManager().registerEvents(new EmojiRenderer(this), this);
    }

    @Override
    public void onDisable() {
        if (DO_DISCORD_SRV) {
            DiscordSRV.api.unsubscribe(discord);
        }
        // Plugin shutdown logic
    }
}
