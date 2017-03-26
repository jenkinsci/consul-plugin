package com.inneractive.jenkins.plugins.consul;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class ConsulOperationDescriptor extends Descriptor<ConsulOperation> {
    private static Logger LOGGER = Logger.getLogger(ConsulOperation.class.getName());

    public boolean isApplicable(Class<? extends AbstractProject<?,?>> jobType) {
        return true;
    }

    public static List<ConsulOperationDescriptor> all(Class<? extends AbstractProject<?,?>> jobType) {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null){
            List<ConsulOperationDescriptor> alldescs = jenkinsInstance.getDescriptorList(ConsulOperation.class);
            List<ConsulOperationDescriptor> descs = new ArrayList<ConsulOperationDescriptor>();
            for (ConsulOperationDescriptor d: alldescs) {
                if (jobType == null || d.isApplicable(jobType)) {
                    descs.add(d);
                }
            }
            return descs;
        } else {
            LOGGER.warning("Couldn't get jenkins instance");
            return null;
        }
    }

    public static List<ConsulOperationDescriptor> all() {
        return all(null);
    }
}
