package com.inneractive.jenkins.plugins.consul.Steps;

import com.google.common.collect.ImmutableSet;
import com.inneractive.jenkins.plugins.consul.*;
import com.inneractive.jenkins.plugins.consul.Util.ConsulUtil;
import com.inneractive.jenkins.plugins.consul.configurations.ConsulClusterConfiguration;
import com.inneractive.jenkins.plugins.consul.configurations.ConsulGlobalConfigurations;
import com.inneractive.jenkins.plugins.consul.operations.ConsulServiceDiscoveryOperation;
import hudson.*;
import hudson.model.*;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ConsulOperationStep extends Step implements Serializable {
    private static Logger LOGGER = Logger.getLogger(ConsulOperationStep.class.getName());
    private String installationName;
    private ArrayList<ConsulOperation> operationList;
    private String consulSettingsProfileName;
    private ConsulClusterConfiguration consulClusterConfigurationProfile;

    @DataBoundConstructor
    public ConsulOperationStep(String installationName, String consulSettingsProfileName) {
        this.installationName = installationName;
        operationList = new ArrayList<>();
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

    @DataBoundSetter
    public void setOperationList(ArrayList<ConsulOperation> operationList) {
        this.operationList = operationList;
    }

    public String getInstallationName() {
        return installationName;
    }

    public ArrayList<ConsulOperation> getOperationList() {
        return operationList;
    }

    public String getConsulSettingsProfileName() {
        return consulSettingsProfileName;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    private static class Execution extends SynchronousStepExecution<String> {
        private Run run;
        private TaskListener taskListener;
        private FilePath filePath;
        private Launcher launcher;
        ConsulOperationStep step;
        Proc consulAgentProcess;

        Execution(final ConsulOperationStep step, final @Nonnull StepContext context) throws IOException, InterruptedException {
            super(context);
            run = context.get(Run.class);
            taskListener = context.get(TaskListener.class);
            filePath = context.get(FilePath.class);
            launcher = context.get(Launcher.class);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            JSONObject jsonObject= new JSONObject();
            consulAgentProcess = ConsulUtil.joinConsul(run, launcher, taskListener, filePath, step.installationName, step.consulClusterConfigurationProfile.getDatacenter(), step.consulClusterConfigurationProfile.getMastersList(), step.consulClusterConfigurationProfile.getToken());
            if ( consulAgentProcess != null) {
                for (ConsulOperation operation : step.operationList){
                    operation.perform(run, launcher, taskListener);
                    if (jsonObject.has(operation.getOperationName())){
                        jsonObject.getJSONObject(operation.getOperationName()).put(operation.getVariableName(), operation.getResponse().get(((ConsulServiceDiscoveryOperation) operation).getServiceName()));
                    } else {
                        jsonObject.put(operation.getOperationName(), operation.getResponse());
                    }
                }
            } else{
                taskListener.getLogger().println("Couldn't connect to consul network.");
                return "";
            }
            ConsulUtil.killConsulAgent(run, launcher, taskListener, filePath, step.installationName, consulAgentProcess);
            consulAgentProcess = null;
            return jsonObject.toString();
        }

    }

    @Extension(optional = true)
    public static class ConsulStepDescriptor extends StepDescriptor {

        public List<ConsulOperationDescriptor> getOperations() {
            return ConsulOperationDescriptor.all();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, EnvVars.class, Launcher.class, FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "Consul";
        }

        @Override
        public String getDisplayName() {
            return "ConsulStep";
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
