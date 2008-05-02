<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: hartfordUpload.jsp,v 1.2 2008/05/02 17:51:39 dyoung Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 

<html>

<sv:head title="Hartford File Upload"></sv:head>

<form method="post">
	<sv:input type="hidden" name="action" value="upload"/>
	
	<table width="100%" cellspacing="0" cellpadding="0">
		<tr>
			<td valign="top" width="100%">
				<table>
					<tr><td class="quickHdr">Welcome!</td></tr>
					<tr><td class="quickText"><strong>Hartford File Upload Utility</strong></td></tr>	
					
				</table>
			</td>
		</tr>
		<tr height="25"><td></td></tr>
		<tr>
			<td valign="top" width="40%">
	
				<table class="listTbl" width="100%" cellspacing="0" cellpadding="4">
					<tr>
						<td width="150">Select file to upload:</td>
						<td>
							<sv:input type="file" name="hartfordFile" size="80" />		
						</td>
					</tr>
				</table>
				
			</td>
		</tr>
		<tr>
			<td><sv:input type="submit" onclick="document.forms[0].encoding = 'multipart/form-data'; svSubmitAction('upload'); return false;" name="Upload" value="Upload"/></td>
		</tr>
		<tr height="25"><td></td></tr>
		<tr>
			<td>Pending files:<br/>
				<table class="listTbl" width="60%" cellspacing="0" cellpadding="2" border="1">
					<tr>
						<td>Filename</td>
						<td>Last Modified</td>
					</tr>
									
					<c:forEach items="${fileList}" var="file">
						<tr>
							<td>${file.name}</td>
							<td>${file.lastModified}</td>
						</tr>
					</c:forEach>
				</table>
			</td>
		</tr>
		<tr height="25"><td></td></tr>
		<tr>
			<td>Messages:<br/>
				<table class="listTbl" width="100%" cellspacing="0" cellpadding="4">
					<tr>
						<td><em>${message}</em></td>
					</tr>
				</table>
			</td>
		</tr>
	</table>			
</form>
