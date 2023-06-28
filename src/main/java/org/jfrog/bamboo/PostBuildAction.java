package org.jfrog.bamboo;

import com.atlassian.bamboo.build.CustomPostBuildCompletedAction;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;

import org.jfrog.bamboo.utils.BambooUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class PostBuildAction implements CustomPostBuildCompletedAction {
    private BuildContext buildContext;
    private CustomVariableContext customVariableContext;
    @Override
    public void init(final @NotNull BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public @NotNull BuildContext call() throws IOException {
       // Delete bamboo temp directory
       String fullBuildKey = buildContext.getResultKey().getKey();
       FileUtils.deleteDirectory(BambooUtils.getJfrogSpecificBuildTmp(customVariableContext, fullBuildKey));
       return buildContext;
    }

    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
