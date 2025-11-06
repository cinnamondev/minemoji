package com.github.cinnamondev.minemoji;

import java.net.URI;
import java.util.List;

public class EmojiSet {
    public static class SpriteMeta {
        public SpriteMeta(String emojiText, String resource) {
            this(emojiText, resource, -1, false);
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

        public String toDiscordString() {
            if (snowflake != -1) { // snowflake is known
                return "<" + (animated ? "a" : "") + ":" + emojiText + ":" + snowflake + ">";
            } else {
                return ":" + emojiText + ":";
            }
        }
    }
    public String prefix;
    public int packVersion;
    public URI url;
    public boolean serveToClient;
    public List<SpriteMeta> emojis;
}
