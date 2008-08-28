<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: runInfoDisplay.jsp,v 1.2 2008/08/28 03:55:08 dyoung Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<sv:head title="Run Information Log Display"></sv:head>
<link rel="stylesheet" href="css/runInfo.css"/>

<c:if test="${contents != null}">
	<sv:dataTable data="${contents}" cellpadding="2" cellspacing="2" styleClass="listTbl">
		<sv:dataTableRows rowVar="row">
			<sv:dataTableColumn title="Date/User" styleClass="listCell" value="${row.date}<br/>${row.user}" headerStyleClass="listTblHdr" width="100" />
			<sv:dataTableColumn title="Message" styleClass="listCell" value="${row.message}" headerStyleClass="listTblHdr" />
		</sv:dataTableRows>
	</sv:dataTable>
	<br/><br/><br/>
</c:if>

