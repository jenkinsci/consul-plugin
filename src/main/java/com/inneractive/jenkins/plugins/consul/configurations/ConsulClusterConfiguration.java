package com.inneractive.jenkins.plugins.consul.configurations;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

public class ConsulClusterConfiguration  implements ExtensionPoint, Describable<ConsulClusterConfiguration>, Serializable{
    private static Logger LOGGER = Logger.getLogger(ConsulClusterConfiguration.class.getName());

    private String profileName;
    private String mastersList;
    private String datacenter;
    private String token;

    @DataBoundConstructor
    public ConsulClusterConfiguration(JSONObject consulClusterConfigurations) {
        profileName = consulClusterConfigurations.getString("profileName");
        mastersList = consulClusterConfigurations.getString("mastersList");
        datacenter = consulClusterConfigurations.getString("datacenter");
        token = consulClusterConfigurations.getString("token");
    }

    public String getProfileName() {
        return profileName;
    }

    public String getMastersList() {
        return mastersList;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Descriptor<ConsulClusterConfiguration> getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null) {
            return (DescriptorImpl) jenkinsInstance.getDescriptorOrDie(getClass());
        } else {
            LOGGER.warning("Couldn't get jenkins instance");
            return null;
        }
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends Descriptor<ConsulClusterConfiguration> {
        @Override
        public String getDisplayName() {
            return "Settings profile";
        }

        public FormValidation doCheckProfileName(@QueryParameter String value) throws IOException, ServletException {
            if (value.isEmpty())
                return FormValidation.error("Profile name is a mandatory field");
            return FormValidation.ok();
        }

        public FormValidation doCheckMastersList(@QueryParameter String value) throws IOException, ServletException {
            if (value.isEmpty())
                return FormValidation.error("Masters list is a mandatory field");
            return FormValidation.ok();
        }

        public FormValidation doCheckDatacenter(@QueryParameter String value) throws IOException, ServletException {
            if (value.isEmpty())
                return FormValidation.error("Datacenter is a mandatory field, if unknown - use dc1 as default");
            return FormValidation.ok();
        }
    }
}
