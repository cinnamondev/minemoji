package com.github.cinnamondev.minemoji.Command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.fellbaum.jemoji.EmojiGroup;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnicodeGroup implements CustomArgumentType.Converted<EmojiGroup, String> {
    static final DynamicCommandExceptionType ERROR_NOT_GROUP = new DynamicCommandExceptionType(str ->
            MessageComponentSerializer.message().serialize(Component.text(str + "is not a valid emoji group"))
    );

    static final List<String> words = List.of(
            "smileys", "emotion",
            "people", "body",
            "component",
            "animals", "nature",
            "food", "drink",
            "travel", "places",
            "activities",
            "objects",
            "symbols",
            "flags"
    );
    @Override
    public EmojiGroup convert(String nativeType) throws CommandSyntaxException {
        return switch (nativeType.toLowerCase()) {
            case "smileys", "emotion" -> EmojiGroup.SMILEYS_AND_EMOTION;
            case "people", "body" -> EmojiGroup.PEOPLE_AND_BODY;
            case "component" -> EmojiGroup.COMPONENT;
            case "animals", "nature" -> EmojiGroup.ANIMALS_AND_NATURE;
            case "food", "drink" -> EmojiGroup.FOOD_AND_DRINK;
            case "travel", "places" -> EmojiGroup.TRAVEL_AND_PLACES;
            case "activities" -> EmojiGroup.ACTIVITIES;
            case "objects" -> EmojiGroup.OBJECTS;
            case "symbols" -> EmojiGroup.SYMBOLS;
            case "flags" -> EmojiGroup.FLAGS;
            default -> throw ERROR_NOT_GROUP.create(nativeType);
        };
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String word: words) {
            if (word.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(word);
            }
        }
        return builder.buildFuture();
    }
}
