package com.github.cinnamondev.minemoji;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.fellbaum.jemoji.Emoji;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.stream.Collectors;


public class Command {
    private final Minemoji p;
    private final LiteralCommandNode<CommandSourceStack> command;
    private Map<String, List<Component>> packPages = Collections.emptyMap();
    private List<Component> defaultPackPages = Collections.emptyList();

    public void registerCustomPacks(Map<String, EmojiSet> packs) {
        this.packPages = packs.entrySet().stream().map(e ->
                Map.entry(e.getKey(),
                        paginateComponents(e.getValue().emojis.stream()
                                .map(emote -> emoteWithLore(
                                        SpriteEmojiManager.spriteMetaToComponent(emote),
                                        ":" + e.getKey() + "--" + emote.emojiText + ":"
                                )).toList(), 8, 8)
                )).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void registerDefaultPack(Map<Emoji, ObjectComponent> pack) {
        this.defaultPackPages = paginateComponents(
                pack.entrySet().stream()
                        .filter(e -> !e.getKey().getDiscordAliases().isEmpty())
                        .map(e -> emoteWithLore(e.getValue(), e.getKey().getDiscordAliases().getFirst()))
                        .toList(),
                8,
                8
        );
    }


    public Command(Minemoji p) {
        this.p = p;
        command = Commands.literal("minemoji")
                .then(Commands.literal("about")
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Minemoji " + p.getPluginMeta().getVersion())
                                        .color(NamedTextColor.LIGHT_PURPLE)
                                        .appendNewline()
                                        .append(Component.text("[Github]")
                                                .style(Style.style(NamedTextColor.AQUA, TextDecoration.BOLD))
                                                .clickEvent(ClickEvent.openUrl("https://github.com/cinnamondev/minemoji"))
                                        )
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .requires(src -> src.getSender().hasPermission("minemoji.list"))
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    if ("unicode".startsWith(builder.getRemainingLowerCase())) {
                                        builder.suggest("unicode");
                                    }
                                    packPages.keySet().stream()
                                            .filter(k ->
                                                    k.toLowerCase().startsWith(builder.getRemainingLowerCase())
                                            )
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listByPack(ctx,
                                                ctx.getArgument("pack", String.class),
                                                ctx.getArgument("page", Integer.class))
                                        )
                                )
                                .executes(ctx -> listByPack(ctx, ctx.getArgument("pack", String.class), 1))
                        )
                        .executes(ctx -> {
                            Component text = Component.text("All available custom emote packs").append(
                                    Component.join(JoinConfiguration.newlines(),
                                            packPages.keySet().stream().map(str ->
                                                    Component.text(str)
                                                            .decorate(TextDecoration.UNDERLINED)
                                                            .hoverEvent(HoverEvent.showText(Component.text("Click to show pack contents")))
                                                            .clickEvent(ClickEvent.runCommand("minemoji list " + str))
                                            ).toList()
                                    ));
                            ctx.getSource().getSender().sendMessage(text);
                            return 1;
                        })
                )
                .then(Commands.literal("help").executes(Command::helpCommand))
                .executes(Command::helpCommand)
                .build();

    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return command;
    }
    private Component emoteWithLore(ObjectComponent component, String knownString) {
        return component
                .hoverEvent(HoverEvent.showText(
                        Component.text(knownString)
                                .appendNewline()
                                .append(Component.text("Click to copy to clipboard!"))
                ))
                .clickEvent(ClickEvent.copyToClipboard(knownString));
    }
    // turn a list of components into a list of components containing columns*rows components smushed together.
    private static List<Component> paginateComponents(List<Component> components, int columns, int rows) {
        return Lists.partition(components, columns * rows).stream()
                .map(l -> Component.join(JoinConfiguration.newlines(),
                        Lists.partition(l, columns).stream()
                                .map(cs -> Component.join(JoinConfiguration.separator(Component.text(" ")), cs))
                                .toList()
                ))
                .toList();
    }

    private int listByPack(CommandContext<CommandSourceStack> context, String pack, int page) throws CommandSyntaxException {
        List<Component> list;
        if (pack.equalsIgnoreCase("unicode")) {
            list = defaultPackPages;
        } else { list = packPages.get(pack); }

        if (list == null || list.isEmpty()) {
            context.getSource().getSender().sendMessage(Component.text("No pack found!").color(NamedTextColor.RED));
            return 1;
        }

        if (page > list.size() || page <= 0) {
            context.getSource().getSender().sendMessage(Component.text("Page does not exist").color(NamedTextColor.RED));
            return 1;
        }

        context.getSource().getSender().sendMessage(
                Component.text("Pack " + pack + ":").appendNewline().append(
                        list.get(page - 1)
                ).appendNewline().append(pageScroller(pack, page, list.size()))
        );
        return 1;
    }

    private static Component pageScroller(String pack, int currentPage, int totalPages) {
        if (totalPages == 1) { return Component.empty(); }
        Component backPage = Component.text("[ << ]")
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.GRAY);
        if ((currentPage-1 > 0)) {
            backPage = backPage.color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("minemoji list " + pack + " " + (currentPage - 1)));
        }

        Component nextPage = Component.text("[ >> ]")
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.GRAY);
        if ((currentPage+1) <= totalPages) {
            nextPage = nextPage.color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("minemoji list " + pack + " " + (currentPage + 1)));
        }

        return backPage
                .append(Component.text(" " + currentPage + "/" + totalPages + " ").color(NamedTextColor.YELLOW))
                .append(nextPage);

    }
    private static int helpCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().getSender().sendMessage(Component.text("""
                /minemoji list <pack-name|unicode>"""));
        return 1;
    }
}
