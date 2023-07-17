[#if mode == 'edit' ]
[#assign targetAction = 'updateServer']
[#assign titleText = 'Edit JFrog Platform Configuration' /]
<html>
<head><title>Update JFrog Server</title></head>
<body>
[#else]
[#assign targetAction = 'createJfrogServer']
<html>
<head><title>Create JFrog Server</title></head>
<body>
[/#if]

[#assign cancelUri = '/admin/jfrogConfiguration.action' /]

[@ww.form action=targetAction id="myform" title='JFrog Platform Configuration' descriptionKey='' submitLabelKey='global.buttons.update' cancelUri='/admin/jfrogConfiguration.action' showActionErrors='true']
    [@ww.param name='buttons']
        [@ww.submit value="Test Connection" name="testConnection" /]
    [/@ww.param]

    [@ui.bambooSection]
        [#if mode == 'edit']
            [@ww.hidden name='serverId'/]
        [#else]
            [@ww.textfield label='Server ID' required="true" name="serverId"  autofocus=true description='Please specify a Server ID (name) to identify this server configuration' /]
        [/#if]

        [@ww.textfield label='JFrog Platform URL' description='Please enter your JFrog Platform URL (example: https://acme.jfrog.io)' name="url" required="true"  /]

        [@ww.radio fieldValue='token' label="Access Token" name='authType' toggle='true' template='radio.ftl' /]
        [@ui.bambooSection dependsOn='authType' showOn='token']
            [@ww.password label='Access Token' name="accessToken" showPassword='true'/]
        [/@ui.bambooSection]

        [@ww.radio fieldValue='basic' label="Basic Authentication" name='authType' toggle='true' template='radio.ftl'/]
        [@ui.bambooSection dependsOn='authType' showOn='basic']
            [@ww.textfield label='Username' name="username"/]
            [@ww.password label='Password' name="password" showPassword='true'/]
        [/@ui.bambooSection]

        [@ww.radio fieldValue='noAuth' label="No Authentication" name='authType' toggle='true' template='radio.ftl'/]

        <br>

        [@ui.bambooSection title='JFrog CLI Settings' collapsible=true headerWeight="h3"]

            [@ui.bambooSection title='JFrog CLI Version']
                [@ww.radio fieldValue='false' label="Latest version" name='specificVersion' toggle='true' template='radio.ftl' /]
                [@ww.radio fieldValue='true' label="Select version" name='specificVersion' toggle='true' template='radio.ftl' /]
                [@ui.bambooSection dependsOn='specificVersion' showOn='true']
                    [@ww.textfield description='Enter specific JFrog CLI version' label='JFrog CLI Version'                        name="cliVersion"                    /]
                [/@ui.bambooSection]
            [/@ui.bambooSection]
            <br>
            [@ui.bambooSection title='JFrog CLI Download Source']
                [@ww.radio fieldValue='false' label="Download JFrog CLI from releases.jfrog.io" name='fromArtifactory' toggle='true' template='radio.ftl' /]
                [@ww.radio fieldValue='true' label="Download JFrog CLI from the configured Artifactory instance" name='fromArtifactory' toggle='true' template='radio.ftl' /]
                [@ui.bambooSection dependsOn='fromArtifactory' showOn='true']
                    [@ww.textfield description='Set the name of a Remote or a Virtual repository in your Artifactory instance, to download JFrog CLI from the Remote Repository set here (or the Remote Repository included in the Virtual Repository set here) should proxy https://releases.jfrog.io'                        label='Source Artifactory Repository'                        name="cliRepository"                    /]
                [/@ui.bambooSection]
            [/@ui.bambooSection]

        [/@ui.bambooSection]
        <br>
    [/@ui.bambooSection]
[/@ww.form]
</body>