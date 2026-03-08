package org.zkaleejoo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxEvolutionTools;

public class MainConfigManager {

    private final CustomConfig configFile;
    private CustomConfig langFile;
    private final CustomConfig evolutionFile;
    private final MaxEvolutionTools plugin;

    //VARIABLES CONFIG
    private String prefix;

    //VARIABLES MENSAJES
    private String msgNoPermission;
    private String msgPluginReload;
    private String msgMilestoneReached;
    private String msgSpecialUnlocked;
    private String msgToolInfo;
    private String msgOnlyPlayers;
    private String msgInvalidTool;
    private String msgEnabledWord;
    private String msgDisabledWord;
    private String msgCommandUsage;

    public MainConfigManager(MaxEvolutionTools plugin) {
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        evolutionFile = new CustomConfig("evolution.yml", null, plugin, false);
        configFile.registerConfig();
        evolutionFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();

        String selectedLanguage = config.getString("general.language", "en");

        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        //CONFIG
        prefix = config.getString("general.prefix", "&#FF0000&lM&#FF0A00&la&#FF1500&lx&#FF1F00&lE&#FF2A00&lv&#FF3400&lo&#FF3E00&ll&#FF4900&lu&#FF5300&lt&#FF5D00&li&#FF6800&lo&#FF7200&ln&#FF7D00&lT&#FF8700&lo&#FF9100&lo&#FF9C00&ll&#FFA600&ls &8» ");

        //MENSAJES
        msgNoPermission = lang.getString("messages.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.plugin-reload", "&aConfiguration successfully reloaded.");
        msgMilestoneReached = lang.getString("messages.milestone-reached", "&aYour tool reached &e%blocks%&a blocks and gained &e%enchant% %level%&a.");
        msgSpecialUnlocked = lang.getString("messages.special-unlocked", "&6Your tool unlocked a special ability.");
        msgToolInfo = lang.getString("messages.tool-info", "&7Tool blocks mined: &e%usage% &8| &7Special: &e%special%");
        msgOnlyPlayers = lang.getString("messages.only-players", "&cOnly players can use this command.");
        msgInvalidTool = lang.getString("messages.invalid-tool", "&cHold a valid tool in your hand.");
        msgEnabledWord = lang.getString("messages.enabled-word", "Enabled");
        msgDisabledWord = lang.getString("messages.disabled-word", "Disabled");
        msgCommandUsage = lang.getString("messages.command-usage", "&cUsage: /maxevolutiontools <reload|toolinfo>");
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        evolutionFile.reloadConfig();
        if (langFile != null) {
            langFile.reloadConfig();
        }
        loadConfig();
    }

    //GETTERS
    public String getPrefix() { return prefix; }

    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPluginReload() { return msgPluginReload; }
    public String getMsgMilestoneReached() { return msgMilestoneReached; }
    public String getMsgSpecialUnlocked() { return msgSpecialUnlocked; }
    public String getMsgToolInfo() { return msgToolInfo; }
    public String getMsgOnlyPlayers() { return msgOnlyPlayers; }
    public String getMsgInvalidTool() { return msgInvalidTool; }
    public String getMsgEnabledWord() { return msgEnabledWord; }
    public String getMsgDisabledWord() { return msgDisabledWord; }
    public String getMsgCommandUsage() { return msgCommandUsage; }
    public FileConfiguration getEvolutionConfig() {
        return evolutionFile.getConfig();
    }
}