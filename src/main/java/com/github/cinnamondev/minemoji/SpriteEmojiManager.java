package com.github.cinnamondev.minemoji;

import com.google.gson.Gson;
import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpriteEmojiManager {
    private final Minemoji p;

    // default twemoji is backed by JEmoji;
    private boolean IS_BASE_EMOJIS_AVAILABLE = false;
    public boolean baseEmojisAvailable() { return IS_BASE_EMOJIS_AVAILABLE; }

    private final Map<Emoji,ObjectComponent> emojiMap;
    Map<String, EmojiSet> customPacks;
    BidiMap<String,Key> customSprites;

    public static CompletableFuture<SpriteEmojiManager> fromRemotePacks(Minemoji p, List<URI> packs) {
        p.getLogger().info("Creating emoji manager using remote packs");
        Gson gson = new Gson();
        try (HttpClient client = HttpClient.newHttpClient()) {
            List<CompletableFuture<EmojiSet>> futures = packs.stream()
                    .map(pack -> HttpRequest.newBuilder(pack).GET().build())
                    .map(request -> client
                            .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                            .thenApply(HttpResponse::body)
                            .thenApply(InputStreamReader::new)
                            .thenApply(stream -> gson.fromJson(stream, EmojiSet.class))
                    ).toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                    .thenApply(_v -> futures.stream().map(CompletableFuture::join))
                    // if fails exceptionally, go through the futures and remove the ones that mucked up.
                    .exceptionally(ex -> futures.stream().flatMap(f -> {
                        try {
                            return Stream.of(f.join());
                        } catch (CompletionException e) {
                            p.getLogger().warning("Malformed pack, ignoring... See message");
                            p.getLogger().warning(ex.getMessage());
                            return Stream.empty();
                        }
                    }))
                    .thenApply(s -> s.map(set -> Map.entry(set.prefix, set))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    ).thenApply(map -> new SpriteEmojiManager(p, map));
        }
    }

    public static SpriteEmojiManager fromLocalPacks(Minemoji p, File packsDirectory) {
        p.getLogger().info("Creating emoji manager using local packs");
        return new SpriteEmojiManager(p, discoverEmojiPacks(p, packsDirectory));
    }

    private static Map<String, EmojiSet> discoverEmojiPacks(Minemoji p, File packsDirectory) {
        HashMap<String, EmojiSet> map = new HashMap<>();
        Gson gson = new Gson();
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
                for (EmojiSet.SpriteMeta emoji : set.emojis) {
                    emoji.emojiText = emoji.emojiText.toLowerCase().strip();
                }
                map.put(set.prefix, set);
            } catch (FileNotFoundException e) { // we shouldnt generally EXPECT to end up here, but for now we just
                throw new RuntimeException(e);  // wrap it.
            }
        }
        return Collections.unmodifiableMap(map);
    }

    protected SpriteEmojiManager(Minemoji p, Map<String, EmojiSet> customPacks) {
        this.p = p;
        p.getLogger().info("Found following packs:" + String.join(", ", customPacks.keySet()));
        if (p.getConfig().getBoolean("unicode-emojis.enabled", true)) {
            this.emojiMap = createBaseEmojiSet();
            if (!emojiMap.isEmpty()) { IS_BASE_EMOJIS_AVAILABLE = true; }
        } else { this.emojiMap = Collections.emptyMap(); }

        this.customPacks = customPacks;
        this.customSprites = new DualHashBidiMap<>(customPacks.entrySet().stream()
                .flatMap(e ->
                        e.getValue().emojis.stream()
                                .map(meta -> Map.entry(
                                        meta.emojiText,
                                        Key.key(meta.resource)
                                ))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

    }

    public static ObjectComponent spriteMetaToComponent(EmojiSet.SpriteMeta meta) {
        return Component.object(ObjectContents.sprite(
                Key.key("paintings"),
                Key.key(meta.resource)
        ));
    }

    public Map<Emoji,ObjectComponent> getDefaultEmojiMap() { return Collections.unmodifiableMap(emojiMap); }
    public Map<String, EmojiSet> getCustomPacks() { return Collections.unmodifiableMap(customPacks); }

    public Optional<ObjectComponent> getEmojiFromActual(Emoji emoji) {
        return Optional.ofNullable(emojiMap.get(emoji));
    }

    public Optional<String> getCustomEmojiBySprite(Key sprite) {
        return Optional.ofNullable(customSprites.getKey(sprite));
    }
    public Optional<ObjectComponent> getCustomEmoji(String key) {
        return Optional.ofNullable(customSprites.get(key))
                .map(sprite -> Component.object(ObjectContents.sprite(
                        Key.key("paintings"),
                        sprite
                )));
    }
    public Map<Emoji, ObjectComponent> createBaseEmojiSet() {
        return EmojiManager.getAllEmojis().stream().map(e ->
                Map.entry(e, Component.object(ObjectContents.sprite(
                        Key.key("paintings"),
                        Key.key("unicode/" + StringUtils.joinWith("-", e.getHtmlHexadecimalCode().replace("&#x", "").split(";")).toLowerCase())
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
            .replacement((result, b) -> {
                if (result.group().trim().toLowerCase().split("--")[0].equalsIgnoreCase("unicode")) {
                    return EmojiManager.getEmoji(result.group())
                            .flatMap(emoji -> getEmojiFromActual(emoji).map(c -> (Component) c))
                            .orElse(Component.text(result.group()));
                } else {
                    return getNamespacedCustomEmoji(result.group().trim().toLowerCase()).map(c -> (Component) c)
                            .orElse(Component.text(result.group()));
                }

            }).build();

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

    protected Optional<Emoji> getEmojiBySprite(Key sprite) {
        // 1f0cf -> &#x1f0cf;
        // 1f1e6-1f1e8 -> &#x1f1e6;&#x1f1e8;
        var arr = sprite.value().split("/");
        String trimmedHexCode = arr[arr.length - 1];
        String hexCode = Arrays.stream(trimmedHexCode.split("-"))
                .map(str -> "&#x" + str)
                .collect(Collectors.joining(";")) + ";";
        return EmojiManager.getByHtmlHexadecimal(hexCode);
    }
    ///  doesnt touch the children. If its an ObjectComponent with a matching sprite key, it will be turned into a
    ///  TextComponent. if it doesnt have a matching key or otherwise, it is left UNTOUCHED!
    protected Component demojizeSingleComponent(Component component) {
        if (component instanceof ObjectComponent o && o.contents() instanceof SpriteObjectContents s) {
            return getCustomEmojiBySprite(s.sprite()).map(str -> (Component) Component.text(":" + str + ":"))
                    .orElse(getEmojiBySprite(s.sprite())
                            .map(e -> e.getDiscordAliases().getFirst())
                            .map(str -> (Component) Component.text(str))
                            .orElse(component) // just use the old one
                    );
        }
        return component;
    }

    public Component demojize(Component component) {
        var childs = component.children().stream().map(this::demojizeSingleComponent).collect(Collectors.toList());
        return demojizeSingleComponent(component).children(childs);
    }
    public Component emojize(Component component) {
        // try to replace Unicode text emotes
        return component.replaceText(UNICODE_REPLACER)
                .replaceText(PREFIXED_EMOTE_REPLACER)
                .replaceText(DEFAULT_EMOTE_REPLACER);
    }

    protected Optional<EmojiSet> getPackByPrefix(String prefix) {
        return Optional.ofNullable(customPacks.get(prefix));
    }
    public Optional<ObjectComponent> getNamespacedCustomEmoji(String identifier) {
        // search a pack for an emoji rather than pucking out of an alias map.
        // requires format :packName--emoji: !
        var _arr =identifier.split("--", 2);
        String packIdentifier = _arr[0];
        String spriteName = _arr[1];
        return getPackByPrefix(packIdentifier)
                .flatMap(set -> set.emojis.stream()
                        .filter(emote -> emote.emojiText.equalsIgnoreCase(spriteName))
                        .findFirst()
                         .map(SpriteEmojiManager::spriteMetaToComponent)
                );
    }
}
