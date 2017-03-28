package com.inneractive.jenkins.plugins.consul;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ConsulInstallation extends ToolInstallation implements EnvironmentSpecific<ConsulInstallation>, NodeSpecific<ConsulInstallation>{
    private static Logger LOGGER = Logger.getLogger(ConsulInstallation.class.getName());
    private static final String UNIX_EXECUTABLE = "consul";
    private static final String WINDOWS_EXECUTABLE = "consul.exe";

    @DataBoundConstructor
    public ConsulInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public ConsulInstallation forEnvironment(EnvVars envVars) {
        return new ConsulInstallation(getName(), envVars.expand(getHome()), getProperties().toList());
    }

    @Override
    public ConsulInstallation forNode(@NonNull Node node, TaskListener taskListener) throws IOException, InterruptedException {
        return new ConsulInstallation(getName(), translateFor(node, taskListener), getProperties().toList());
    }

    protected String getExecutableFilename() {
        return Functions.isWindows() ? WINDOWS_EXECUTABLE : UNIX_EXECUTABLE;
    }

    public String getExecutablePath(final Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            @Override
            public void checkRoles(RoleChecker roleChecker) throws SecurityException {

            }

            public String call() throws IOException {
                String consulHomePath = getHome();
                if (consulHomePath != null && !consulHomePath.isEmpty()){
                    FilePath homeDirectory = new FilePath(new File(consulHomePath));

                    try {
                        if (!(homeDirectory.exists() && homeDirectory.isDirectory()))
                            throw new FileNotFoundException("Couldn't find home directory " + homeDirectory);
                    } catch (InterruptedException ex) {
                        throw new IOException(ex);
                    }

                    FilePath executable = new FilePath(homeDirectory, getExecutableFilename());

                    try {
                        if (!executable.exists())
                            throw new FileNotFoundException("Couldn't find executable file in path " + homeDirectory);
                    } catch (InterruptedException ex) {
                        throw new IOException(ex);
                    }

                    return executable.getRemote();
                } else {
                    LOGGER.warning("Couldn't find home directory for consul");
                    return null;
                }
            }
        });
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<ConsulInstallation>{

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Consul";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new ConsulInstaller(null));
        }

        @Override
        public void setInstallations(ConsulInstallation... installations) {
            super.setInstallations(installations);
            save();
        }
    }
}
