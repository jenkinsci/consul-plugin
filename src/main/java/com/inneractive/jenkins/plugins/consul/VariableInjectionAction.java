package com.inneractive.jenkins.plugins.consul;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

public class VariableInjectionAction implements EnvironmentContributingAction {

    private String key;
    private String value;

    public VariableInjectionAction(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void buildEnvVars(AbstractBuild build, EnvVars envVars) {

        if (envVars != null && key != null && value != null) {
            envVars.put(key, value);
        }
    }

    public String getDisplayName() {
        return "VariableInjectionAction";
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}