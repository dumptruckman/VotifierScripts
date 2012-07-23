/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.votifierscripts;

import buscript.Buscript;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class VotifierScripts extends JavaPlugin implements Listener {

    private Buscript buscript;

    @EventHandler
    public void vote(VotifierEvent event) {
        executeVoteScript(event.getVote().getUsername(), null);
    }

    @Override
    public void onEnable() {
        getVoteScript();
        buscript = new Buscript(this);
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
        if (args.length != 1) {
            return false;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        executeVoteScript(args[0], player);
        return true;
    }

    public File getVoteScript() {
        File scriptFile = new File(getDataFolder(), "vote-script.txt");
        if (!scriptFile.exists()) {
            try {
                this.saveResource("vote-script.txt", false);
                if (!scriptFile.exists()) {
                    scriptFile.createNewFile();
                }
            } catch (IOException e) {
                getLogger().severe("Error creating script file: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return null;
            }
        }
        return scriptFile;
    }

    public File getStartupScript() {
        File scriptFile = new File(getDataFolder(), "startup-script.txt");
        if (!scriptFile.exists()) {
            try {
                this.saveResource("startup-script.txt", false);
                if (!scriptFile.exists()) {
                    scriptFile.createNewFile();
                }
            } catch (IOException e) {
                getLogger().severe("Error creating script file: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return null;
            }
        }
        return scriptFile;
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
}
