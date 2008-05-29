<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: genericUpload.jsp,v 1.1 2008/05/29 15:24:41 dyoung Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<sv:head title="File Upload Utility"></sv:head>

<link rel="stylesheet" href="css/genericUpload.css"/>

<c:choose>
	<c:when test='${fatalMsg ne "" and fatalMsg != null}'>
		${fatalMsg}
	</c:when>
	
	<c:otherwise>
		<table width="100%" height="95%" cellspacing="0" cellpadding="0">
			<tr>
				<td valign="top" width="175">
					<img class="mainImg" src="images/upload.png" width="130"/>
					
					<table width="80%" cellspacing="0" cellpadding="0">
						<tr>
							<td valign="top" align="center">
								<br/><br/><br/>	
								<c:if test='${uploadKey ne "" and uploadKey != null and (unauthorized eq "" or unauthorized == null)}'>
									<a class="pageNav" href="?appl=${appl}&config=${config}">Return to List</a>
								</c:if>
							</td>
					</table>
				</td>
				<td valign="top" class="bodyText">
					<c:if test='${(uploadKey eq "" or uploadKey == null) or unauthorized eq "true"}'>
						<form method="post" name="frm">
							<input type="hidden" name="action" value="choose"/>
					</c:if>
					<c:if test='${uploadKey ne "" and uploadKey != null and (unauthorized eq "" or unauthorized == null)}'>
						<form method="post" name="frm" enctype="multipart/form-data">
							<input type="hidden" name="action" value="upload"/>
					</c:if>
			
							<input type="hidden" name="appl" value="${appl}"/>
							<input type="hidden" name="config" value="${config}"/>
							<input type="hidden" name="uploadKey" value="${uploadKey}"/>

							<table width="100%" cellspacing="0" cellpadding="0">
								<tr><td class="uploadHdr"><strong>File Upload Utility ${uploadKeyDisplay}</strong></td></tr>
								<tr><td class="uploadTitle">Welcome ${winIsLoggedIn.name}!</td></tr>
							</table>
							<p/>
					

					<c:if test='${(uploadKey eq "" or uploadKey == null) or unauthorized eq "true"}'>
							Select the upload type from the list below.  If authorized, you will be taken to the upload screen.<p/>
							<ul>
								<c:forEach items="${uploadKeys}" var="key">
									<li><a class="uploadKey" href="?appl=${appl}&config=${config}&uploadKey=${key}">${key}</a><br/></li>
								</c:forEach>
							</ul>
					</c:if>

					<c:if test='${uploadKey ne "" and uploadKey != null and (unauthorized eq "" or unauthorized == null)}'>
							Select the file to upload then click the "Upload" button below.
							<table width="100%" cellspacing="0" cellpadding="4">
								<tr><td><input class="fileInput" type="file" name="hrFile" size="100" /></td></tr>
							</table>
							<button onclick="frm.submit();">Upload</button>
							<br/><br/><br/><br/>
	
							<span class="fileListHdr">Files Already In Directory</span>
							<br/>
							<em>(if no files expected, contact Technical Support)</em>
							<p/>
						
							
							<sv:dataTable data="${fileList}" cellpadding="2" cellspacing="2" styleClass="listTbl">
								<sv:dataTableRows rowVar="row">
									<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" headerStyleClass="listTblHdr" />
									<sv:dataTableColumn title="Date of Upload" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" />
									
								</sv:dataTableRows>
							</sv:dataTable>
					</c:if>
										
					<c:if test='${message ne "" and message != null}'>
						<br/><br/>
						<table width="100%" cellspacing="0" cellpadding="0">
							<tr height="25"><td></td></tr>
							<tr><td class="bodyText">Messages:</td></tr>
							<tr><td class="messageText">${message}</td></tr>
						</table>
					</c:if>	
					<c:if test='${errMessage ne "" and errMessage != null}'>
						<br/><br/>
						<table width="100%" cellspacing="0" cellpadding="0">
							<tr height="25"><td></td></tr>
							<tr><td class="bodyText">Messages:</td></tr>
							<tr><td class="messageTextError">${errMessage}</td></tr>
						</table>
					</c:if>		
						
					</form>
				</td>
			</tr>
			<tr>
				<td colspan=2><img src="common/images/savvisLogo.jpg" width="300" align="right"/></td>
			</tr>
		</table>
	</c:otherwise>
</c:choose>
