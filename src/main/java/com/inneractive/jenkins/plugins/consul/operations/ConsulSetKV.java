package com.inneractive.jenkins.plugins.consul.operations;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.inneractive.jenkins.plugins.consul.ConsulOperation;
import com.inneractive.jenkins.plugins.consul.ConsulOperationDescriptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * Created by lioz on 7/26/17.
 */
public class ConsulSetKV extends ConsulOperation {

    private final String valuePath;
    private final String value;

    @DataBoundConstructor
    public ConsulSetKV(String valuePath, String value) {
        this.valuePath = valuePath;
        this.value = value;
    }

    public String getValuePath() {
        return valuePath;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean perform(Run build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        String finalValue = build.getEnvironment(listener).get(this.value);
        if (finalValue == null || finalValue.isEmpty()){
            finalValue = value;
        }
        Response<Boolean> kvResponse = new ConsulClient("localhost").setKVValue(valuePath, value);
        return kvResponse.getValue();
    }

    @Override
    public String getOperationName() {
        return "SetKeyValueStore";
    }

    @Override
    public String getVariableName() {
        return value;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends ConsulOperationDescriptor {
        @Override
        public String getDisplayName() {
            return "Set a value in K/V store";
        }

        public FormValidation doCheckValuePath(@QueryParameter String value) {
            if (value.isEmpty())
                return FormValidation.error("Path is a mandatory field");
            return FormValidation.ok();
        }

        public FormValidation doCheckValue(@QueryParameter String value) {
            if (value.isEmpty())
                return FormValidation.error("Environment variable name is a mandatory field");
            return FormValidation.ok();
        }
    }
}