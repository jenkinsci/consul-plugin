package com.inneractive.jenkins.plugins.consul;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.EnvironmentContributingAction;

public class InjectVariable implements Action, EnvironmentContributingAction {
    private String key;
    private String value;

    public InjectVariable(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        if (envVars != null && key != null && value != null)
            envVars.put(key, value);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "InjectVariable";
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
