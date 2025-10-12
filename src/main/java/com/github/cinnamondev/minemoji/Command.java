package com.github.cinnamondev.minemoji;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;


public class Command {
    private final SpriteEmojiManager manager;
    private final Minemoji p;
    public Command(Minemoji p) {
        this.p = p;
        this.manager = p.emojiManager;
    }

    private final LiteralCommandNode<CommandSourceStack> COMMAND = Commands.literal("minemoji")
            .then(Commands.literal("list")
                    .then(Commands.argument("pack", StringArgumentType.word())
                            .executes(ctx -> listByPack(ctx, false)))
                    .then(Commands.literal("normal")
                            .executes(ctx -> listByPack(ctx, true)))
            )
            .then(Commands.literal("help"))
            .executes(Command::helpCommand)
            .build();

    private Component emoteWithLore(ObjectComponent component, String knownString) {
        return component
                .hoverEvent(HoverEvent.showText(
                        Component.text(knownString)
                                .appendNewline()
                                .append(Component.text("Click to copy to clipboard!"))
                ))
                .clickEvent(ClickEvent.copyToClipboard(knownString));
    }
    private static Component formatEmotes(List<Component> components, int columns) {
        return Component.join(JoinConfiguration.newlines(),
                Lists.partition(components, columns).stream()
                        .map(cs -> Component.join(JoinConfiguration.separator(Component.text(" ")), cs))
                        .toList()
        );
    }

    private int listByPack(CommandContext<CommandSourceStack> context, boolean defaultPack) throws CommandSyntaxException {
        List<Component> list;
        if (defaultPack) {
            list = manager.getDefaultEmojiMap().entrySet().stream()
                    .map(e -> emoteWithLore(e.getValue(),e.getKey().getDiscordAliases().getFirst()))
                    .toList();
        } else {
            list = manager.getPackByPrefix(context.getArgument("pack", String.class)).stream()
                    .flatMap(p -> p.emojis.stream()
                            .map(m -> emoteWithLore(SpriteEmojiManager.spriteMetaToComponent(m), ":" + m.emojiText + ":"))
                    ).toList();
        }

        if (list.isEmpty()) {
            context.getSource().getSender().sendMessage(Component.text("No pack found!"));
            return 1;
        }

        context.getSource().getSender().sendMessage(formatEmotes(list, 10));
        return 1;
    }
    private static int helpCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 1;
    }
}
