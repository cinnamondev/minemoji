package com.github.cinnamondev.minemoji;

import net.fellbaum.jemoji.Emoji;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.ObjectContents;
import org.apache.commons.collections4.BidiMap;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CustomEmojiSet implements EmojiSet {
    public String prefix;
    public int packVersion;
    public URI url;
    public boolean serveToClient;
    public List<SpriteMeta> emojis;

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public int packVersion() {
        return packVersion;
    }

    @Override
    public @Nullable URI uri() {
        return serveToClient ? url : null; // backwards compat.
    }

    @Override
    public List<SpriteMeta> allEmotes() {
        return emojis;
    }

    @Override
    public Optional<SpriteMeta> tryFindByText(String emoteText) {
        return emojis.stream()
                .filter(meta -> meta.emojiText.equalsIgnoreCase(emoteText))
                .findFirst();
    }

    @Override
    public Optional<SpriteMeta> tryFindByKey(Key key) {
        return emojis.stream()
                .filter(meta -> meta.key().value().equalsIgnoreCase(key.value()))
                .findFirst();
    }

}
