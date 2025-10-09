package com.github.cinnamondev.minemoji;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RequestPacks implements Listener {
    public static CompletableFuture<RequestPacks> requestPacks(SpriteEmojiManager emojiManager) {
        // ew bwaah

        CompletableFuture<ResourcePackInfo>[] futures = (CompletableFuture<ResourcePackInfo>[])
                emojiManager.customPacks.values().parallelStream()
                        .map(e -> ResourcePackInfo.resourcePackInfo()
                                .uri(e.url)
                                .id(UUID.fromString(e.prefix))
                                .computeHashAndBuild()
                        ).toArray();

        return CompletableFuture.allOf(futures).thenApply(_v -> new RequestPacks(
                emojiManager,
                Arrays.stream(futures)
                        .map(CompletableFuture::join)
                        .toList()
        ));
    }

    private ResourcePackRequest request;
    protected RequestPacks(SpriteEmojiManager emojiManager, List<ResourcePackInfo> packs) {
        request = ResourcePackRequest.resourcePackRequest()
                .packs(packs)
                .replace(false)
                .required(false)
                .build();
    }

    @EventHandler
    public void onConfigurationPhase(AsyncPlayerConnectionConfigureEvent event) {
        event.getConnection().getAudience().sendResourcePacks(request);
        //event.getConnection().getAudience()
        //        .sendResourcePacks()
    }
}
