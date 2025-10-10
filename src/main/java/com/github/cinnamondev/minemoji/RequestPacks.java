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
import java.util.stream.Collectors;

public class RequestPacks implements Listener {
    public static CompletableFuture<RequestPacks> requestPacks(Minemoji p, SpriteEmojiManager emojiManager) throws URISyntaxException {
        // ew bwaah
        CompletableFuture<ResourcePackInfo> future = new CompletableFuture<>();

        ArrayList<CompletableFuture<ResourcePackInfo>> futures =
                emojiManager.customPacks.values().parallelStream()
                        .map(e -> ResourcePackInfo.resourcePackInfo()
                                .uri(e.url)
                                .id(UUID.fromString(e.prefix))
                                .computeHashAndBuild()
                        ).collect(Collectors.toCollection(ArrayList::new));

        if (p.getConfig().getBoolean("unicode-emojis.enabled", true)) {
            String defaultPack = p.getConfig().getString("unicode-emojis.uri");
            if (defaultPack != null && !defaultPack.isEmpty()) {
                futures.add(
                        ResourcePackInfo.resourcePackInfo()
                                .uri(new URI(defaultPack))
                                .id(UUID.fromString("unicode-emojis"))
                                .computeHashAndBuild()
                );
            }
        }

        if (futures.isEmpty()) {
            p.getLogger().warning("No resource packs to serve.");
            return CompletableFuture.completedFuture(new RequestPacks(
                    p, emojiManager,
                    Collections.emptyList()
            ));
        }
        return CompletableFuture.allOf((CompletableFuture<ResourcePackInfo>[]) futures.toArray())
                .thenApply(_v -> new RequestPacks(
                        p, emojiManager,
                        futures.stream()
                                .map(CompletableFuture::join)
                                .toList()
                ));
    }

    private final ResourcePackRequest request;
    private final Minemoji p;
    protected RequestPacks(Minemoji p, SpriteEmojiManager emojiManager, List<ResourcePackInfo> packs) {
        this.p = p;
        Component joinPrompt; {
            String miniMessage= p.getConfig().getString("join-prompt");
            if (miniMessage != null && !miniMessage.isEmpty()) {
                MiniMessage mm = MiniMessage.miniMessage();
                joinPrompt = mm.deserialize(miniMessage);
            } else {
                joinPrompt = null;
            }
        }

        request = ResourcePackRequest.resourcePackRequest()
                .packs(packs)
                .replace(false)
                .required(p.getConfig().getBoolean("enforce-packs", false))
                .prompt(joinPrompt)
                .callback(ResourcePackCallback.onTerminal(
                        (uuid, audience) -> { // success
                            waitingConfigurationThreads.computeIfPresent(uuid, (_uuid, latch) -> {
                                latch.countDown();
                                return latch;
                            });
                            p.getLogger().info("Successfully sent resource packs to " + uuid.toString());
                        }, (uuid, audience) -> {
                            waitingConfigurationThreads.computeIfPresent(uuid, (_uuid, latch) -> {
                                latch.countDown();
                                return latch;
                            });
                            audience.sendMessage(Component.text("We use resource packs to serve emojis too! :("));
                        }
                )).build();

    }

    private final HashMap<UUID, CountDownLatch> waitingConfigurationThreads = new HashMap<>();

    @EventHandler
    public void serveEmojiPacks(AsyncPlayerConnectionConfigureEvent e) throws InterruptedException {
        CountDownLatch latch = waitingConfigurationThreads
                .computeIfAbsent(e.getConnection().getProfile().getId(), _u -> new CountDownLatch(1));

        e.getConnection().getAudience().sendResourcePacks(request);

        latch.await(15L, TimeUnit.SECONDS);
        //event.getConnection().getAudience()
        //        .sendResourcePacks()
    }

    public void onConnectionDrop(PlayerConnectionCloseEvent e) {
        CountDownLatch latch = waitingConfigurationThreads.get(e.getPlayerUniqueId());
        if (latch == null) { return; }
        latch.countDown();
        p.getServer().getScheduler()
                .runTaskLaterAsynchronously(p, () -> waitingConfigurationThreads.remove(e.getPlayerUniqueId()), 5L);
    }
}
