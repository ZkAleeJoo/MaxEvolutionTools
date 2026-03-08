package org.zkaleejoo.commands.subcommands;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface SubCommand {

    String getName();

    String getPermission();

    boolean execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}