package com.inneractive.jenkins.plugins.consul;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

public class ConsulInstaller extends DownloadFromUrlInstaller {

    @DataBoundConstructor
    public ConsulInstaller(String id) {
        super(id);
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<ConsulInstaller>{

        @Override
        public String getDisplayName() {
            return "Install from hashicorp repo";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType.isInstance(ConsulInstallation.class);
        }

    }
}