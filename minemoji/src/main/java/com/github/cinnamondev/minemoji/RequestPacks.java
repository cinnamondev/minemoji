package com.github.cinnamondev.minemoji;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.github.cinnamondev.common.EmojiSet;
import com.github.cinnamondev.common.UnicodeEmojiSet;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RequestPacks implements Listener {
    public static CompletableFuture<RequestPacks> requestPacks(Minemoji p, @Nullable List<EmojiSet> emoteSets, @Nullable UnicodeEmojiSet unicodeEmojiSet) {
        ArrayList<CompletableFuture<ResourcePackInfo>> futures;
        if (emoteSets != null) {
            futures = emoteSets.parallelStream()
                    .filter(set -> set.uri() != null)
                    .map(set -> ResourcePackInfo.resourcePackInfo()
                            .uri(set.uri())
                            .id(UUID.nameUUIDFromBytes(set.prefix().getBytes()))
                            .computeHashAndBuild() // TODO: we should compute our own hash so we can change the url to serve (for redirects)
                    ).collect(Collectors.toCollection(ArrayList::new));
        } else {
            futures = new ArrayList<>();
        }

        if (unicodeEmojiSet != null && unicodeEmojiSet.uri() != null) {
            futures.add(
                    ResourcePackInfo.resourcePackInfo()
                            .uri(unicodeEmojiSet.uri())
                            .id(UUID.nameUUIDFromBytes("unicode-emojis".getBytes()))
                            .computeHashAndBuild()
            );
        }

        if (futures.isEmpty()) {
            p.getComponentLogger().warn("No resource packs to serve.");
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApplyAsync(_v -> new RequestPacks(
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
