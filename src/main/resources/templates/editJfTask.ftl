<br>
<div style="border: 2px solid #51a630; padding: 15px; width: fit-content;">

    [@ww.label
    value="🐸 JFrog CLI command to run:"
    description="Enter your JFrog CLI command after 'jf'. There is no need to provide the URL, credentials, Server ID or build flags. "
    /]

    [@ww.textarea
    name='jf.task.command'
    rows='2'
    cssStyle="max-width: 400px;"
    placeholder="jf <command>"
    required='true'
    /]
    <br>
    [@ww.label
    value="🐸 JFrog configuration to use:"
    description="Select one of the servers configured on the plugin configuration page."
    /]
    [@ww.select
    cssStyle="max-width: 400px; height: 31px;"
    name='jf.task.server.id'
    list=serverConfigManager.allServerConfigs
    listKey='serverId'
    toggle='true'
    required='true'
    /]
    <br>
    [@ww.label
    value="🐸 Working Directory (optional):"
    description="Enter a working directory path. Leave blank for current working directory."
    /]

    [@ww.textfield
    name='jf.task.working.directory'
    cssStyle="max-width: 400px;"
    /]

</div>
<br>