package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BambooUtils {

    public static final String JFROG_PLUGIN_NAME = "bamboo-jfrog-plugin";

    public static String getJFrogPluginIdentifier(PluginAccessor pluginAccessor) {
        Plugin plugin = pluginAccessor.getPlugin("org.jfrog.bamboo." + JFROG_PLUGIN_NAME);
        if (plugin != null) {
            return JFROG_PLUGIN_NAME + "/" + plugin.getPluginInformation().getVersion();
        }
        return "";
    }

    /**
     * Return the JFrog specific Build folder and create it if needed.
     * Example - '~/bamboo-agent-home/temp/jfrog/MYP-CLIP-JOB1-16'
     *
     * @param customVariableContext - Task custom variables
     * @param buildContext         - The build context which holds the environment for the configuration.
     * @return JFrog specific Build folder
     */
    public static String getJfrogSpecificBuildTmp(CustomVariableContext customVariableContext, BuildContext buildContext) throws IOException {
        String bambooTemp = customVariableContext.getVariableContexts().get("tmp.directory").getValue();
        String fullBuildKey = buildContext.getResultKey().getKey();
        return Files.createDirectories(Paths.get(bambooTemp, "jfrog", fullBuildKey)).toString();
    }

}
