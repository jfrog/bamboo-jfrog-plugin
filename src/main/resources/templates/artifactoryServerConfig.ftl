[#if mode == 'edit' ]
    [#assign targetAction = 'updateServer']
    [#assign titleText = 'Edit JFrog Platform Configuration' /]
<html>
<head><title>Update Artifactory Server</title></head>
<body>
[#else]
    [#assign targetAction = 'createArtifactoryServer']
<html>
<head><title>Create Artifactory Server</title></head>
<body>
[/#if]

[#assign cancelUri = '/admin/jfrogConfig.action' /]

<div class="paddedClearer"></div>
[@ww.form action=targetAction id="myform"
          title='JFrog Platform Configuration'
          descriptionKey=''
          submitLabelKey='global.buttons.update'
          cancelUri='/admin/jfrogConfig.action'
          showActionErrors='true'
]
    [@ww.param name='buttons']
        [@ww.submit value="Test Connection" name="sendTest" /]
    [/@ww.param]


    [@ui.bambooSection]
        [@ww.textfield label='Server ID' required="true" name="serverId" autofocus=true/]
        [@ww.textfield label='JFrog Platform URL' name="url" required="true"/]
        <br>
        <div style="display: flex; gap: 20px;">
             [@ww.radio fieldValue='token' label="Access Token" name='auth' toggle='true' template='radio.ftl' /]
             [@ww.radio fieldValue='password' label="Basic Authentication" name='auth' cssStyle="margin: 0" toggle='true' template='radio.ftl'/]
        </div>

        [@ui.bambooSection dependsOn='auth' showOn='token']
            [@ww.password label='Access Token' name="accessToken"/]
        [/@ui.bambooSection]

        [@ui.bambooSection dependsOn='auth' showOn='password']
            [@ww.textfield label='User' name="username"/]
            [@ww.password label='Password' name="password" showPassword='true'/]
        [/@ui.bambooSection]

        [#--The Dummy password is a workaround for the autofill (Chrome)--]
        [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
    [/@ui.bambooSection]
[/@ww.form]
</body>