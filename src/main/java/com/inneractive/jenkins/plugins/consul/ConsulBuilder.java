package com.inneractive.jenkins.plugins.consul;

import com.inneractive.jenkins.plugins.consul.Util.ConsulUtil;
import com.inneractive.jenkins.plugins.consul.configurations.ConsulClusterConfiguration;
import com.inneractive.jenkins.plugins.consul.configurations.ConsulGlobalConfigurations;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ConsulBuilder extends Builder {
    private static Logger LOGGER = Logger.getLogger(ConsulBuilder.class.getName());
    private String installationName;
    private List<ConsulOperation> operationList;
    private String consulSettingsProfileName;
    private ConsulClusterConfiguration consulClusterConfigurationProfile;

    @DataBoundConstructor
    public ConsulBuilder(String installationName, List<ConsulOperation> operationList, String consulSettingsProfileName) {
        this.installationName = installationName;


        if (operationList != null && !operationList.isEmpty()) {
            this.operationList = operationList;
        }
        else {
            this.operationList = Collections.emptyList();
        }

        this.consulSettingsProfileName = consulSettingsProfileName;

        Jenkins jenkinsInstance= Jenkins.getInstance();
        if (jenkinsInstance != null) {
            ConsulGlobalConfigurations.DescriptorImpl globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl) jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
            for(ConsulClusterConfiguration consulClusterConfiguration: globalConsulConfigurationsDescriptor.getConfigurationsList()){
                if (consulClusterConfiguration.getProfileName().equals(consulSettingsProfileName)){
                    consulClusterConfigurationProfile = consulClusterConfiguration;
                }
            }
        } else {
            LOGGER.warning("Couldn't get jenkins instance");
        }
    }

    public ConsulInstallation[] getConsulInstallations(){
        return ConsulUtil.getInstallations();
    }

    public List<ConsulOperation> getOperationList() {
        return operationList;
    }

    public String getConsulSettingsProfileName() {
        return consulSettingsProfileName;
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
        Proc consulAgentProcess = ConsulUtil.joinConsul(build, launcher, listener, null, installationName, consulClusterConfigurationProfile.getDatacenter(), consulClusterConfigurationProfile.getMastersList(), consulClusterConfigurationProfile.getToken());
        if ( consulAgentProcess != null) {
            for (ConsulOperation operation : operationList){
                operation.perform(build, launcher, listener);
            }
        } else{
            listener.getLogger().println("Couldn't connect to consul network.");
            return false;
        }
        ConsulUtil.killConsulAgent(build, launcher, listener, null, installationName, consulAgentProcess);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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

        public ConsulInstallation[] getConsulInstallations(){
            return ConsulUtil.getInstallations();
        }

        public ListBoxModel doFillConsulSettingsProfileNameItems() {
            ConsulGlobalConfigurations.DescriptorImpl globalConsulConfigurationsDescriptor;
            Jenkins jenkinsInstance= Jenkins.getInstance();
            if (jenkinsInstance != null) {
                globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl) jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
            } else {
                LOGGER.warning("Couldn't get jenkins instance");
                return null;
            }

            ListBoxModel items = new ListBoxModel();
            for (ConsulClusterConfiguration consulClusterConfiguration: globalConsulConfigurationsDescriptor.getConfigurationsList()) {
                items.add(consulClusterConfiguration.getProfileName());
            }
            return items;
        }
    }
}