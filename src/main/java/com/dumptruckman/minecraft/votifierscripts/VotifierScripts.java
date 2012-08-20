/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.votifierscripts;

import buscript.Buscript;
import buscript.StringReplacer;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VotifierScripts extends JavaPlugin implements Listener {

    private Buscript buscript;

    private String username = null;
    private String service = null;
    private String address = null;
    private String timestamp = null;

    private File voteScriptFile = null;
    private File startupScriptFile = null;
    private File playerFile = null;
    private FileConfiguration playerConfig = null;

    @EventHandler
    public void vote(VotifierEvent event) {
        Vote vote = event.getVote();
        username = vote.getUsername();
        service = vote.getServiceName();
        address = vote.getAddress();
        timestamp = vote.getTimeStamp();
        executeVoteScript(vote.getUsername(), null);
        username = null;
        service = null;
        address = null;
        timestamp = null;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        List<String> playerList = getPlayerConfig().getStringList("runWhenOnline." + player);
        if (playerList != null && !playerList.isEmpty()) {
            for (String script : playerList) {
                getScriptAPI().executeScript(new File(getScriptAPI().getScriptFolder(), script), player);
            }
            getPlayerConfig().set("runWhenOnline." + player, null);
            try {
                getPlayerConfig().save(getPlayerFile());
            } catch (IOException e) {
                getLogger().severe("Could not save player data.  Player may receive benefits of script multiple times!  Reason: " + e.getMessage());
            }
        }
    }

    private class UsernameReplacer implements StringReplacer {
        @Override
        public String getRegexString() {
            return "%username%";
        }

        @Override
        public String getReplacement() {
            return username;
        }

        @Override
        public String getGlobalVarName() {
            return "username";
        }
    }

    private class ServiceReplacer implements StringReplacer {
        @Override
        public String getRegexString() {
            return "%service%";
        }

        @Override
        public String getReplacement() {
            return service;
        }

        @Override
        public String getGlobalVarName() {
            return "service";
        }
    }

    private class AddressReplacer implements StringReplacer {
        @Override
        public String getRegexString() {
            return "%address%";
        }

        @Override
        public String getReplacement() {
            return address;
        }

        @Override
        public String getGlobalVarName() {
            return "address";
        }
    }

    private class TimestampReplacer implements StringReplacer {
        @Override
        public String getRegexString() {
            return "%timestamp%";
        }

        @Override
        public String getReplacement() {
            return timestamp;
        }

        @Override
        public String getGlobalVarName() {
            return "timestamp";
        }
    }

    @Override
    public void onEnable() {
        getVoteScript();
        getPlayerConfig();
        buscript = new Buscript(this);
        buscript.registerStringReplacer(new UsernameReplacer());
        buscript.registerStringReplacer(new ServiceReplacer());
        buscript.registerStringReplacer(new AddressReplacer());
        buscript.registerStringReplacer(new TimestampReplacer());
        buscript.addScriptMethods(new VotifierFunctions(this));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                getScriptAPI().executeScript(getStartupScript());
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        username = args[0];
        service = getServer().getName();
        if (args.length >= 2) {
            service = args[1];
        }
        address = "localhost";
        if (args.length >= 3) {
            address = args[2];
        }
        timestamp = new Timestamp(System.currentTimeMillis()).toString();
        if (args.length >= 4) {
            timestamp = args[3];
        }
        executeVoteScript(username, player);
        username = null;
        service = null;
        address = null;
        timestamp = null;
        return true;
    }

    public File getVoteScript() {
        if (voteScriptFile == null) {
            voteScriptFile = new File(getDataFolder(), "vote-script.txt");
            if (!voteScriptFile.exists()) {
                try {
                    this.saveResource("vote-script.txt", false);
                    if (!voteScriptFile.exists()) {
                        voteScriptFile.createNewFile();
                    }
                } catch (IOException e) {
                    getLogger().severe("Error creating script file: " + e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                    return null;
                }
            }
        }
        return voteScriptFile;
    }

    public File getStartupScript() {
        if (startupScriptFile == null) {
            startupScriptFile = new File(getDataFolder(), "startup-script.txt");
            if (!startupScriptFile.exists()) {
                try {
                    this.saveResource("startup-script.txt", false);
                    if (!startupScriptFile.exists()) {
                        startupScriptFile.createNewFile();
                    }
                } catch (IOException e) {
                    getLogger().severe("Error creating script file: " + e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                    return null;
                }
            }
        }
        return startupScriptFile;
    }

    private File getPlayerFile() {
        if (playerFile == null) {
            playerFile = new File(getDataFolder(), "players.dat");
            if (!playerFile.exists()) {
                try {
                    playerFile.createNewFile();
                } catch (IOException e) {
                    getLogger().severe("Error creating dat file: " + e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                    return null;
                }
            }
        }
        return playerFile;
    }

    private FileConfiguration getPlayerConfig() {
        if (playerConfig == null) {
            playerConfig = YamlConfiguration.loadConfiguration(getPlayerFile());
        }
        return playerConfig;
    }

    public void executeVoteScript(String target, Player player) {
        File scriptFile = getVoteScript();
        if (scriptFile == null) {
            return;
        }
        buscript.executeScript(scriptFile, target, player);
    }

    public Buscript getScriptAPI() {
        return buscript;
    }

    void runWhenOnline(String script, String target) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(target);
        if (player.isOnline()) {
            getScriptAPI().executeScript(new File(getScriptAPI().getScriptFolder(), script), target);
        } else {
            List<String> playerScripts = getPlayerConfig().getStringList("runWhenOnline." + target);
            if (playerScripts == null) {
                playerScripts = new ArrayList<String>(1);
            }
            playerScripts.add(script);
            getPlayerConfig().set("runWhenOnline." + target, playerScripts);
            try {
                getPlayerConfig().save(getPlayerFile());
            } catch (IOException e) {
                getLogger().severe("Could not save player data.  Player may not receive benefits of script!  Reason: " + e.getMessage());
            }
        }
    }
}
