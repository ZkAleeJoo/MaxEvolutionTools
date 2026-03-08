package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.listeners.ToolEvolutionListener;

public final class MaxEvolutionTools extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private ToolEvolutionManager toolEvolutionManager;

    @Override
    public void onEnable() {
        mainConfigManager = new MainConfigManager(this);

        toolEvolutionManager = new ToolEvolutionManager(this);
        toolEvolutionManager.reload();

        MainCommand mainCommand = new MainCommand(this);
        if (getCommand("maxevolutiontools") != null) {
            getCommand("maxevolutiontools").setExecutor(mainCommand);
            getCommand("maxevolutiontools").setTabCompleter(mainCommand);
        } else {
            getLogger().severe("Command maxevolutiontools is not defined in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new ToolEvolutionListener(this, toolEvolutionManager), this);

        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "   _____                 ___________           .__          __  .__            ___________           .__          ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "  /     \\ _____  ___  ___\\_   _____/__  ______ |  |  __ ___/  |_|__| ____   ___\\__    ___/___   ____ |  |   ______");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + " /  \\ /  \\\\__  \\ \\  \\/  / |    __)_\\  \\/ /  _ \\|  | |  |  \\   __\\  |/  _ \\ /    \\|    | /  _ \\ /  _ \\|  |  /  ___/");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "/    Y    \\/ __ \\_>    <  |        \\\\   (  <_> )  |_|  |  /|  | |  (  <_> )   |  \\    |(  <_> |  <_> )  |__\\___ \\ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "\\____|__  (____  /__/\\_ \\/_______  / \\_/ \\____/|____/____/ |__| |__|\\____/|___|  /____| \\____/ \\____/|____/____  >");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "        \\/     \\/      \\/        \\/                                            \\/                              \\/ ");
    }

    @Override
    public void onDisable() {
    }

    public MainConfigManager getConfigManager() {
        return mainConfigManager;
    }

    public ToolEvolutionManager getToolEvolutionManager() {
        return toolEvolutionManager;
    }
}