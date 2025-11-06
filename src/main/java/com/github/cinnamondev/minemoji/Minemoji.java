package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class Minemoji extends JavaPlugin {
    public static final URI DEFAULT_URI = URI.create("https://cinnamondev.github.io/minemoji/packs/twemoji-latest.zip");
    private Command command;
    public DiscordIntegration discord = null;
    private RequestPacks packListener = null;
    private EmojiRenderer renderer = null;
    private DiscordIntegration discordIntegration = null;
    private SpriteEmojiManager manager = null;
    public SpriteEmojiManager getEmoteManager() {
        return manager;
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.command = new Command(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                commands.registrar().register(command.command())
        );

        this.renderer = new EmojiRenderer(this, getServer().getPluginManager().isPluginEnabled("DiscordSRV"));
        getServer().getPluginManager().registerEvents(renderer, this);
        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            this.discordIntegration = new DiscordIntegration(this);
            DiscordSRV.api.subscribe(this.discordIntegration);
        }

        blockingLoad();

    }

    public CompletableFuture<SpriteEmojiManager> loadManager() {
        this.manager = null; // reloads will always be dangerous because we dont check if something is USING manager right then.

        if (packListener != null) { packListener.unregister(); }

        CompletableFuture<SpriteEmojiManager> managerFuture;
        if (!getConfig().getBoolean("custom-packs.download", false)) {
            managerFuture = CompletableFuture.completedFuture(
                    SpriteEmojiManager.fromLocalPacks(
                            this,
                            getDataPath().resolve("packs").toFile()
                    )
            );
        } else {
            ArrayList<URI> uris = new ArrayList<>();
            for (String s : getConfig().getStringList("custom-packs.packs")) {
                try {
                    uris.add(new URI(s));
                } catch (URISyntaxException e) {
                    getComponentLogger().warn("Malformed URI {} in custom packs list.", s);
                }
            }
            managerFuture = SpriteEmojiManager.fromRemotePacks(this, uris);
        }

        return managerFuture
                .thenApply(m -> {
                    this.manager = m;
                    command.registerCustomPacks(manager.getCustomPacks());
                    command.registerDefaultPack(manager.getDefaultEmojiMap());
                    return m;
                });
    }

    public void blockingLoad() {
        try {
            var managerFuture = loadManager().orTimeout(2, TimeUnit.MINUTES);
            CompletableFuture<Void> packListenerFuture;
            if (getConfig().getBoolean("serve-packs", true)) {
                packListenerFuture = managerFuture.thenComposeAsync(manager -> {
                    try {
                        return RequestPacks.requestPacks(this, manager);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }).thenAccept(listener -> {
                    this.packListener = listener;
                    getServer().getPluginManager().registerEvents(listener, this);
                });
            } else {
                packListenerFuture = CompletableFuture.completedFuture(null);
            }

            CompletableFuture.allOf(packListenerFuture, managerFuture).join();
        } catch (CompletionException e) {
            getLogger().severe("Manager or pack listener couldn't be loaded; You're on your own.");
            getLogger().throwing("minemoji", "onenable", e);
            throw e;
        }
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            DiscordSRV.api.unsubscribe(discord);
        }
        // Plugin shutdown logic
    }
}
