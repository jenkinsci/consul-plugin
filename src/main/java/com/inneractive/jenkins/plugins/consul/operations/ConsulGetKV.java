package com.inneractive.jenkins.plugins.consul.operations;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.inneractive.jenkins.plugins.consul.ConsulOperation;
import com.inneractive.jenkins.plugins.consul.ConsulOperationDescriptor;
import com.inneractive.jenkins.plugins.consul.VariableInjectionAction;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ConsulGetKV extends ConsulOperation {
    private final String valuePath;
    private final String environmentVariableName;

    @DataBoundConstructor
    public ConsulGetKV(String valuePath, String environmentVariableName) {
        this.valuePath = valuePath;
        this.environmentVariableName = environmentVariableName;
    }

    public String getValuePath() {
        return valuePath;
    }

    public String getEnvironmentVariableName() {
        return environmentVariableName;
    }

    @Override
    public boolean perform(Run build, Launcher launcher, TaskListener listener) {
        Response<GetValue> kvResponse = new ConsulClient("localhost").getKVValue(valuePath);
        if (kvResponse.getValue() != null){
            String consulResponse = kvResponse.getValue().getDecodedValue();
            response.addProperty(valuePath , consulResponse);
            build.addAction(new VariableInjectionAction(environmentVariableName, consulResponse));
        }
        return true;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends ConsulOperationDescriptor {
        @Override
        public String getDisplayName() {
            return "Retrieve value from K/V store";
        }

        public FormValidation doCheckValuePath(@QueryParameter String value) {
            if (value.isEmpty())
                return FormValidation.error("Path is a mandatory field");
            return FormValidation.ok();
        }

        public FormValidation doCheckEnvironmentVariableName(@QueryParameter String value) {
            if (value.isEmpty())
                return FormValidation.error("Environment variable name is a mandatory field");
            return FormValidation.ok();
        }
    }

    @Override
    public String getOperationName() {
        return "ServiceDiscovery " + valuePath;
    }
}
