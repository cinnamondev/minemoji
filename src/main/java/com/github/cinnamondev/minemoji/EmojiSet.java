package com.github.cinnamondev.minemoji;

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

    }

    public String prefix;
    public int packVersion;
    public URI url;
    public List<SpriteMeta> emojis;
}
