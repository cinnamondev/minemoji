package com.github.cinnamondev.minemoji;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RequestPacks implements Listener {
    public static CompletableFuture<RequestPacks> requestPacks(Minemoji p, SpriteEmojiManager emojiManager) throws URISyntaxException {
        ArrayList<CompletableFuture<ResourcePackInfo>> futures =
                emojiManager.getCustomPacks().values().parallelStream()
                        .filter(pack -> pack.serveToClient)
                        .map(e -> ResourcePackInfo.resourcePackInfo()
                                .uri(e.url)
                                .id(UUID.nameUUIDFromBytes(e.prefix.getBytes()))
                                .computeHashAndBuild()
                        ).collect(Collectors.toCollection(ArrayList::new));

        if (p.getConfig().getBoolean("unicode-emojis.enabled", true)) {
            String defaultPack = p.getConfig().getString("unicode-emojis.uri");
            URI uri;
            if (defaultPack == null) { uri = Minemoji.DEFAULT_URI; } else { uri = new URI(defaultPack); }
            futures.add(
                    ResourcePackInfo.resourcePackInfo()
                            .uri(uri)
                            .id(UUID.nameUUIDFromBytes("unicode-emojis".getBytes()))
                            .computeHashAndBuild()
            );
        }

        if (futures.isEmpty()) {
            p.getComponentLogger().warn("No resource packs to serve.");
            return CompletableFuture.completedFuture(new RequestPacks(
                    p, Collections.emptyList()
            ));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(_v -> new RequestPacks(
                        p,
                        futures.stream()
                                .map(CompletableFuture::join)
                                .toList()
                ));
    }

    private final Minemoji p;
    private final List<ResourcePackInfo> packs;
    private final Component joinPrompt;
    protected RequestPacks(Minemoji p, List<ResourcePackInfo> packs) {
        this.p = p;
        this.packs = packs;
        p.getLogger().info("Additional resource packs to fetch:");
        packs.forEach(pack -> {
            p.getLogger().info(pack.uri().toString());
        });
        String miniMessage= p.getConfig().getString("join-prompt");
        if (miniMessage != null && !miniMessage.isEmpty()) {
            MiniMessage mm = MiniMessage.miniMessage();
            joinPrompt = mm.deserialize(miniMessage);
        } else {
            joinPrompt = null;
        }



    }

    private final HashMap<UUID, CountDownLatch> waitingConfigurationThreads = new HashMap<>();

    public void unregister() {
        AsyncPlayerConnectionConfigureEvent.getHandlerList().unregister(this);
        PlayerConnectionCloseEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void serveEmojiPacks(AsyncPlayerConnectionConfigureEvent e) throws InterruptedException {
        UUID uuid = e.getConnection().getProfile().getId();
        // latch is put in a map so it's accessible from other events too.
        CountDownLatch latch = waitingConfigurationThreads
                .computeIfAbsent(uuid, _u -> new CountDownLatch(packs.size()));

        AtomicBoolean accepted = new AtomicBoolean(true);
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packs)
                .replace(false)
                .required(p.getConfig().getBoolean("enforce-packs", false))
                .prompt(joinPrompt)
                .callback(ResourcePackCallback.onTerminal(
                        (packUuid, audience) -> { // success
                            waitingConfigurationThreads.computeIfPresent(uuid, (_uuid, l) -> {
                                l.countDown();
                                return l;
                            });
                        }, (packUuid, audience) -> {
                            waitingConfigurationThreads.computeIfPresent(uuid, (_uuid, l) -> {
                                l.countDown();
                                return l;
                            });
                            accepted.set(false);
                            audience.sendMessage(Component.text("We use resource packs to serve emojis! :("));
                        }
                )).build();

        e.getConnection().getAudience().sendResourcePacks(request);

        latch.await(30L, TimeUnit.SECONDS);
        if (!accepted.get() && p.getConfig().getBoolean("enforce-packs", false)) {
            e.getConnection().disconnect(Component.text("Resource pack is required!"));
        }
        //event.getConnection().getAudience()
        //        .sendResourcePacks()
    }

    @EventHandler
    public void onConnectionDrop(PlayerConnectionCloseEvent e) {
        CountDownLatch latch = waitingConfigurationThreads.get(e.getPlayerUniqueId());
        if (latch == null) { return; }

        while (latch.getCount() > 0) { // janky just drain the latch so we can drop this thread
            latch.countDown();
        }
        p.getServer().getScheduler()
                .runTaskLaterAsynchronously(p, () -> waitingConfigurationThreads.remove(e.getPlayerUniqueId()), 5L);
    }
}
