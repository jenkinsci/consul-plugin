package com.inneractive.jenkins.plugins.consul.Util;

import com.ecwid.consul.ConsulException;
import com.ecwid.consul.v1.ConsulClient;
import com.inneractive.jenkins.plugins.consul.ConsulInstallation;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import java.io.IOException;

public abstract class ConsulUtil {

    private static ConsulInstallation getConsulInstallation(Run build, final Launcher launcher, final TaskListener listener, FilePath filePath, String installationName) throws IOException, InterruptedException {
        Node node;
        if (filePath != null)
            node = filePath.toComputer().getNode();
        else
            node = Computer.currentComputer().getNode();
        if (node != null) {
            ConsulInstallation ci = getInstallation(build, listener, installationName).forNode(node, listener);
            return  ci.forEnvironment(build.getEnvironment(listener));
        } else {
            return null;
        }
    }

    public static ConsulInstallation[] getInstallations(){
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null) {
            ConsulInstallation[] consulInstallations = ((ConsulInstallation.DescriptorImpl) jenkinsInstance.getDescriptor(ConsulInstallation.class)).getInstallations();
            return consulInstallations;
        }
        return null;
    }

    private static ConsulInstallation getInstallation(Run build, TaskListener listener, String installationName) throws IOException, InterruptedException{
        ConsulInstallation[] consulInstallations = getInstallations();
        if (consulInstallations != null){
            for (ConsulInstallation i : consulInstallations){
                if( installationName != null && installationName.equals(i.getName())){
                    return (ConsulInstallation)i;
                }
            }
        }
        return null;
    }

    public static Proc joinConsul(Run build, final Launcher launcher, final TaskListener listener, FilePath filePath, String installationName, String dataCenter, String masters, String token) throws IOException, InterruptedException {
        ConsulInstallation consulInstallation = getConsulInstallation(build, launcher, listener, filePath, installationName);
        Proc consulAgentProcess;
        boolean success = false;
        String consulHomePath = consulInstallation.getHome();
        if (consulHomePath!= null && !consulHomePath.isEmpty()){
            consulAgentProcess = launcher.launch().cmds(new CommandBuilder(consulInstallation, launcher).agent().withDatacenter(dataCenter).join(masters).withToken(token).withDatadir(consulHomePath).withAdvertise("127.0.0.1").getCmds()).envs(build.getEnvironment(listener)).stderr(listener.getLogger()).start();
        } else {
            listener.getLogger().println("Couldn't get consul home directory");
            consulAgentProcess = null;
        }
        listener.getLogger().println("Waiting for agent to join...");
        for(int i=0; i<10 ; i++){
            try {
                if(i!=0)
                    listener.getLogger().println("Retry (" + i + ")");
                new ConsulClient("localhost").getStatusLeader();
                success = true;
                break;
            } catch (ConsulException e){
                Thread.sleep(100);
            }
        }
        if (success) {
            if (consulAgentProcess != null)
                return consulAgentProcess;
        } else {
            if (consulAgentProcess != null){
                consulAgentProcess.kill();
            }
            return null;
        }
        return null;
    }

    public static void killConsulAgent(Run build, final Launcher launcher, final TaskListener listener, FilePath filePath,  String installationName, Proc consulAgentProcess) throws IOException, InterruptedException {
        ConsulInstallation consulInstallation = getConsulInstallation(build, launcher, listener, filePath, installationName);
        launcher.launch().cmds(new CommandBuilder(consulInstallation, launcher).leave().getCmds()).envs(build.getEnvironment(listener)).join();
        if (consulAgentProcess.isAlive())
            consulAgentProcess.kill();
    }
}
