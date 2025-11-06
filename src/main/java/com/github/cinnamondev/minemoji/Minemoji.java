package com.github.cinnamondev.minemoji;

import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class Minemoji extends JavaPlugin {
    public static final URI DEFAULT_URI = URI.create("https://cinnamondev.github.io/minemoji/packs/twemoji-latest.zip");
    private Command command;
    public DiscordIntegration discord = null;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
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
                    getComponentLogger().warn("Malformed URI " + s + " in custom packs list.");
                }
            }
            managerFuture = SpriteEmojiManager.fromRemotePacks(this, uris);
        }

        managerFuture.thenAccept(m -> {})
                .exceptionally(ex -> {
                    getLogger().warning("Couldn't make sprite manager, plugin will be dud!\n" + ex.getMessage());
                    return null;
                });

        if (getConfig().getBoolean("serve-packs", true)) {
            managerFuture.thenComposeAsync(manager -> {
                        try {
                            return RequestPacks.requestPacks(this, manager);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .thenAccept(listener -> getServer().getPluginManager().registerEvents(listener, this))
                    .exceptionally(ex -> {
                        getComponentLogger().warn("Couldn't make pack listener, see:", ex);
                        return null;
                    });
        }

        managerFuture.thenApply(m -> new EmojiRenderer(m, getServer().getPluginManager().isPluginEnabled("DiscordSRV")))
                .thenAccept(listener -> getServer().getPluginManager().registerEvents(listener, this))
                .exceptionally(ex -> {
                    getComponentLogger().warn("Couldn't attach emoji chat renderer, see:",ex);
                    return null;
                });

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            managerFuture.thenApply(manager -> new DiscordIntegration(this, manager))
                    .thenAccept(integration -> {
                        this.discord = integration;
                        DiscordSRV.api.subscribe(integration);
                    })
                    .exceptionally(ex -> {
                        getComponentLogger().warn("Couldn't start DiscordSRV integration: ", ex);
                        return null;
                    });
        }

        this.command = new Command(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                commands.registrar().register(command.command())
        );

        managerFuture
                .thenAccept(manager -> {
                    command.registerCustomPacks(manager.getCustomPacks());
                    command.registerDefaultPack(manager.getDefaultEmojiMap());
                })
                .exceptionally(ex -> {
                    getComponentLogger().warn("Couldn't register packs to command... ", ex);
                    return null;
                });
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            DiscordSRV.api.unsubscribe(discord);
        }
        // Plugin shutdown logic
    }
}
