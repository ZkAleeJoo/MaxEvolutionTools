package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+"   _____                 ___________           .__          __  .__            ___________           .__          ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+" /     \\ _____  ___  ___\\_   _____/__  ______ |  |  __ ___/  |_|__| ____   ___\\__    ___/___   ____ |  |   ______");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+" /  \\ /  \\\\__  \\ \\  \\/  / |    __)_\\  \\/ /  _ \\|  | |  |  \\   __\\  |/  _ \\ /    \\|    | /  _ \\ /  _ \\|  |  /  ___/");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+"/    Y    \\/ __ \\_>    <  |        \\\\   (  <_> )  |_|  |  /|  | |  (  <_> )   |  \\    |(  <_> |  <_> )  |__\\___ \\ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+"\\____|__  (____  /__/\\_ \\/_______  / \\_/ \\____/|____/____/ |__| |__|\\____/|___|  /____| \\____/ \\____/|____/____  >");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW+"        \\/     \\/      \\/        \\/                                            \\/                              \\/ ");

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
