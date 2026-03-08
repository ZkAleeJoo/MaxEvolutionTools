package org.zkaleejoo;

import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;


public final class MaxEvolutionTools extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    
    //PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        mainConfigManager = new MainConfigManager(this);
        getCommand("maxevolutiontools").setExecutor(new MainCommand(this));

    }

    @Override
    public void onDisable() {
    }


    public void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        registerCommand("maxevolutiontools", mainCommand, mainCommand);

        
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command \"" + name + "\" is missing in plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }




    public MainConfigManager getConfigManager() {
        return mainConfigManager;
    }

}
