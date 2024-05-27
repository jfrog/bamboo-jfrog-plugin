package org.jfrog.bamboo;

import com.atlassian.bamboo.build.CustomBuildProcessorServer;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BambooUtils;

import java.io.IOException;

/**
 * Post-build action that deletes the Bamboo temp directory.
 * Implements CustomBuildProcessorServer to run on the server
 * To make sure we have permissions and access to the temp folder we created.
 */
public class ServerPostBuildAction implements CustomBuildProcessorServer {
    private BuildContext buildContext;
    private CustomVariableContext customVariableContext;

    /**
     * Initializes the post-build action with the build context.
     *
     * @param buildContext The build context.
     */
    @Override
    public void init(final @NotNull BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    /**
     * Performs the post-build action.
     *
     * @return The modified build context.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public @NotNull BuildContext call() throws IOException {
        BambooUtils.DeleteJFrogTempDir(this.buildContext, this.customVariableContext);
        return buildContext;
    }

    /**
     * Sets the custom variable context.
     *
     * @param customVariableContext The custom variable context.
     */
    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
