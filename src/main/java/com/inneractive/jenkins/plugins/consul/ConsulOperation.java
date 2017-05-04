package com.inneractive.jenkins.plugins.consul;

import com.google.gson.JsonObject;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.*;
import jenkins.model.Jenkins;
import java.util.logging.Logger;

public abstract class ConsulOperation implements ExtensionPoint, Describable<ConsulOperation> {
    private static Logger LOGGER = Logger.getLogger(ConsulOperation.class.getName());
    protected JsonObject response = new JsonObject();

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

    public abstract boolean perform(Run build, Launcher launcher, TaskListener listener);

    public abstract String getOperationName();

    public JsonObject getResponse() {
        return response;
    }
}
