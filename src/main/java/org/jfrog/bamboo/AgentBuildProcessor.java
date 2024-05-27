package org.jfrog.bamboo;

import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BambooUtils;

import java.io.IOException;

/**
 * Post-build action that deletes the temporary JFrog build directory,
 * executed on the agent side (remote or local) to make sure we have
 * access to the temp folder we created.
 */
public class AgentBuildProcessor implements CustomBuildProcessor {
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
        String fullBuildKey = buildContext.getResultKey().getKey();
        FileUtils.deleteDirectory(BambooUtils.getJfrogTmpSubdir(customVariableContext, fullBuildKey));
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
