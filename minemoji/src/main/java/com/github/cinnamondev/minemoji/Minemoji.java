package com.github.cinnamondev.minemoji;

import com.github.cinnamondev.common.EmojiSet;
import com.github.cinnamondev.minemoji.Command.Command;
import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class Minemoji extends JavaPlugin {
    public static final URI DEFAULT_URI = URI.create("https://cinnamondev.github.io/minemoji/packs/twemoji-latest.zip");
    private final Command command = new Command(this);
    private RequestPacks packListener = null;
    private DiscordIntegration discordIntegration = null;
    private SpriteEmojiManager manager = null;
    private static final int BSTATS_PLUGIN_ID = 31205;
    private Metrics bStats;
    public Metrics getBStats() { return bStats; }
    public SpriteEmojiManager getEmoteManager() {
        return manager;
    }
    @Override
    public void onEnable() {
        this.bStats = new Metrics(this, BSTATS_PLUGIN_ID);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new EmojiRenderer(this), this);
        load().whenComplete((_v, ex) -> {
            if (ex == null) {
                if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
                    this.discordIntegration = new DiscordIntegration(this);
                    DiscordSRV.api.subscribe(this.discordIntegration);
                }
            } else {
                getLogger().severe("Couldn't load. You're on your own! o7");
                getLogger().severe(ex.getMessage());
            }
        });

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                commands.registrar().register(command.command())
        );


    }

    public CompletableFuture<SpriteEmojiManager> createManager() {
        CompletableFuture<Map<String, CustomEmojiSet>> customEmotes;

        ArrayList<URI> uris = new ArrayList<>();
        for (String s : getConfig().getStringList("custom-packs.packs")) {
            try {
                uris.add(new URI(s));
            } catch (URISyntaxException e) {
                getComponentLogger().warn("Malformed URI {} in custom packs list. Ignoring. ", s);
            }
        }
        customEmotes = SpriteEmojiManager.downloadPacks(this, uris)
                .thenApply(set -> {
                    HashMap<String, CustomEmojiSet> fullSet = new HashMap<>(set);
                    fullSet.putAll(SpriteEmojiManager.fromLocalFilesystem(this,
                            getDataPath().resolve("packs").toFile()
                    ));
                    return fullSet;
                });

        return customEmotes.thenApply(sets -> new SpriteEmojiManager(this, sets));
    }

    public CompletableFuture<Void> load() {
        CompletableFuture<SpriteEmojiManager> managerFuture = createManager().orTimeout(2, TimeUnit.MINUTES);
        CompletableFuture<RequestPacks> packListenerFuture = CompletableFuture.completedFuture(null);
        if (getConfig().getBoolean("serve-packs", true)) {
            packListenerFuture =
                    managerFuture.thenComposeAsync(manager ->
                            RequestPacks.requestPacks(
                                    this,
                                    manager.customEmoteMap.values().stream().map(s -> (EmojiSet) s).toList(),
                                    manager.unicodeEmojiSet
                            ));
        }

        CompletableFuture<RequestPacks> finalPackListenerFuture = packListenerFuture;
        return CompletableFuture.allOf(managerFuture, packListenerFuture)
                .thenAcceptAsync(_v -> {
                    try {
                        this.manager = managerFuture.get();
                        if (packListener != null) { // unregister before swapping if it was there.
                            packListener.unregister();
                        }
                        this.packListener = finalPackListenerFuture.get();
                        if (packListener != null) { // register it again, if present.
                            this.getServer().getPluginManager().registerEvents(packListener, this);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        getLogger().severe("Manager or pack listener couldn't be loaded; You're on your own.");
                        getLogger().throwing("minemoji", "blockingLoad", e);
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void onDisable() {
        if (discordIntegration != null) {
            DiscordSRV.api.unsubscribe(discordIntegration);
        }
        // Plugin shutdown logic
    }
}
