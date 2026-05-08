package com.github.cinnamondev.minemoji;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.key.Key;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnicodeEmojiSet implements EmojiSet {
    private BidiMap<String, Emoji> emojiTable = new DualHashBidiMap<>(createEmojiMap());
    private final URI uri;

    public UnicodeEmojiSet(URI uri) {
        this.uri = uri;
    }

    private static Map<String,Emoji> createEmojiMap() {
        return EmojiManager.getAllEmojis().stream()
                .filter(emoji -> !emoji.getDiscordAliases().isEmpty())
                .collect(Collectors.toMap(e ->
                                StringUtils.joinWith("-", e.getHtmlHexadecimalCode().replace("&#x", "")
                                                .split(";"))
                                        .toLowerCase(),
                        Function.identity()
                ));
    }
    public SpriteMeta findByEmote(Emoji emoji) {
        String str = emojiTable.getKey(emoji);
        return new SpriteMeta(emoji.getDiscordAliases().getFirst().replace(":", ""), "unicode/" + str, false);
    }

    @Override
    public String prefix() {
        return "unicode";
    }

    @Override
    public int packVersion() {
        return -1;
    }

    @Override
    public @Nullable URI uri() {
        return uri;
    }

    @Override
    public List<SpriteMeta> allEmotes() {
        return emojiTable.values().stream().map(this::findByEmote).toList();
    }

    @Override
    public Optional<SpriteMeta> tryFindByText(String emoteText) {
        String aliasText = emoteText;
        if (!(aliasText.startsWith(":") && aliasText.endsWith(":"))) {
            aliasText = ":" + aliasText + ":";
        }
        return EmojiManager.getByDiscordAlias(aliasText)
                .map(this::findByEmote);
    }

    @Override
    public Optional<SpriteMeta> tryFindByKey(Key key) {
        var arr = key.value().splitWithDelimiters("/",2);
        if (arr.length != 3) { return Optional.empty(); }
        String family = arr[0];
        String hex = arr[2];

        if (!family.equalsIgnoreCase("unicode")) {
            return Optional.empty();
        }
        return Optional.ofNullable(emojiTable.get(hex)).map(this::findByEmote);
    }

}
