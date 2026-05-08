package com.github.cinnamondev.minemoji.Command;

import com.github.cinnamondev.minemoji.EmojiSet;
import com.github.cinnamondev.minemoji.Minemoji;
import com.github.cinnamondev.minemoji.UnicodeEmojiSet;
import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.EmojiSubGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Command {
    private static final Logger log = LoggerFactory.getLogger(Command.class);
    final Minemoji p;
    public Command(Minemoji p) {
        this.p = p;
    }


    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("minemoji")
                .then(Commands.literal("tellraw")
                        .requires(src -> src.getSender().hasPermission("minemoji.tellraw"))
                        .then(Commands.argument("targets", ArgumentTypes.players())
                                .then(Commands.argument("component", ArgumentTypes.component())
                                        .executes(this::tellRaw))))
                .then(Commands.literal("pack")
                        .requires(src -> src.getSender().hasPermission("minemoji.list"))
                        .then(Commands.argument("set", new PackArgument(p))
                                .then(Commands.literal("emotes")
                                        .then(Commands.argument("page", IntegerArgumentType.integer(0,1000))
                                                .executes(this::packEmotePage))
                                        .executes(this::packEmoteFirstPage)
                                )
                                .then(Commands.literal("about").executes(this::packDefaultExecutor))
                                .executes(this::packDefaultExecutor)
                        ))
                .then(Commands.literal("reload").requires(src -> src.getSender().hasPermission("minemoji.reload"))
                        .executes(this::reload))
                .then(Commands.literal("help").executes(this::defaultExecutor))
                .executes(this::defaultExecutor)
                .build();
    }

    private Component collectComponents(List<Component> components, int componentsPerLine) {
        return Component.join(JoinConfiguration.newlines(),
                Lists.partition(components, componentsPerLine).stream()
                        .map(cs -> Component.join(JoinConfiguration.spaces(), cs))
                        .toList()
        );
    }

    int reload(CommandContext<CommandSourceStack> ctx) {
        p.reloadConfig();
        p.load().whenComplete((_v, ex) -> {
            if (ex != null) {
                ctx.getSource().getSender().sendMessage(
                        Component.text("Failed to reload. Please read console.")
                                .color(NamedTextColor.RED)
                );
                p.getLogger().severe(ex.getMessage());
            } else {
                ctx.getSource().getSender().sendMessage(Component.text("Done!"));
            }
        });
        return 1;
    }

    int tellRaw(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
        final List<Player> targets = targetResolver.resolve(ctx.getSource());

        final Component component = ctx.getArgument("component", Component.class);
        final Component emojified = p.getEmoteManager().emojize(component);

        for (final Player player : targets) {
            player.sendMessage(emojified);
        }
        return 1;
    }

    int packEmoteFirstPage(CommandContext<CommandSourceStack> ctx) {
        final EmojiSet set = ctx.getArgument("set", EmojiSet.class);

        ctx.getSource().getSender().sendMessage(formatPage(set,0));

        return 1;
    }
    int packEmotePage(CommandContext<CommandSourceStack> ctx) {
        final EmojiSet set = ctx.getArgument("set", EmojiSet.class);
        final int page =  ctx.getArgument("page", Integer.class);

        ctx.getSource().getSender().sendMessage(
                formatPage(set, page)
        );

        return 1;
    }

    private static final Component DISABLED_NEXT_PAGE = Component.text("[ >> ]")
            .style(Style.style(TextDecoration.BOLD, NamedTextColor.GRAY));
    private static final Component DISABLED_PREV_PAGE = Component.text("[ << ]")
            .style(Style.style(TextDecoration.BOLD, NamedTextColor.GRAY));
    Component formatPage(EmojiSet set, int page) {
        var pages = set.fetchPage(page, 10, 10);
        if (pages.isPresent()) {
            var p = pages.get();
            Component backButton = page != 0
                    ? Component.text("[ << ]")
                    .style(Style.style(TextDecoration.BOLD, NamedTextColor.BLUE))
                    .clickEvent(ClickEvent.runCommand("minemoji pack " + set.prefix() + " emotes " + (page-1)))
                    : DISABLED_PREV_PAGE;
            Component nextButton = pages.get().getValue()
                    ? Component.text("[ >> ]")
                    .style(Style.style(TextDecoration.BOLD, NamedTextColor.BLUE))
                    .clickEvent(ClickEvent.runCommand("minemoji pack " + set.prefix() + " emotes " + (page +1)))
                    : DISABLED_NEXT_PAGE;
            return Component.join(JoinConfiguration.newlines(), List.of(
                    Component.text("===Emotes in " + set.prefix() + "===").style(Style.style(TextDecoration.BOLD)),
                    p.getKey(),
                    backButton.appendSpace().append(nextButton)

            ));
        } else {
            return Component.text("Invalid page.").color(NamedTextColor.RED);
        }
    }


    int unicodeListing(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final EmojiSubGroup group = ctx.getArgument("group", EmojiSubGroup.class);
        final UnicodeEmojiSet set = p.getEmoteManager().unicodeEmojiSet;
        if (set == null) { throw PackArgument.ERROR_INVALID_PACK.create("unicode"); }

        Component emoteCollection = collectComponents(
                EmojiManager.getAllEmojisBySubGroup(group).stream()
                        .map(e -> (Component) set.findByEmote(e).componentWithContextualLore(set))
                        .toList(),
                40
        );
        ctx.getSource().getSender().sendMessage(
                Component.text("All default emotes in: " + group.getName())
                        .style(Style.style(NamedTextColor.LIGHT_PURPLE))
                        .appendNewline()
                        .append(emoteCollection.color(NamedTextColor.WHITE))
        );
        return 1;
    }
    int packDefaultExecutor(CommandContext<CommandSourceStack> ctx) {
        final EmojiSet set = ctx.getArgument("set", EmojiSet.class);
        Component text = Component.join(JoinConfiguration.newlines(),
                List.of(
                        Component.text("Custom: " + (set instanceof UnicodeEmojiSet ? "No" : "Yes")),
                        Component.text("Prefix: " + set.prefix()),
                        Component.text("Version: " + set.packVersion()),
                        Component.text("URL: " + set.uri()),
                        Component.text("[ List Emotes ]")
                                .style(Style.style(NamedTextColor.AQUA, TextDecoration.BOLD))
                                .clickEvent(ClickEvent.runCommand("minemoji pack " + set.prefix() + " emotes"))
                )
        );

        ctx.getSource().getSender().sendMessage(text);
        return 1;
    }

    int defaultExecutor(CommandContext<CommandSourceStack> ctx) {
        String website = p.getPluginMeta().getWebsite();
        if (website == null) { website = ""; }
        Component header = Component.text("Minemoji " + p.getPluginMeta().getVersion())
                .append(Component.text(" [Github]").color(NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(website)))
                        .style(Style.style(NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));

        ctx.getSource().getSender().sendMessage(header);
        return 1;
    }
}
