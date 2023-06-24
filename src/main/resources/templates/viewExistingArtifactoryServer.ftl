<div class="toolbar">
    <div class="aui-toolbar inline">
        <ul class="toolbar-group">
            <li class="toolbar-item">
                <a class="toolbar-trigger"
                   href="[@s.url action='artifactoryServerConfig' namespace='/admin' /]">
                [@s.text name='artifactory.server.add' /]</a>
            </li>
        </ul>
    </div>
</div>

<br/>

[@ui.bambooPanel]

<div>
<table id="existingArtifactoryServer" class="aui">
    <thead>
    <tr>
        <th>Server ID</th>
        <th>JFrog Platform URL</th>
        <th>Username</th>
        <th class="operations">Operations</th>
    </tr>
    </thead>
    [#if action.getServerConfigs()?has_content]
        [#foreach serverConfig in serverConfigs]
            <tr>
                <td>
                    ${serverConfig.serverId}
                </td>
                <td>
                    <a href="${serverConfig.url}" target="_blank" >${serverConfig.url}</a>
                </td>
                <td>
                ${serverConfig.username}
                </td>
                <td class="operations">
                    <a id="editServer-${serverConfig.serverId}" href="[@ww.url action='editServer' serverId=serverConfig.serverId/]">
                        Edit
                    </a>
                    |
                    <a id="deleteServer-${serverConfig.serverId}"
                       href="[@ww.url action='confirmDeleteServer' serverId=serverConfig.serverId returnUrl=currentUrl/]"
                       class="delete" title="[@ww.text name='artifactory.server.delete' /]">[@ww.text name="global.buttons.delete" /]
                    </a>
                </td>
            </tr>
        [/#foreach]
    [#else]
        <tr>
            <td class="labelPrefixCell" colspan="4">
                [@ww.text name="artifactory.server.manage.none"/]
            </td>
        </tr>
    [/#if]
</table>
</div>
[/@ui.bambooPanel]

[@dj.simpleDialogForm triggerSelector=".delete" width=560 height=400 headerKey="artifactory.server.delete" submitCallback="reloadThePage"/]
