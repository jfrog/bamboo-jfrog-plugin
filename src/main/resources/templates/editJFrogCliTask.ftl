[@ww.textfield
label="JFrog CLI command to run:"
required='true'
name="jfrog.task.cli.command"
/]

[@ww.select
name='jfrog.task.cli.serverid'
label="JFrog Platform ServerID to use:"
list=serverConfigManager.allServerConfigs
listKey='id'
listValue='url'
toggle='true'
required='true'
/]
