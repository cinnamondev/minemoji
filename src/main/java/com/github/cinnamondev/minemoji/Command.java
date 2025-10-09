package com.github.cinnamondev.minemoji;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.dependencies.mcdiscordreserializer.discord.DiscordSerializer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class Command {
    public static LiteralCommandNode<CommandSourceStack> COMMAND = Commands.literal("minemoji")
            .then(Commands.literal("list")
                    .then(Commands.argument("pack", StringArgumentType.word()))
                    .then(Commands.literal("normal"))
            )
            .then(Commands.literal("help"))
            .executes(Command::helpCommand)
            .build();


    private static int listByPack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 1;
    }
    private static int helpCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 1;
    }
}
