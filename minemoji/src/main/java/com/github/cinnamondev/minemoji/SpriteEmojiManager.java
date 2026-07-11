package com.github.cinnamondev.minemoji;

import com.github.cinnamondev.common.EmojiSet;
import com.github.cinnamondev.common.UnicodeEmojiSet;
import com.google.gson.Gson;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import net.fellbaum.jemoji.EmojiManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;

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
    private static final String DEFAULT_UNICODE_PACK = "https://itscinnamon.dev/minemoji/packs/twemoji-latest.zip";
    private Minemoji p = null;
    private boolean scanChildrenAsWholes = false;
    public UnicodeEmojiSet unicodeEmojiSet = null;

    public static CompletableFuture<Map<String, CustomEmojiSet>> downloadPacks(Minemoji p, List<URI> packs) {
        Gson gson = new Gson();
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            List<CompletableFuture<CustomEmojiSet>> futures = packs.stream()
                    .peek(uri -> p.getLogger().info("Remote Pack:" + uri))
                    .map(pack -> HttpRequest.newBuilder(pack).GET().build())
                    .map(request -> client
                            .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                            .thenApply(HttpResponse::body)
                            .thenApply(InputStreamReader::new)
                            .thenApply(stream -> gson.fromJson(stream, CustomEmojiSet.class))
                    ).toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                    .thenApply(_v -> futures.stream().map(CompletableFuture::join))
                    // if fails exceptionally, go through the futures and remove the ones that mucked up.
                    .exceptionally(ex -> futures.stream().flatMap(f -> {
                        try {
                            return Stream.of(f.join());
                        } catch (CompletionException e) {
                            p.getLogger().warning("Malformed pack ignoring... (download) See message");
                            p.getLogger().warning(ex.getMessage());
                            return Stream.empty();
                        }
                    }))
                    .thenApply(s -> s.map(set -> Map.entry(set.prefix, set))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }
    }

    public static Map<String, CustomEmojiSet> fromLocalFilesystem(Minemoji p, File jsonDirectory) {
        HashMap<String, CustomEmojiSet> emoteSet = new HashMap<>();
        Gson gson = new Gson();

        if (!jsonDirectory.exists()) {
            jsonDirectory.mkdirs();
            p.getLogger().warning("No pack directory was created, I've created one for you at " + jsonDirectory.getAbsolutePath());
            return Collections.emptyMap();
        }
        if (!jsonDirectory.isDirectory()) {
            p.getLogger().severe("Pack directory was not a directory? Dropping out!");
        }

        File[] files = jsonDirectory.listFiles((dir,name) -> name.endsWith(".json"));
        for (File pack : files) {
            try {
                FileReader reader = new FileReader(pack);
                try {
                    CustomEmojiSet set = gson.fromJson(reader, CustomEmojiSet.class);
                    for (CustomEmojiSet.SpriteMeta emoji : set.emojis) {
                        emoji.emojiText = emoji.emojiText.toLowerCase().strip();
                    }
                    emoteSet.put(set.prefix, set);
                } catch (Exception e) {
                    p.getLogger().severe("Pack parsing error for pack " + pack.getName());
                    p.getLogger().severe(e.getMessage());
                }

            } catch (FileNotFoundException e) { // we shouldnt generally EXPECT to end up here, but for now we just
                throw new RuntimeException(e);  // wrap it.
            }
        }
        return emoteSet;
    }

    public SpriteEmojiManager(Minemoji p, Map<String, CustomEmojiSet> customEmotes) {
        this.p = p;
        scanChildrenAsWholes = p.getConfig().getBoolean("check-colored-chat-messages", false);
        p.getBStats().addCustomChart(new DrilldownPie("n_custom_packs", () -> {
            Map<String, Map<String, Integer>> data = new HashMap<>();
            int vagueRange = (customEmotes.size() / 3);
            int vagueRangeUpper = vagueRange + 3;
            data.put(vagueRange + "-" + vagueRangeUpper, Map.of(Integer.toString(customEmotes.size()),1));
            return data;
        }));
        this.customEmoteMap.putAll(customEmotes);
        if (p.getConfig().getBoolean("unicode-emojis.enabled", true)) {
            String uriString = p.getConfig().getString("unicode-emojis.uri");
            if (uriString != null) {
                try {
                    URI packURI = URI.create(uriString);
                    unicodeEmojiSet = new UnicodeEmojiSet(packURI);
                    p.getBStats().addCustomChart(new SimplePie("used_twemoji_pack",
                            () -> uriString.equalsIgnoreCase(DEFAULT_UNICODE_PACK) ? "Default Twemoji" : uriString)
                    );
                } catch (IllegalArgumentException e) {
                    p.getLogger().info("Invalid URI. Unicode will not be created.");
                }
            } else {
                p.getLogger().info("No uri provided for Unicode. Your emotes should be bundled in another resource pack!");
                p.getBStats().addCustomChart(new SimplePie("used_twemoji_pack", () -> "Unknown"));
                unicodeEmojiSet = new UnicodeEmojiSet(null);
            }
        } else {
            p.getBStats().addCustomChart(new SimplePie("used_twemoji_pack", () -> "Disabled"));
        }
    }

    public final HashMap<String, CustomEmojiSet> customEmoteMap = new HashMap<>();
    public Optional<EmojiSet> findEmojiSet(String family) {
        if (family.equalsIgnoreCase("unicode")) { return Optional.ofNullable(unicodeEmojiSet); }
        return Optional.ofNullable(customEmoteMap.get(family));
    }
    public Optional<EmojiSet> findEmojiSet(Key emoteKey) {
        return findEmojiSet(StringUtils.substringBefore(emoteKey.value(), '/'));
    }
    public Optional<EmojiSet.SpriteMeta> findEmoji(Key emoteKey) {
        return findEmojiSet(emoteKey).flatMap(set -> set.tryFindByKey(emoteKey));
    }

    /// Text replacer that replaces unicode sequences with the corresponding emoji sprite
    private final TextReplacementConfig UNICODE_REPLACER = TextReplacementConfig.builder()
            .match(EmojiManager.getEmojiPattern())
            .replacement((result, b) -> {
                String match = result.group();
                return EmojiManager.getEmoji(match)
                        .map(e -> (Component) unicodeEmojiSet.findByEmote(e).toObjectComponent())
                        .orElse(Component.text(match));
            })
            .build();

    ///  Text replacer that replaces prefixed emotes :identifier--sprite: with their corresponding emoji
    ///  (has to search rather than map lookup)
    private final TextReplacementConfig PREFIXED_EMOTE_REPLACER = TextReplacementConfig.builder()
            .match(Pattern.compile(":([a-zA-Z0-9_]+)--([a-zA-Z0-9_~-]+):"))
            .replacement((result, b) ->
                    findEmojiSet(result.group(1).trim())
                            .flatMap(set -> set.tryFindByText(result.group(2)).map(meta -> (Component) meta.toObjectComponent()))
                            .orElse(Component.text(result.group()))
            ).build();

    ///  Text replacer that replaces custom emotes.
    ///  As per how the alias and sprite lookups are, if there is a conflict of names, whichever came in last
    ///  gets the spot. Default emotes should override all emotes.
    private final TextReplacementConfig DEFAULT_EMOTE_REPLACER = TextReplacementConfig.builder()
            .match(":([a-zA-Z0-9_~-]+):")
            .replacement((result, b) -> customEmoteMap.values().stream()
                    .flatMap(set -> set.tryFindByText(result.group(1).toLowerCase().trim())
                            .map(EmojiSet.SpriteMeta::toObjectComponent)
                            .map(c -> (Component) c)
                            .stream()
                    ).findFirst()
                    .or(() -> EmojiManager.getByDiscordAlias(":" + result.group(1).toLowerCase().trim() +":")
                                    .flatMap(e -> unicodeEmojiSet != null
                                            ? Optional.of(unicodeEmojiSet.findByEmote(e))
                                            : Optional.empty()
                                    ).map(EmojiSet.SpriteMeta::toObjectComponent)
                    )
                    .orElse(Component.text(result.group()))
            ).build();

    public Component walkComponent(Component component) {
        Component root = component;
        List<Component> children = component.children();
        ArrayList<Component> processedChildren = new ArrayList<>();

        boolean foundStart = false;
        ArrayList<TextComponent> colonWindow = new ArrayList<>(); // first and final element will be : with text between.
        for (Component child : children) {
            if (!(child instanceof TextComponent tc)) {
                processedChildren.add(child);
                continue;
            }
            String content = tc.content(); // content May NOT be one character, we need to explore this first.


            int index = content.indexOf(':');
            if (index < 0) { // no : is found!
                if (!foundStart) {
                    // havent found start of an emote, dump back into processed.
                    processedChildren.add(tc);
                } else {
                    colonWindow.add(tc); // have found part of emote already, just dump into window while we open the
                    // screen.
                }
            } else { // we have found : in it.
                Style existingStyle = tc.style();
                if (foundStart) {
                    // we already found the start before, so... this is the end bit
                    TextComponent partialComponent = Component.text(content.substring(0, index + 1)).style(existingStyle);
                    colonWindow.add(partialComponent);
                    Component unprocessed = flattenColouredComponents(colonWindow);
                    Component potentialEmote = emojize(unprocessed);
                    if (potentialEmote instanceof ObjectComponent oc) {
                        processedChildren.add(oc);
                    } else {
                        processedChildren.add(tc);
                    }
                    foundStart = false;
                    colonWindow = new ArrayList<>();
                    // now for the other part.
                    String partial = content.substring(index + 1);
                    if (!partial.isEmpty()) {
                        processedChildren.add(Component.text(partial).style(existingStyle));
                    }
                } else {
                    if (index != 0) {
                        TextComponent partialComponent = Component.text(content.substring(0, index)).style(existingStyle);
                        processedChildren.add(partialComponent);
                    }

                    int secondColonCheck = content.indexOf(':', index + 1);
                    if (secondColonCheck != -1 && secondColonCheck != index) {
                        processedChildren.add(tc);
                        // we know that this will be dealt with by the regular checkers as its own component. avoid
                        // double work where we can (even though this hack DOES cause double work. GRAH.)
                    } else {
                        TextComponent partialComponent = Component.text(content.substring(index)).style(existingStyle);
                        colonWindow.add(partialComponent);
                        foundStart = true;
                    }
                }
            }
        }

        if (foundStart) {
            processedChildren.addAll(colonWindow); // at this point we have gone through all and never reset foundstart
            // so theres no emote. give up here.
        }
        return root.children(processedChildren);
    }

    private Component flattenColouredComponents(ArrayList<TextComponent> tcList) {
        String content = tcList.stream().map(TextComponent::content).collect(Collectors.joining());
        return Component.text(content);
    }

    public Component emojize(Component component) {
        Component prescanned = scanChildrenAsWholes
                ? walkComponent(component)
                : component;

        Component firstStage = unicodeEmojiSet != null
                ? prescanned.replaceText(UNICODE_REPLACER)
                : prescanned;

        return firstStage
                .replaceText(PREFIXED_EMOTE_REPLACER)
                .replaceText(DEFAULT_EMOTE_REPLACER);
    }


    public Component demojize(Component component) {
        var children = component.children().stream()
                .map(this::demojizeSingleComponent)
                .toList();

        return demojizeSingleComponent(component).children(children);
    }

    Component demojizeSingleComponent(Component component) {
        if (!(component instanceof ObjectComponent oc && oc.contents() instanceof SpriteObjectContents soc)) {
            return component;
        }

        return findEmoji(soc.sprite())
                .map(m -> (Component) Component.text(m.toDiscordString()))
                .orElse(component);
    }

}
