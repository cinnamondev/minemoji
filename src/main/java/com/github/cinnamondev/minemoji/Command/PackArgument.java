package com.github.cinnamondev.minemoji.Command;

import com.github.cinnamondev.minemoji.EmojiSet;
import com.github.cinnamondev.minemoji.Minemoji;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public class PackArgument implements CustomArgumentType.Converted<EmojiSet, String> {
    private final Minemoji p;

    static final DynamicCommandExceptionType ERROR_INVALID_PACK = new DynamicCommandExceptionType(setName -> {
        return MessageComponentSerializer.message().serialize(Component.text(setName + " is not a valid pack."));
    });

    public PackArgument(Minemoji p) { this.p = p; }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if ("unicode".startsWith(builder.getRemainingLowerCase())) {
            builder.suggest("unicode");
        }
        var emotes = p.getEmoteManager().customEmoteMap;

        for (String prefix : emotes.keySet()) {
            if (prefix.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(prefix);
            }
        }

        return builder.buildFuture();
    }

    @Override
    public EmojiSet convert(String s) throws CommandSyntaxException {
        if (s.equalsIgnoreCase("unicode")) { return p.getEmoteManager().unicodeEmojiSet; }
        var oSet = p.getEmoteManager().findEmojiSet(s);
        if (oSet.isPresent()) {
            return oSet.get();
        } else {
            throw ERROR_INVALID_PACK.create(s);
        }
    }
}
