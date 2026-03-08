package org.zkaleejoo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxEvolutionTools;

public class MainConfigManager {

    private CustomConfig configFile;
    private CustomConfig langFile;
    private MaxEvolutionTools plugin;

    //VARIABLES CONFIG
    private String selectedLanguage;
    private String prefix;

    //VARIABLES MENSAJES
    private String msgNoPermission;
    private String msgPluginReload;

    public MainConfigManager(MaxEvolutionTools plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();

        selectedLanguage = config.getString("general.language", "en");

        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        //CONFIG
        prefix = config.getString("general.prefix", "&#FF0000&lM&#FF0A00&la&#FF1500&lx&#FF1F00&lE&#FF2A00&lv&#FF3400&lo&#FF3E00&ll&#FF4900&lu&#FF5300&lt&#FF5D00&li&#FF6800&lo&#FF7200&ln&#FF7D00&lT&#FF8700&lo&#FF9100&lo&#FF9C00&ll&#FFA600&ls &8» ");

        //MENSAJES
        msgNoPermission = lang.getString("messages.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.plugin-reload", "&aConfiguration successfully reloaded.");
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        if(langFile != null) langFile.reloadConfig();
        loadConfig();
    }

    //GETTERS
    public String getPrefix() { return prefix; }

    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPluginReload() { return msgPluginReload; }
}