package com.inneractive.jenkins.plugins.consul.operations;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.model.HealthService;
import com.inneractive.jenkins.plugins.consul.ConsulOperation;
import com.inneractive.jenkins.plugins.consul.ConsulOperationDescriptor;
import com.inneractive.jenkins.plugins.consul.VariableInjectionAction;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.ArrayList;
import java.util.List;

public class ConsulServiceDiscoveryOperation extends ConsulOperation{

    private final String serviceName;
    private final String serviceTag;
    private final String environmentVariableName;
    private final String healthStatus;
    private final boolean addPort;


    @DataBoundConstructor
    public ConsulServiceDiscoveryOperation(String serviceName, String serviceTag, String environmentVariableName, String healthStatus, boolean addPort) {
        this.serviceName = serviceName;
        this.serviceTag = serviceTag;
        this.environmentVariableName = environmentVariableName;
        this.healthStatus = healthStatus;
        this.addPort = addPort;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceTag() {
        return serviceTag;
    }

    public String getEnvironmentVariableName() {
        return environmentVariableName;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public boolean isAddPort() {
        return addPort;
    }



    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        ArrayList<String> response = new ArrayList<>();
        List<HealthService> servicesList;
        switch (healthStatus){
            case "Healthy":
                servicesList = new ConsulClient("localhost").getHealthServices(serviceName, true, QueryParams.DEFAULT).getValue();
                break;
            case "Unhealthy":
                servicesList = new ConsulClient("localhost").getHealthServices(serviceName,false, QueryParams.DEFAULT).getValue();
                List<HealthService> servicesListTmp = new ArrayList<HealthService>();
                List<HealthService> healthyServicesList = new ConsulClient("localhost").getHealthServices(serviceName, true, QueryParams.DEFAULT).getValue();
                for ( HealthService service : servicesList)
                    servicesListTmp.add(service);
                for (HealthService origService : servicesListTmp){
                    for (HealthService cleanService : healthyServicesList){
                        if (origService.getNode().getAddress().equals(cleanService.getNode().getAddress()))
                            servicesList.remove(origService);
                    }
                }
                break;
            case "All":
                servicesList = new ConsulClient("localhost").getHealthServices(serviceName,false, QueryParams.DEFAULT).getValue();
                break;
            default:
                servicesList = new ArrayList<>();
                break;
        }
        for (HealthService node : servicesList) {
            String nodeDetails = "";
            if(serviceTag != null && !serviceTag.isEmpty()){
                if (node.getService().getTags().contains(serviceTag)){
                    nodeDetails = node.getNode().getAddress();
                }
            } else {
                nodeDetails = node.getNode().getAddress();
            }
            if (addPort){
                nodeDetails += ":" + node.getService().getPort();
            }
            response.add(nodeDetails);
        }
        if(environmentVariableName != null && !environmentVariableName.isEmpty()) {
            build.addAction(new VariableInjectionAction(environmentVariableName, response.toString()));
        } else{
            build.addAction(new VariableInjectionAction(serviceName, response.toString()));
        }
        return true;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends ConsulOperationDescriptor {
        @Override
        public String getDisplayName() {
            return "Service discovery query";
        }
    }
}
