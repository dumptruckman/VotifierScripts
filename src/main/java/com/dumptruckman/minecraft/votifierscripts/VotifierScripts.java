package com.dumptruckman.minecraft.votifierscripts;

import com.vexsoftware.votifier.model.VotifierEvent;
import murlen.util.fscript.FSException;
import murlen.util.fscript.FScript;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotifierScripts extends JavaPlugin implements Listener {

    ScriptExtension scriptExtension;
    Permission permissions;
    String target = null;
    boolean runTasks;
    File scriptFolder;

    Map<String, List<Map<String, Object>>> delayedScripts = new HashMap<String, List<Map<String, Object>>>();

    private class ScriptTask implements Runnable {
        @Override
        public void run() {
            if (!runTasks) {
                return;
            }
            long time = System.currentTimeMillis();
            for (Map.Entry<String, List<Map<String, Object>>> entry : delayedScripts.entrySet()) {
                List<Map<String, Object>> removals = new ArrayList<Map<String, Object>>(entry.getValue().size());
                for (Map<String, Object> script : entry.getValue()) {
                    if (script.get("time") != null) {
                        try {
                            long scriptTime = Long.valueOf(script.get("time").toString());
                            if (time >= scriptTime) {
                                if (script.get("file") != null) {
                                    File scriptFile = new File(script.get("file").toString());
                                    if (scriptFile.exists()) {
                                        executeScript(entry.getKey(), scriptFile);
                                        removals.add(script);
                                    } else {
                                        try {
                                            scriptFile.createNewFile();
                                        } catch (IOException ignore) { }
                                        if (scriptFile.exists()) {
                                            executeScript(entry.getKey(), scriptFile);
                                            removals.add(script);
                                        } else {
                                            getLogger().warning("Missing script file: " + scriptFile);
                                            removals.add(script);
                                        }
                                    }
                                } else {
                                    getLogger().warning("Invalid delayed script entry");
                                    removals.add(script);
                                }
                            }
                        } catch (NumberFormatException ignore) {
                            getLogger().warning("Invalid delayed script entry");
                            removals.add(script);
                        }
                    } else {
                        getLogger().warning("Invalid delayed script entry");
                        removals.add(script);
                    }
                }
                entry.getValue().removeAll(removals);
                if (!removals.isEmpty()) {
                    saveConfig();
                }
            }
            getServer().getScheduler().scheduleSyncDelayedTask(VotifierScripts.this, this, 20L);
        }
    }

    @EventHandler
    public void vote(VotifierEvent event) {
        executeDefaultScript(event.getVote().getUsername());
    }

    @Override
    public void onEnable() {
        scriptFolder = new File(getDataFolder(), "scripts");
        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }
        setupPermissions();
        getServer().getPluginManager().registerEvents(this, this);
        scriptExtension = new ScriptExtension(this);
        reloadConfig();
        ConfigurationSection scripts = getConfig().getConfigurationSection("scripts");
        if (scripts != null) {
            for (String player : scripts.getKeys(false)) {
                List<Map<String, Object>> playerScripts = new ArrayList<Map<String, Object>>();
                delayedScripts.put(player, playerScripts);
                for (Object scriptObj : scripts.getList(player)) {
                    if (scriptObj instanceof Map) {
                        Map scriptMap = (Map) scriptObj;
                        Map<String, Object> script = new HashMap<String, Object>(2);
                        for (Object keyObj : scriptMap.keySet()) {
                            script.put(keyObj.toString(), scriptMap.get(keyObj));
                        }
                        playerScripts.add(script);
                    }
                }
            }
        }
        runTasks = true;
        getServer().getScheduler().scheduleSyncDelayedTask(this, new ScriptTask(), 20L);
    }

    @Override
    public void onDisable() {
        runTasks = false;
        saveConfig();
    }

    public FScript getFScript() {
        FScript fScript = new FScript();
        fScript.registerExtension(scriptExtension);
        return fScript;
    }

    private boolean setupPermissions()
    {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permissions = permissionProvider.getProvider();
        }
        return (permissions != null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        executeDefaultScript("dumptruckman");
        return true;
    }

    @Override
    public void saveConfig() {
        getConfig().set("scripts", delayedScripts);
        super.saveConfig();
    }

    private File loadDefaultScriptFile() {
        File scriptFile = new File(getDataFolder(), "vote-script.txt");
        if (!scriptFile.exists()) {
            try {
                scriptFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Error creating script file: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return null;
            }
        }
        return scriptFile;
    }

    public void executeDefaultScript(String player) {
        File scriptFile = loadDefaultScriptFile();
        if (scriptFile == null) {
            return;
        }
        executeScript(player, scriptFile);
    }

    public void executeScript(String player, File scriptFile) {
        target = player;
        runScript(scriptFile);
    }

    public void scheduleScript(String player, String fileName, long delay) {
        List<Map<String, Object>> playerScripts = delayedScripts.get(player);
        if (playerScripts == null) {
            playerScripts = new ArrayList<Map<String, Object>>();
            delayedScripts.put(player, playerScripts);
        }
        Map<String, Object> script = new HashMap<String, Object>(2);
        script.put("time", System.currentTimeMillis() + delay);
        script.put("file", fileName);
        playerScripts.add(script);
        saveConfig();
    }

    private void runScript(File script) {
        Reader reader = null;
        FScript fScript = getFScript();
        try{
            reader = new FileReader(script);
            fScript.load(reader);
            fScript.run();
        } catch (IOException e){
            getLogger().warning("Read error: " + e.getMessage());
        } catch (FSException e) {
            getLogger().warning("Error parsing script: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) { }
            }
        }
    }
}
