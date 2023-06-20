package org.jfrog.bamboo;

import com.atlassian.bamboo.build.CustomPostBuildCompletedAction;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;

import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BambooUtils;

import java.io.File;
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
       FileUtils.deleteDirectory(BambooUtils.getJfrogSpecificBuildTmp(customVariableContext, buildContext));
       return buildContext;
    }

    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
