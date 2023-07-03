<br>
    [@ww.label
    cssClass="long-field"
    value="🐸 JFrog CLI command to run:"
    description="Enter your JFrog CLI command after 'jf'. There is no need to provide the URL, credentials, Server ID or build flags. "
    /]

    [@ww.textarea
    name='jf.task.command'
    rows='2'
    placeholder="jf <command>"
    required='true'
    cssClass="long-field"
    /]
    <br>
    [@ww.label
    value="🐸 JFrog configuration to use:"
    description="Select one of the servers configured on the plugin configuration page."
    /]
    [@ww.select
    cssClass="long-field"
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
    cssClass="long-field"
    name='jf.task.working.directory'
    /]

<br>