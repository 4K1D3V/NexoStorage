package gg.kite.command;

import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StorageTab implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "listids", "place", "stats");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .filter(s -> sender.hasPermission("nexostorage." + (s.equals("reload") ? "reload" : "admin")))
                    .toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("place") && sender.hasPermission("nexostorage.admin")) {
            completions.addAll(Arrays.stream(NexoBlocks.blockIDs())
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList());
        }
        return completions;
    }
}