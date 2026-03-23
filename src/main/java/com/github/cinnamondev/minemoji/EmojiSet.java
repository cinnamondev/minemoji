package com.github.cinnamondev.minemoji;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface EmojiSet {
    Key ATLAS = Key.key("paintings");
    class SpriteMeta {
        public SpriteMeta(String emojiText, String resource, boolean animated) {
            this(emojiText, resource, -1, animated);
        }
        public SpriteMeta(String emojiText, String resource, long snowflake, boolean animated) {
            this.emojiText = emojiText;
            this.resource = resource;
            this.animated = animated;
            this.snowflake = snowflake;
        }
        public boolean animated = false;
        public long snowflake = -1;
        public String emojiText;
        public String resource;
        //public String[] aliases;

        Key key() {
            return Key.key(resource);
        }
        public String toDiscordString() {
            // turns out discordsrv takes care of guild awareness for us. Huh. More you know.
            //if (snowflake > 0) { // snowflake is known
            //    return ":" + emojiText + ":" + snowflake;
            //} else {
                return ":" + emojiText + ":";
            //}
        }

        public ObjectComponent toObjectComponent() {
            return Component.object().contents(ObjectContents.sprite(
                    ATLAS,
                    key()
            )).build();
        }

        public ObjectComponent componentWithBasicLore() {
            return toObjectComponent().hoverEvent(HoverEvent.showText(
                    Component.text(":" + emojiText + ":")
                            .appendNewline()
                            .append(Component.text("Click to copy to clipboard!"))
            )).clickEvent(ClickEvent.copyToClipboard(":" + emojiText + ":"));
        }

        public ObjectComponent componentWithContextualLore(EmojiSet ownerSet) {
            return toObjectComponent().hoverEvent(HoverEvent.showText(
                    Component.text(":" + emojiText + ":")
                            .appendNewline().append(ownerSet instanceof UnicodeEmojiSet
                                    ? Component.text("Default Emote")
                                    : Component.text("Custom Emote! (" + ownerSet.prefix() + ")")
                                    .style(Style.style(NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                            ).appendNewline()
                            .append(Component.text("Click to copy to clipboard!"))
            )).clickEvent(ClickEvent.copyToClipboard(":" + ownerSet.prefix() + "--" + emojiText + ":"));
        }
    }

    String prefix();
    int packVersion();
    @Nullable URI uri();
    List<SpriteMeta> allEmotes();
    Optional<SpriteMeta> tryFindByText(String emoteText);
    Optional<SpriteMeta> tryFindByKey(Key key);
    default Optional<SpriteMeta> tryFindByComponent(ObjectComponent component) {
        if (!(component.contents() instanceof SpriteObjectContents soc)) { return Optional.empty(); }
        return tryFindByKey(soc.sprite());
    }

    default Optional<ObjectComponent> tryFindComponent(String emoteText) {
            return tryFindByText(emoteText).map(SpriteMeta::toObjectComponent);
    }
    default Optional<String> tryCreateEmoteString(String emoteText) {
        return tryFindByText(emoteText).map(SpriteMeta::toDiscordString);
    }
    default Optional<String> tryCreateEmoteString(ObjectComponent component) {
        return tryFindByComponent(component).map(SpriteMeta::toDiscordString);
    }
}
