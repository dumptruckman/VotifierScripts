/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.votifierscripts;

class VotifierFunctions {

    private VotifierScripts plugin;

    VotifierFunctions(VotifierScripts plugin) {
        this.plugin = plugin;
    }

    public void runWhenOnline(String script, String target) {
        plugin.runWhenOnline(script, plugin.getScriptAPI().stringReplace(target));
    }
}
