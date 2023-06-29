<title>[@ww.text name="jfrogServer.delete" /]</title>
[@ww.form action="deleteServer" namespace="/admin"
submitLabelKey="global.buttons.delete"
id="confirmDelete"]
    [@s.hidden name="returnUrl" /]
    [@s.hidden name="serverId" /]
    [@ui.messageBox type="warning" title="Are you sure you want to delete this JFrog Platform configuration?" /]

    <br/>

[/@ww.form]
