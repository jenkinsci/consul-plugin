package com.inneractive.jenkins.plugins.consul;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import java.util.logging.Logger;

public abstract class ConsulOperation implements ExtensionPoint, Describable<ConsulOperation> {
    private static Logger LOGGER = Logger.getLogger(ConsulOperation.class.getName());

    @Override
    public Descriptor<ConsulOperation> getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null) {
            return (ConsulOperationDescriptor) jenkinsInstance.getDescriptorOrDie(getClass());
        } else {
            LOGGER.warning("Couldn't get jenkins instance");
            return null;
        }
    }

    public abstract boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener);
}
