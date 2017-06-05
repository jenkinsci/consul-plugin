package com.inneractive.jenkins.plugins.consul;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import java.io.Serializable;
import java.util.logging.Logger;

public abstract class ConsulOperation implements ExtensionPoint, Describable<ConsulOperation>, Serializable{
    private static Logger LOGGER = Logger.getLogger(ConsulOperation.class.getName());
    protected JSONObject response = new JSONObject();

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

    public abstract String getVariableName();

    public JSONObject getResponse() {
        return response;
    }
}
