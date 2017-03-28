package com.inneractive.jenkins.plugins.consul;

import com.ecwid.consul.ConsulException;
import com.ecwid.consul.v1.ConsulClient;
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
    private ConsulInstallation consulInstallation;
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

    public ConsulInstallation getConsulInstallation() {
        return consulInstallation;
    }

    private String getMasters() {
        return (!consulMasters.isEmpty()) ? consulMasters : globalConsulConfigurationsDescriptor.getGlobalConsulMasters();
    }

    private String getDatacenter() {
        return (!consulDatacenter.isEmpty()) ? consulDatacenter : globalConsulConfigurationsDescriptor.getGlobalConsulDatacenter();
    }

    private String getToken() {
        return (!consulToken.isEmpty()) ? consulToken : globalConsulConfigurationsDescriptor.getGlobalConsulToken();
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

    private ConsulInstallation getInstallation(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException{
        for (ConsulInstallation i : getDescriptor().getInstallations()){
            if( installationName != null && installationName.equals(i.getName())){
                return (ConsulInstallation)i.translate(build, listener);
            }
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        setExecutable(build, launcher, listener);
        if (joinConsul(build, launcher, listener)) {
            for (ConsulOperation operation : operationList){
                operation.perform(build, launcher, listener);
            }
        } else{
            listener.getLogger().println("Couldn't connect to consul network.");
            return false;
        }
        killConsulAgent(build, launcher, listener);
        return true;
    }

    private void setExecutable(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        Node node = Computer.currentComputer().getNode();
        if (node != null) {
            ConsulInstallation ci = getInstallation(build, listener).forNode(node, listener);
            consulInstallation = ci.forEnvironment(build.getEnvironment(listener));
        } else {
            LOGGER.warning("Couldn't get jenkins node");
            consulInstallation = null;
        }
    }

    private boolean joinConsul(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        boolean success = false;
        String consulHomePath = consulInstallation.getHome();
        if (consulHomePath != null && !consulHomePath.isEmpty()){
            consulAgentProcess = launcher.launch().cmds(new CommandBuilder(consulInstallation, launcher).agent().withDatacenter(getDatacenter()).join(getMasters()).withToken(getToken()).withDatadir(consulHomePath).withAdvertise("127.0.0.1").getCmds()).envs(build.getEnvironment(listener)).stderr(listener.getLogger()).start();
        } else {
            LOGGER.severe("Couldn't get consul home directory");
            consulAgentProcess = null;
        }
        listener.getLogger().println("Waiting for agent to join...");
        for(int i=0; i<10 ; i++){
            try {
                if(i!=0)
                    listener.getLogger().println("Retry (" + i + ")");
                new ConsulClient("localhost").getStatusLeader();
                success = true;
                break;
            } catch (ConsulException e){
                Thread.sleep(100);
            }
        }
        return success;
    }

    private void killConsulAgent(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        launcher.launch().cmds(new CommandBuilder(consulInstallation, launcher).leave().getCmds()).envs(build.getEnvironment(listener)).join();
        if (consulAgentProcess.isAlive())
            consulAgentProcess.kill();
        consulAgentProcess = null;
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

        public ConsulInstallation[] getInstallations(){
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null){
                return ((ConsulInstallation.DescriptorImpl) jenkinsInstance.getDescriptor(ConsulInstallation.class)).getInstallations();
            } else{
                LOGGER.warning("Couldn't get jenkins instance");
            }
            return null;
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
            if (value.isEmpty() && globalConsulConfigurationsDescriptor.getGlobalConsulMasters().isEmpty())
                return FormValidation.error("Masters list is a mandatory field here if not configured in global jenkins configurations as well.");
            return FormValidation.ok();
        }

        public FormValidation doCheckConsulDatacenter(@QueryParameter String value){
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null)
                globalConsulConfigurationsDescriptor = ((ConsulGlobalConfigurations.DescriptorImpl)jenkinsInstance.getDescriptor(ConsulGlobalConfigurations.class));
            else
                LOGGER.warning("Couldn't get jenkins instance");
            if (value.isEmpty() && globalConsulConfigurationsDescriptor.getGlobalConsulDatacenter().isEmpty())
                return FormValidation.error("Datacenter is a mandatory field here if not configured in global jenkins configurations as well.");
            return FormValidation.ok();
        }
    }
}