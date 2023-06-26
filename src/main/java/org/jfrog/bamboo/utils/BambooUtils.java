package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.utils.EscapeChars;
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
     * @param fullBuildKey         - The full build key.
     * @return JFrog specific Build folder
     */
    public static String getJfrogSpecificBuildTmp(CustomVariableContext customVariableContext, String fullBuildKey) throws IOException {
        String bambooTemp = customVariableContext.getVariableContexts().get("tmp.directory").getValue();
        return Files.createDirectories(Paths.get(bambooTemp, "jfrog", fullBuildKey)).toString();
    }

    /**
     * Determines the build URL of this Bamboo instance.<br> This method is needed since we query the plugin's servlets
     * for build information that isn't accessible to a remote agent.<br> The URL can generally be found in {@link
     * com.atlassian.bamboo.configuration.AdministrationConfiguration}
     *
     * @return Bamboo build URL if found. Null if running in an un-recognized type of agent.
     */
    public static String createBambooBuildUrl(String fullBuildKey, AdministrationConfiguration administrationConfiguration, AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        String url = "";
        if (administrationConfiguration != null) {
            url = administrationConfiguration.getBaseUrl();
        } else if (administrationConfigurationAccessor != null) {
            url = administrationConfigurationAccessor.getAdministrationConfiguration().getBaseUrl();
        }

        StringBuilder summaryUrl = new StringBuilder(url);
        if (!url.endsWith("/")) {
            summaryUrl.append("/");
        }
        return summaryUrl.append("browse/").
                append(EscapeChars.forFormSubmission(fullBuildKey)).toString();
    }
}
