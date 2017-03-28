package com.inneractive.jenkins.plugins.consul;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ConsulGlobalConfigurations extends GlobalPluginConfiguration {

    @DataBoundConstructor
    public ConsulGlobalConfigurations() {
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {
        private String globalConsulMasters;
        private String globalConsulDatacenter;
        private String globalConsulToken;

        public DescriptorImpl() {
            load();
        }

        public String getGlobalConsulMasters() {
            return globalConsulMasters;
        }

        public String getGlobalConsulDatacenter() {
            return globalConsulDatacenter;
        }

        public String getGlobalConsulToken() {
            return globalConsulToken;
        }

        public void setGlobalConsulMasters(String globalConsulMasters) {
            this.globalConsulMasters = globalConsulMasters;
        }

        public void setGlobalConsulDatacenter(String globalConsulDatacenter) {
            this.globalConsulDatacenter = globalConsulDatacenter;
        }

        public void setGlobalConsulToken(String globalConsulToken) {
            this.globalConsulToken = globalConsulToken;
        }

        @Override
        public String getDisplayName() {
            return "Consul - global configurations";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONObject globalConsulConfig = (JSONObject) json.get("globalConsulConfigurations");
            globalConsulMasters = globalConsulConfig.getString("globalConsulMasters");
            globalConsulDatacenter = globalConsulConfig.getString("globalConsulDatacenter");
            globalConsulToken = globalConsulConfig.getString("globalConsulToken");

            req.bindJSON(this, json);
            save();

            return super.configure(req, json);
        }

        public FormValidation doCheckGlobalConsulMasters (@QueryParameter String value){
            if (value.isEmpty())
                return FormValidation.warning("Masters list is a mandatory field. You will have to configure it in every job.");
            return FormValidation.ok();
        }

        public FormValidation doCheckGlobalConsulDatacenter(@QueryParameter String value){
            if (value.isEmpty())
                return FormValidation.warning("Datacenter is mandatory field, use dc1 if you never configured it in consul or configure it manually for every job.");
            return FormValidation.ok();
        }
    }
}
