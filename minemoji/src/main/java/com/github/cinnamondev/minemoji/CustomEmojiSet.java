package com.github.cinnamondev.minemoji;

import com.github.cinnamondev.common.EmojiSet;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;


public class CustomEmojiSet implements EmojiSet {

    public String prefix;
    public int packVersion;
    public URI url;
    public boolean serveToClient;
    public List<EmojiSet.SpriteMeta> emojis;

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
    public List<EmojiSet.SpriteMeta> allEmotes() {
        return emojis;
    }

    @Override
    public Optional<EmojiSet.SpriteMeta> tryFindByText(String emoteText) {
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
