package com.github.cinnamondev.minemoji;

import com.google.gson.Gson;
import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.object.ObjectContents;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpriteEmojiManager {
    private Minemoji p;

    // default twemoji is backed by JEmoji;
    Map<Emoji,ObjectComponent> emojiMap;
    Map<String, EmojiSet> customPacks;
    Map<String,ObjectComponent> customSprites;

    public SpriteEmojiManager(Minemoji p) {
        this.p = p;
        this.emojiMap = createBaseEmojiSet();

        this.customPacks = discoverEmojiPacks();
        this.customSprites = customPacks.entrySet().stream()
                .flatMap(e ->
                        Arrays.stream(e.getValue().emojis)
                                .map(meta -> Map.entry(
                                        meta.emojiText,
                                        meta.toComponent(e.getValue())
                                ))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.customSprites.entrySet().forEach(e -> {
            p.getLogger().info(e.getKey());
            p.getLogger().info(e.getValue().toString());

        });
    }

    public class EmojiSet {
        public String prefix;
        public int packVersion;
        public SpriteMeta[] emojis;
    }
    public class SpriteMeta {
        public String emojiText;
        public String resource;
        //public String[] aliases;

        public ObjectComponent toComponent(EmojiSet context) {
            return Component.object(ObjectContents.sprite(
                    Key.key("paintings"),
                    Key.key(resource)
            ));
        }
    }
    public Optional<ObjectComponent> getEmojiFromActual(Emoji emoji) {
        return Optional.ofNullable(emojiMap.get(emoji));
    }

    public Optional<ObjectComponent> getCustomEmoji(String key) {
        customSprites.keySet().forEach(customEmoji -> p.getLogger().info(customEmoji));
        p.getLogger().info(customSprites.get(key).toString());
        return Optional.ofNullable(customSprites.get(key));
    }
    public Map<Emoji, ObjectComponent> createBaseEmojiSet() {
        return EmojiManager.getAllEmojis().stream().map(e ->
                Map.entry(e, Component.object(ObjectContents.sprite(
                        Key.key("paintings"),
                        Key.key("twemoji/" + StringUtils.joinWith("-", e.getHtmlHexadecimalCode().replace("&#x", "").split(";")).toLowerCase())
                )))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /// Text replacer that replaces unicode sequences with the corresponding emoji sprite
    private final TextReplacementConfig UNICODE_REPLACER = TextReplacementConfig.builder()
            .match(EmojiManager.getEmojiPattern())
            .replacement((result, b) -> {
                String match = result.group();
                return EmojiManager.getEmoji(match)
                        .flatMap(emoji -> getEmojiFromActual(emoji).map(c -> (Component) c))
                        .orElse(Component.text(match));
            })
            .build();

    ///  Text replacer that replaces prefixed emotes :identifier--sprite: with their corresponding emoji
    ///  (has to search rather than map lookup)
    private final TextReplacementConfig PREFIXED_EMOTE_REPLACER = TextReplacementConfig.builder()
            .match(Pattern.compile(":([a-zA-Z0-9_]*--[a-zA-Z0-9_]*):"))
            .replacement((result, b) ->
                    getNamespacedCustomEmoji(result.group().trim().toLowerCase()).map(c -> (Component) c)
                        .orElse(Component.text(result.group()))
            ).build();

    ///  Text replacer that replaces custom or default emotes according to their name/alias.
    ///  As per how the alias and sprite lookups are, if there is a conflict of names, whichever came in last
    ///  gets the spot. Default emotes should override all emotes.
    private final TextReplacementConfig DEFAULT_EMOTE_REPLACER = TextReplacementConfig.builder()
            .match(":([a-zA-Z0-9_]*):")
            .replacement((result, b) -> {
                String match = result.group(1).toLowerCase().trim();
                return getCustomEmoji(match).map(c -> (Component) c)
                        .orElse(EmojiManager.getByDiscordAlias(result.group()) // try to get emoji by alias
                                .flatMap(this::getEmojiFromActual)
                                .map(c -> (Component) c)
                                .orElse(Component.text(result.group()))
                        );
            })
            .build();
    public Component replaceAllPossibleEmotesInComponent(Component component) {
        // try to replace Unicode text emotes
        return component.replaceText(UNICODE_REPLACER)
                .replaceText(PREFIXED_EMOTE_REPLACER)
                .replaceText(DEFAULT_EMOTE_REPLACER);
    }
    public Optional<EmojiSet> getPackByPrefix(String prefix) {
        return Optional.ofNullable(customPacks.get(prefix));
    }
    public Optional<ObjectComponent> getNamespacedCustomEmoji(String identifier) {
        // search a pack for an emoji rather than pucking out of an alias map.
        // requires format :packName--emoji: !
        var _arr =identifier.split("--", 2);
        String packIdentifier = _arr[0];
        String spriteName = _arr[1];
        return getPackByPrefix(packIdentifier)
                .flatMap(set -> Arrays.stream(set.emojis)
                        .filter(emote -> emote.emojiText.equalsIgnoreCase(spriteName))
                        .findFirst()
                         .map(meta -> meta.toComponent(set))
                );
    }
    public Map<String, EmojiSet> discoverEmojiPacks() {
        HashMap<String, EmojiSet> map = new HashMap<>();
        Gson gson = new Gson();
        File packsDirectory = p.getDataPath().resolve("packs/").toFile();
        if (!packsDirectory.exists()) {
            packsDirectory.mkdirs();
            p.getLogger().info("Creating packs directory (no packs available): " + packsDirectory.getAbsolutePath());
            return Collections.emptyMap();
        }

        File[] files = packsDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            p.getLogger().info("no files in packs directory matching filter(?)");
            return Collections.emptyMap();
        }

        for (File pack : files) {
            try {
                FileReader reader = new FileReader(pack);
                EmojiSet set = gson.fromJson(reader, EmojiSet.class);
                for (SpriteMeta emoji : set.emojis) {
                    emoji.emojiText = emoji.emojiText.toLowerCase().strip();
                }
                map.put(set.prefix, set);
            } catch (FileNotFoundException e) { // we shouldnt generally EXPECT to end up here, but for now we just
                throw new RuntimeException(e);  // wrap it.
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
