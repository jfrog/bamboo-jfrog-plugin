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

<div class="paddedClearer"></div>
[@ww.form action=targetAction id="myform"
title='JFrog Platform Configuration'
descriptionKey=''
submitLabelKey='global.buttons.update'
cancelUri='/admin/jfrogConfiguration.action'
showActionErrors='true'
]
    [@ww.param name='buttons']
        [@ww.submit value="Test Connection" name="sendTest" /]
    [/@ww.param]

    [@ui.bambooSection]

        [@ww.textfield
            label='Server ID'
            description='Please specify a Server ID (name) to identify this server configuration'
            required="true"
            name="serverId"
            autofocus=true
        /]

        [@ww.textfield
            label='JFrog Platform URL'
            description='Please enter your JFrog Platform URL (example: https://acme.jfrog.io)'
            name="url"
            required="true"
        /]

       [@ww.radio fieldValue='token' label="Access Token" name='authentication' toggle='true' template='radio.ftl' /]
       [@ww.radio fieldValue='basic' label="Basic Authentication" name='authentication' toggle='true' template='radio.ftl'/]

        [@ui.bambooSection dependsOn='authentication' showOn='token']
            [@ww.password label='Access Token' name="accessToken" showPassword='true'/]
        [/@ui.bambooSection]

        [@ui.bambooSection dependsOn='authentication' showOn='basic']
            [@ww.textfield label='User' name="username"/]
            [@ww.password label='Password' name="password" showPassword='true'/]
        [/@ui.bambooSection]
        <br>
    [/@ui.bambooSection]
[/@ww.form]
</body>