package com.inneractive.jenkins.plugins.consul.Util;

import com.inneractive.jenkins.plugins.consul.ConsulInstallation;
import hudson.Launcher;

import java.io.IOException;
import java.util.ArrayList;

public class CommandBuilder {

    private ArrayList<String> cmds = new ArrayList<>();

    public CommandBuilder(ConsulInstallation installation, Launcher launcher) throws IOException, InterruptedException {
        cmds = new ArrayList<>();
        cmds.add(installation.getExecutablePath(launcher));
    }

    CommandBuilder agent(){
        cmds.add("agent");
        return this;
    }

    CommandBuilder join(String consulMasters){
        if (!consulMasters.isEmpty()) {
            for (String consulMaster : consulMasters.trim().split(",")) {
                cmds.add("-join");
                cmds.add(consulMaster.trim());
            }
        }
        return this;
    }

    CommandBuilder leave(){
        cmds.add("leave");
        return this;
    }

    CommandBuilder withToken(String token){
        if (!token.isEmpty()) {
            cmds.add("-token");
            cmds.add(token);
        }
        return this;
    }

    CommandBuilder withDatacenter(String datacenter){
        if (!datacenter.isEmpty()) {
            cmds.add("-datacenter");
            cmds.add(datacenter);
        }
        return this;
    }

    CommandBuilder withDatadir(String datadir){
        if (!datadir.isEmpty()) {
            cmds.add("-data-dir");
            cmds.add(datadir);
        }
        return this;
    }

    CommandBuilder withAdvertise(String address){
        if (!address.isEmpty()) {
            cmds.add("-advertise");
            cmds.add(address);
        }
        return this;
    }

    CommandBuilder getKv(String path){
        cmds.add("kv");
        cmds.add("Get");
        cmds.add(path);
        return this;
    }



    ArrayList<String> getCmds() {
        return cmds;
    }
}
