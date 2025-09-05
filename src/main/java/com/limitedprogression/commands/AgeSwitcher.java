package com.limitedprogression.commands;
import com.limitedprogression.core.LimitedProgression;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

public class AgeSwitcher implements CommandExecutor{

    private final LimitedProgression plugin;

    public AgeSwitcher(LimitedProgression lp){
        plugin = lp;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("age")){
            if(sender instanceof Player){
                Player p = (Player) sender;
                if(args.length > 0){
                    if(args[0].equalsIgnoreCase("set")){
                        if(args.length>1){
                            try {
                                LimitedProgression.age newAge = LimitedProgression.age.valueOf(args[1]);
                                this.plugin.setCurrentAge(newAge);
                                p.sendMessage("set current age to "+newAge.name());
                            }
                            catch (IllegalArgumentException e){
                                p.sendMessage("incorrect value for age");
                            }
                        }
                        else{
                            p.sendMessage("incorrect number of arguments");
                        }
                    }
                }
                else{
                    p.sendMessage("This server is currently set to the "+ plugin.getCurrentAge().toString()+" Age");
                }

            }
        }


        return false;
    }

}

