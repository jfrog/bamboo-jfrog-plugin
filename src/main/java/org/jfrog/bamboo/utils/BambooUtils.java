package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for common Bamboo operations.
 */
public class BambooUtils {

    public static final String JFROG_PLUGIN_NAME = "bamboo-jfrog-plugin";

    /**
     * Get the JFrog plugin identifier in the format "bamboo-jfrog-plugin/version".
     *
     * @param pluginAccessor The plugin accessor.
     * @return The JFrog plugin identifier.
     */
    public static String getJFrogPluginIdentifier(PluginAccessor pluginAccessor) {
        Plugin plugin = pluginAccessor.getPlugin("org.jfrog.bamboo." + JFROG_PLUGIN_NAME);
        if (plugin != null) {
            return JFROG_PLUGIN_NAME + "/" + plugin.getPluginInformation().getVersion();
        }
        return "";
    }

    /**
     * Return the JFrog-specific build folder and create it if needed.
     *
     * @param customVariableContext The task custom variables.
     * @return The JFrog-specific build folder.
     * @throws IOException If an I/O error occurs.
     */
    public static String getJfrogTmpDir(CustomVariableContext customVariableContext) throws IOException {
        String bambooTemp = customVariableContext.getVariableContexts().get("tmp.directory").getValue();
        Path jfrogSpecificBuildTmp = Files.createDirectories(Paths.get(bambooTemp, "jfrog"));
        return jfrogSpecificBuildTmp.toString();
    }

    /**
     * Return the JFrog-specific build folder and create it if needed.
     *
     * @param customVariableContext The task custom variables.
     * @param subdir                The subdirectory name.
     * @return The JFrog-specific build folder.
     * @throws IOException If an I/O error occurs.
     */
    public static String getJfrogTmpSubdir(CustomVariableContext customVariableContext, String subdir) throws IOException {
        String jfrogTemp = getJfrogTmpDir(customVariableContext);
        Path jfrogSpecificBuildTmp = Files.createDirectories(Paths.get(jfrogTemp, subdir));
        return jfrogSpecificBuildTmp.toString();
    }

    /**
     * Determines the build URL of this Bamboo instance.
     * This method is needed since we query the plugin's servlets for build information that isn't accessible to a remote agent.
     * The URL can generally be found in {@link AdministrationConfiguration}.
     *
     * @param fullBuildKey                        The full build key.
     * @param administrationConfiguration         The administration configuration.
     * @param administrationConfigurationAccessor The administration configuration accessor.
     * @return The Bamboo build URL if found.
     */
    public static String createBambooBuildUrl(String fullBuildKey, AdministrationConfiguration administrationConfiguration,
                                              AdministrationConfigurationAccessor administrationConfigurationAccessor) {
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
        return summaryUrl.append("browse/").append(EscapeChars.forFormSubmission(fullBuildKey)).toString();
    }
}
