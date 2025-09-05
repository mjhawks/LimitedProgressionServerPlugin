package com.limitedprogression.commands;
import com.limitedprogression.core.LimitedProgression;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class AgeTabCompleter implements TabCompleter{
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1){ //first line
            List<String> arg1options = Collections.singletonList("set");
            return arg1options;
        } else if (args.length == 2) {
            List<String> ages = Arrays.asList(Stream.of(LimitedProgression.age.values()).map(Enum::name).toArray(String[]::new));
            return ages;

        }
        return null;
    }
}
