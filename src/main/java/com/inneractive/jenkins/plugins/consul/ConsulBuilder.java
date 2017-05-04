package com.inneractive.jenkins.plugins.consul;

import com.inneractive.jenkins.plugins.consul.Util.ConsulUtil;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ConsulBuilder extends Builder {

    private static Logger LOGGER = Logger.getLogger(ConsulBuilder.class.getName());
    private String installationName;
    private String consulMasters;
    private String consulDatacenter;
    private String consulToken;
    private JSONObject overrideGlobalConsulConfigurations;
    private Proc consulAgentProcess;
    private List<ConsulOperation> operationList;
    private ConsulGlobalConfigurations.DescriptorImpl globalConsulConfigurationsDescriptor;

    @DataBoundConstructor
    public ConsulBuilder(String installationName, List<ConsulOperation> operationList) {
        this.installationName = installationName;


        if (operationList != null && !operationList.isEmpty()) {
            this.operationList = operationList;
        }
        else {
            this.operationList = Collections.emptyList();
        }

        Jenkins jenkinsInstance= Jenkins.getInstance();
        if (jenkinsInstance != null)
            globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl)jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
    }

    public JSONObject getOverrideGlobalConsulConfigurations() {
        return overrideGlobalConsulConfigurations;
    }

    public String getConsulMasters() {
        return consulMasters;
    }

    public String getConsulDatacenter() {
        return consulDatacenter;
    }

    public String getConsulToken() {
        return consulToken;
    }

    private String getMasters() {
        return globalConsulConfigurationsDescriptor.getConsulMasters(consulMasters);
    }

    private String getDatacenter() {
        return globalConsulConfigurationsDescriptor.getConsulDatacenter(consulDatacenter);
    }

    private String getToken() {
        return globalConsulConfigurationsDescriptor.getConsulToken(consulToken);
    }

    public List<ConsulOperation> getOperationList() {
        return operationList;
    }

    @DataBoundSetter
    public void setOperationList(List<ConsulOperation> operationList) {
        this.operationList = operationList;
    }

    @DataBoundSetter
    public void setOverrideGlobalConsulConfigurations(JSONObject overrideGlobalConsulConfigurations) {
        consulMasters = overrideGlobalConsulConfigurations.getString("consulMasters").replaceAll(" ", "");
        consulDatacenter = overrideGlobalConsulConfigurations.getString("consulDatacenter");
        consulToken = overrideGlobalConsulConfigurations.getString("consulToken");
        this.overrideGlobalConsulConfigurations = overrideGlobalConsulConfigurations;
    }

    @Override
    public ConsulBuilder.DescriptorImpl getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null) {
            return (ConsulBuilder.DescriptorImpl) jenkinsInstance.getDescriptorOrDie(ConsulBuilder.class);
        }
        else {
            LOGGER.warning("Couldn't get jenkins instance");
            return null;
        }

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        consulAgentProcess = ConsulUtil.joinConsul(build, launcher, listener,null, installationName, getConsulDatacenter(), getMasters(), getToken());
        if ( consulAgentProcess != null) {
            for (ConsulOperation operation : operationList){
                operation.perform(build, launcher, listener);
            }
        } else{
            listener.getLogger().println("Couldn't connect to consul network.");
            return false;
        }
        ConsulUtil.killConsulAgent(build, launcher, listener, null, installationName, consulAgentProcess);
        consulAgentProcess = null;
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        ConsulGlobalConfigurations.DescriptorImpl globalConsulConfigurationsDescriptor;

        public DescriptorImpl() {
            super(ConsulBuilder.class);
            load();
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Consul discovery";
        }

        public List<ConsulOperationDescriptor> getOperations() {
            return ConsulOperationDescriptor.all();
        }

        public FormValidation doCheckConsulMasters(@QueryParameter String value){
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null)
                globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl)jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
            else
                LOGGER.warning("Couldn't get jenkins instance");
            if (globalConsulConfigurationsDescriptor.getConsulMasters(value).isEmpty())
                return FormValidation.error("Masters list is a mandatory field here if not configured in global jenkins configurations as well.");
            return FormValidation.ok();
        }

        public FormValidation doCheckConsulDatacenter(@QueryParameter String value){
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null)
                globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl)jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
            else
                LOGGER.warning("Couldn't get jenkins instance");
            if (globalConsulConfigurationsDescriptor.getConsulDatacenter(value).isEmpty())
                return FormValidation.error("Datacenter is a mandatory field here if not configured in global jenkins configurations as well.");
            return FormValidation.ok();
        }
    }
}