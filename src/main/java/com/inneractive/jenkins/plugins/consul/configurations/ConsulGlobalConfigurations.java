package com.inneractive.jenkins.plugins.consul.configurations;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ConsulGlobalConfigurations extends GlobalPluginConfiguration {
    @DataBoundConstructor
    public ConsulGlobalConfigurations() {
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {
        private JSONObject configurationsList;
        public List<ConsulClusterConfiguration.DescriptorImpl> getConsulClusterConfigurationDescriptor() {
            return Arrays.asList(new ConsulClusterConfiguration.DescriptorImpl());
        }

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            if (json != null && !json.isEmpty()) {
                this.configurationsList = json.getJSONObject("globalConsulConfigurations");
            } else {
                configurationsList = new JSONObject();
            }
            req.bindJSON(this, json);
            save();

            return super.configure(req, json);
        }

        public List<ConsulClusterConfiguration> getConfigurationsList() {
            List<ConsulClusterConfiguration> consulClusterConfigurations = new LinkedList<>();
            if (configurationsList != null && !configurationsList.isEmpty()) {
                Object configurations = configurationsList.get("configurationsList");
                if (configurations instanceof JSONArray) {
                    JSONArray configurationsJsonArray = (JSONArray) configurations;
                    for (int i = 0; i < configurationsJsonArray.size(); i++) {
                        consulClusterConfigurations.add(new ConsulClusterConfiguration(configurationsJsonArray.getJSONObject(i).getJSONObject("consulClusterConfigurations")));
                    }
                } else {
                    consulClusterConfigurations.add(new ConsulClusterConfiguration(((JSONObject) configurations).getJSONObject("consulClusterConfigurations")));
                }
            }
            return consulClusterConfigurations;
        }

        @Override
        public String getDisplayName() {
            return "Consul - global configurations";
        }

    }
}
