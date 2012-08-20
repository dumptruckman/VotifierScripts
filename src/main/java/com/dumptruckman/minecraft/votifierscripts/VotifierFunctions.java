/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.votifierscripts;

import buscript.Buscript;
import buscript.util.TimeTools;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;

class VotifierFunctions extends ScriptableObject {

    private VotifierScripts plugin;

    VotifierFunctions(VotifierScripts plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getClassName() {
        return "VotifierScripts";
    }

    public void runWhenOnline(String script, String target) {
        plugin.runWhenOnline(script, plugin.getScriptAPI().stringReplace(target));
    }
}
