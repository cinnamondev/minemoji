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
import net.fellbaum.jemoji.EmojiSubGroup;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnicodeSubGroup implements CustomArgumentType.Converted<EmojiSubGroup, String> {
    static final DynamicCommandExceptionType ERROR_NOT_GROUP = new DynamicCommandExceptionType(str ->
            MessageComponentSerializer.message().serialize(Component.text(str + "is not a valid emoji group"))
    );

    @Override
    public EmojiSubGroup convert(String nativeType) throws CommandSyntaxException {
        return EmojiSubGroup.fromString(nativeType.toLowerCase());
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (EmojiSubGroup group: EmojiSubGroup.values()) {
            if (group.getName().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(group.getName());
            }
        }
        return builder.buildFuture();
    }
}
