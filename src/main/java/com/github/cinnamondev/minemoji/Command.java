package com.github.cinnamondev.minemoji;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class Command {
    public static LiteralCommandNode<CommandSourceStack> COMMAND = Commands.literal("minemoji")
            .then(Commands.literal("list")
                    .then(Commands.argument("pack", StringArgumentType.word()))
                    .then(Commands.literal("normal"))
            )
            .then(Commands.literal("reload"))
            .then(Commands.literal("help"))
            .executes(Command::HelpCommand)
            .build();

    private static int HelpCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 1;
    }
}
