<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: genericUpload.jsp,v 1.2 2008/07/28 19:59:11 dyoung Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<sv:head title="File Upload Utility"></sv:head>

<link rel="stylesheet" href="css/genericUpload.css"/>

<form name="downloadForm" method="post">
	<input type="hidden" name="appl" value="${appl}"/>
	<input type="hidden" name="config" value="${config}"/>
	<input type="hidden" name="key" value="${key}"/>
	<input type="hidden" name="download" value="1"/>
	<input type="hidden" name="file" value=""/>
	<input type="hidden" name="path" value=""/>
	<input type="hidden" name="src" value=""/>
</form>

<form name="moveFileForm" method="post">
	<input type="hidden" name="appl" value="${appl}"/>
	<input type="hidden" name="config" value="${config}"/>
	<input type="hidden" name="key" value="${key}"/>
	<input type="hidden" name="errorArchive" value="1"/>
	<input type="hidden" name="file" value=""/>
	<input type="hidden" name="path" value=""/>
	<input type="hidden" name="src" value=""/>
</form>

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
								<br/><br/>	
								<c:if test='${key ne "" and key != null and (unauthorized eq "" or unauthorized == null)}'>
									<a class="pageNav" href="?appl=${appl}&config=${config}&key=${key}">Refresh This Page</a>
									<br/><br/>
									<a class="pageNav" href="?appl=${appl}&config=${config}">Return to Upload List</a>
								</c:if>
							</td>
					</table>
				</td>
				<td valign="top" class="bodyText">
					<c:if test='${(key eq "" or key == null) or unauthorized eq "true"}'>
						<form method="post" name="frm">
							<input type="hidden" name="action" value="choose"/>
					</c:if>
					<c:if test='${key ne "" and key != null and (unauthorized eq "" or unauthorized == null)}'>
						<form method="post" name="frm" enctype="multipart/form-data">
							<input type="hidden" name="action" value="upload"/>
					</c:if>
			
							<input type="hidden" name="appl" value="${appl}"/>
							<input type="hidden" name="config" value="${config}"/>
							<input type="hidden" name="key" value="${key}"/>

							<table width="100%" cellspacing="0" cellpadding="0">
								<tr><td class="uploadHdr"><strong>File Upload Utility ${uploadKeyDisplay}</strong></td></tr>
								<tr><td class="uploadTitle">Welcome ${winIsLoggedIn.name}!</td></tr>
							</table>
							<br/>
					

					<c:if test='${(key eq "" or key == null) or unauthorized eq "true"}'>
						Select the upload type from the list below.  If authorized, you will be taken to the upload screen.<p/>
						<ul>
							<c:forEach items="${uploadKeys}" var="key">
								<li><a class="uploadKey" href="?appl=${appl}&config=${config}&key=${key}">${key}</a><br/></li>
							</c:forEach>
						</ul>

						<c:if test='${message ne "" and message != null}'>
							<p/>
							<table width="100%" cellspacing="0" cellpadding="0">
								<tr height="25"><td></td></tr>
								<tr><td class="bodyText">Messages:</td></tr>
								<tr><td class="messageText">${message}</td></tr>
							</table>
						</c:if>	
						<c:if test='${errMessage ne "" and errMessage != null}'>
							<table width="100%" cellspacing="0" cellpadding="0">
								<tr height="25"><td></td></tr>
								<tr><td class="bodyText">Messages:</td></tr>
								<tr><td class="messageTextError">${errMessage}</td></tr>
							</table>
						</c:if>		
							
					</c:if>
					
					<c:if test='${key ne "" and key != null and (unauthorized eq "" or unauthorized == null)}'>
					
						<table width="100%" cellspacing="0" cellpadding="5">
							<tr>
								<td width="50%" valign="top">
									<br/>
									<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
										<tr><th class="listTblHdr">Upload A File</th></tr>
										<tr><td style="width: 60%;" class="listCell"><input class="fileInput" type="file" name="hrFile"/></td></tr>
									</table>
									<button onclick="frm.submit();">Upload</button>
									<br/><br/><br/>
									
									<c:if test='${message ne "" and message != null}'>
										<table width="100%" cellspacing="0" cellpadding="0">
											<tr><td class="bodyText">Messages:</td></tr>
											<tr><td class="messageText">${message}</td></tr>
										</table>
										<br/></br>
									</c:if>	
									<c:if test='${errMessage ne "" and errMessage != null}'>
										<table width="100%" cellspacing="0" cellpadding="0">
											<tr><td class="bodyText">Messages:</td></tr>
											<tr><td class="messageTextError">${errMessage}</td></tr>
										</table>
										<br/></br>
									</c:if>		
						
									<c:if test="${files_pending != null}">
										<span class="fileListHdr">Pending Files To Be Processed</span>
										<sv:dataTable data="${files_pending}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<!-- sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="?appl=${appl}&config=${config}&key=${key}&download=1&file=${row.name}&path=${row.path}&src=pending" linkTarget="_blank" headerStyleClass="listTblHdr" style="width: 60%;"/ -->
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='pending';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Upload Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>

									<c:if test="${files_archive != null}">
										<span class="fileListHdr">Processed Files</span>
										<sv:dataTable data="${files_archive}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<!-- <sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="?appl=${appl}&config=${config}&key=${key}&download=1&file=${row.name}&path=${row.path}&src=archive" linkTarget="_blank" headerStyleClass="listTblHdr" style="width: 60%;"/> -->
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='archive';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Process Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>
									
								</td>
								<td valign="top">
									<c:if test="${files_runInfo != null}">
										<span class="fileListHdr">Information Logs</span>
										<sv:dataTable data="${files_runInfo}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<!-- <sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="?appl=${appl}&config=${config}&key=${key}&download=1&file=${row.name}&path=${row.path}" linkTarget="_blank" headerStyleClass="listTblHdr" style="width: 60%;"/> -->
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Last Modified" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>
						
									<c:if test="${files_error != null}">
										<span class="fileListHdrErr">Exception Files</span>
										<sv:dataTable data="${files_error}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<!-- <sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="?appl=${appl}&config=${config}&key=${key}&download=1&file=${row.name}&path=${row.path}&src=error" linkTarget="_blank" headerStyleClass="listTblHdrErr" style="width: 60%;"/> -->
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='error';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Process Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdrErr" width="30%" />
												
												<c:if test="${files_errorArchive != null}">
													<sv:dataTableColumn title="" styleClass="listCell" value="archive" linkClass="drillLink" linkHref="Javascript:document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.src.value='archived errors';document.moveFileForm.submit()" headerStyleClass="listTblHdrErr" width="10%" />
												</c:if>
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>
			
									<c:if test="${files_errorArchive != null}">
										<span class="fileListHdr">Archived Exception Files</span>
										<sv:dataTable data="${files_errorArchive}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<!-- <sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="?appl=${appl}&config=${config}&key=${key}&download=1&file=${row.name}&path=${row.path}&src=archived errors" linkTarget="_blank" headerStyleClass="listTblHdr" style="width: 60%;"/> -->
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='archived errors';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Archive Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
									</c:if>
			
								</td>
							</tr>
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
