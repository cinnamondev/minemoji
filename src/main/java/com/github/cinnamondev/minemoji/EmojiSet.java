package com.github.cinnamondev.minemoji;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.ObjectContents;

import java.net.URI;
import java.util.List;

public class EmojiSet {
    public static class SpriteMeta {
        public SpriteMeta(String emojiText, String resource) {
            this.emojiText = emojiText;
            this.resource = resource;
        }
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

    public String prefix;
    public int packVersion;
    public URI url;
    public List<SpriteMeta> emojis;
}
