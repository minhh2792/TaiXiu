package com.cortezromeo.taixiu;

import com.cortezromeo.taixiu.api.server.VersionSupport;
import com.cortezromeo.taixiu.command.TaiXiuAdminCommand;
import com.cortezromeo.taixiu.command.TaiXiuCommand;
import com.cortezromeo.taixiu.file.HeadDatabaseFile;
import com.cortezromeo.taixiu.file.InventoryFile;
import com.cortezromeo.taixiu.file.MessageFile;
import com.cortezromeo.taixiu.listener.JoinListener;
import com.cortezromeo.taixiu.listener.PaneListener;
import com.cortezromeo.taixiu.manager.AutoSaveManager;
import com.cortezromeo.taixiu.manager.DatabaseManager;
import com.cortezromeo.taixiu.manager.TaiXiuManager;
import com.cortezromeo.taixiu.storage.SessionDataStorage;
import com.cortezromeo.taixiu.support.VaultSupport;
import com.cortezromeo.taixiu.task.AutoSaveTask;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static com.cortezromeo.taixiu.manager.DebugManager.setDebug;
import static com.cortezromeo.taixiu.util.MessageUtil.log;

public final class TaiXiu extends JavaPlugin {

    private AutoSaveTask autoSaveTask = null;
    public static TaiXiu plugin;
    private static TaiXiuManager manager;
    private static final String version = Bukkit.getServer().getClass().getName().split("\\.")[3];
    public static VersionSupport nms;
    private boolean serverSoftwareSupport = true;

    @Override
    public void onLoad() {

        plugin = this;

        Class supp;

        try {
            supp = Class.forName("com.cortezromeo.taixiu.support.version." + version + "." + version);
        } catch (ClassNotFoundException e) {
            serverSoftwareSupport = false;
            this.getLogger().severe("Plugin không thể chạy trên phiên bản: " + version);
            return;
        }

        try {
            nms = (VersionSupport) supp.getConstructor(Class.forName("org.bukkit.plugin.Plugin"), String.class).newInstance(this, version);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
            serverSoftwareSupport = false;
            this.getLogger().severe("Plugin không thể hỗ trợ phiên bản: " + version);
            return;
        }
    }

    @Override
    public void onEnable() {

        if (!serverSoftwareSupport) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        log("&f--------------------------------");
        log("&a▀▀█▀▀  █▀▀█ ▀█▀   ▀▄ ▄▀ ▀█▀  █  █");
        log("&a  █    █▄▄█  █      █    █   █  █");
        log("&a  █    █  █ ▄█▄   ▄▀ ▀▄ ▄█▄  ▀▄▄▀");
        log("");
        log("&fVersion: &b" + getDescription().getVersion());
        log("&fAuthor: &bCortez_Romeo");
        log("&eKhởi chạy plugin trên phiên bản: " + version);
        log("&f--------------------------------");

        initFile();
        setDebug(getConfig().getBoolean("debug"));

        initDatabase();
        initCommand();
        initListener();
        initSupport();

        manager = new TaiXiuManager();
        getManager().startTask(getConfig().getInt("task.taiXiuTask.time-per-session"));
        AutoSaveManager.startAutoSave(getConfig().getInt("auto-save-database.time"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (getConfig().getBoolean("toggle-settings.auto-toggle")) {
                if (!DatabaseManager.togglePlayers.contains(p.getName())) {
                    DatabaseManager.togglePlayers.add(p.getName());
                }
            }
        }
    }

    private void initFile() {

        // config.yml
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        try {
            ConfigUpdater.update(this, "config.yml", configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        reloadConfig();

        // message.yml
        String messageFileName = getForCurrentVersion("message.yml", "messagev13.yml");
        MessageFile.setup();
        MessageFile.saveDefault();
        File messageFile = new File(getDataFolder(), "message.yml");
        try {
            ConfigUpdater.update(this, messageFileName, messageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MessageFile.reload();

        // inventory.yml
        if (!new File(getDataFolder() + "/inventory.yml").exists()) {
            InventoryFile.setup();
            InventoryFile.setupLang();
        } else
            InventoryFile.fileExists();
        InventoryFile.reload();

        // headdatabase.yml
        if (!new File(getDataFolder() + "/headdatabase.yml").exists()) {
            HeadDatabaseFile.setup();
        } else
            HeadDatabaseFile.fileExists();
        HeadDatabaseFile.reload();

    }

    private void initCommand() {
        new TaiXiuCommand(this);
        new TaiXiuAdminCommand(this);
    }

    private void initListener() {
        new PaneListener(this);
        new JoinListener(this);
    }

    private void initDatabase() {
        SessionDataStorage.init();
        DatabaseManager.loadAllDatabase();
    }

    private void initSupport() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            log("&cPlugin &bTài Xỉu &ccần thêm plugin &6Vault&c và plugin về &6Economy&c để hoạt động");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            VaultSupport.setup();
        }
    }

    public TaiXiuManager getManager() {
        return manager;
    }

    public static String getServerVersion() {
        return version;
    }

    public static String getForCurrentVersion(String v12, String v13) {
        switch (getServerVersion()) {
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
            case "v1_11_R1":
            case "v1_12_R1":
                return v12;
        }
        return v13;
    }

    @Override
    public void onDisable() {

        log("&f--------------------------------");
        log("&c▀▀█▀▀  █▀▀█ ▀█▀   ▀▄ ▄▀ ▀█▀  █  █");
        log("&c  █    █▄▄█  █      █    █   █  █");
        log("&c  █    █  █ ▄█▄   ▄▀ ▀▄ ▄█▄  ▀▄▄▀");
        log("");
        log("&fVersion: &b" + getDescription().getVersion());
        log("&fAuthor: &bCortez_Romeo");
        log("&f--------------------------------");

        DatabaseManager.saveAllDatabase();

    }
}
