package com.dumptruckman.minecraft.votifierscripts;

import murlen.util.fscript.FSException;
import murlen.util.fscript.FSFastExtension;
import murlen.util.fscript.FSUnsupportedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;

class ScriptExtension extends FSFastExtension {

    private VotifierScripts plugin;

    ScriptExtension(VotifierScripts plugin) {
        this.plugin = plugin;
    }

    @Override
    public Object callFunction(String name, ArrayList params) throws FSException {
        if (name.equalsIgnoreCase("hasperm") && params.size() == 2) {
            if (plugin.permissions != null) {
                return plugin.permissions.has(params.get(0).toString(), plugin.target, params.get(1).toString());
            } else {
                throw new FSException("Vault must be installed to use hasperm(world, perm)!");
            }
        } else if (name.equalsIgnoreCase("hasperm") && params.size() == 1) {
            Player player = Bukkit.getPlayerExact(name);
            return player != null && player.hasPermission(params.get(0).toString());
        } else if (name.equalsIgnoreCase("cmd") && params.size() == 1) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), insertTarget(params.get(0).toString()));
        } else if (name.equalsIgnoreCase("online") && params.size() == 0) {
            return Bukkit.getPlayerExact(plugin.target) != null;
        } else if (name.equalsIgnoreCase("broadcast") && params.size() == 1) {
            Bukkit.broadcastMessage(insertTarget(params.get(0).toString()));
        } else if (name.equalsIgnoreCase("broadcast") && params.size() == 2) {
            Bukkit.broadcast(insertTarget(params.get(0).toString()), params.get(1).toString());
        } else if (name.equalsIgnoreCase("runlater") && params.size() == 2) {
            long delay = TimeTools.fromShortForm(params.get(1).toString());
            plugin.scheduleScript(plugin.target, new File(plugin.scriptFolder, params.get(0).toString()).toString(), delay * 1000);
        } else {
            throw new FSUnsupportedException();
        }
        return null;
    }

    private String insertTarget(String string) {
        return string.replaceAll("%p", plugin.target);
    }
}
