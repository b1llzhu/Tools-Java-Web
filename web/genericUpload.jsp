<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: genericUpload.jsp,v 1.5 2008/08/25 14:29:37 dyoung Exp $

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
	<input type="hidden" name="moveCode" value=""/>
	<input type="hidden" name="file" value=""/>
	<input type="hidden" name="path" value=""/>
	<input type="hidden" name="src" value=""/>
</form>

<c:choose>
	<c:when test='${fatalMsg ne "" and fatalMsg != null}'>
		${fatalMsg}
	</c:when>
	
	<c:otherwise>
		<table width="100%" height="90%" cellspacing="0" cellpadding="0">
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
							<input type="hidden" name="appl" value="${appl}"/>
							<input type="hidden" name="config" value="${config}"/>
							<input type="hidden" name="key" value="${key}">
					</c:if>

					<table width="100%" cellspacing="0" cellpadding="0">
						<tr><td class="uploadHdr"><strong>File Upload Utility ${uploadKeyDisplay}</strong></td></tr>
						<tr><td class="uploadTitle">Welcome ${winIsLoggedIn.name}!</td></tr>
					</table>
					<br/>
					

					<c:if test='${(key eq "" or key == null) or unauthorized eq "true"}'>
						Select the upload type from the list below.  If authorized, you will be taken to the upload screen.<p/>
						<ul>
							<c:forEach items="${uploads}" var="upload">
								<li><a class="uploadKey" href="?appl=${appl}&config=${config}&key=${upload.key}">${upload.name}</a><br/></li>
								<c:set var="action" value="${a.value}" />
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
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='pending';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Upload Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>

									<c:if test="${files_working != null}">
										<span class="fileListHdr">Files Currently Being Processed</span><br/>
										<span class="fileListSubHdr">(files must be at least 15 minutes old before they can be resubmitted)</span>
										<sv:dataTable data="${files_working}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='working';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Process Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="30%" />

												<c:if test="${empty row.age or row.age < 15}">
													<sv:dataTableColumn title=" " styleClass="listCell" value="" headerStyleClass="listTblHdr" width="10%" />
												</c:if>
												<c:if test="${row.age >= 15}">
													<sv:dataTableColumn title=" " styleClass="listCell" value="resubmit" linkClass="drillLink" linkHref="Javascript:document.moveFileForm.moveCode.value='resubmit';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.src.value='working';document.moveFileForm.submit()" headerStyleClass="listTblHdr" width="10%" />
												</c:if>
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>

									<c:if test="${files_archive != null}">
										<span class="fileListHdr">Processed Files</span>
										<sv:dataTable data="${files_archive}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='archive';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Process Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>
									
									<c:if test="${files_error != null}">
										<span class="fileListHdrErr">Exception Files</span>
										<sv:dataTable data="${files_error}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='error';document.downloadForm.submit()" headerStyleClass="listTblHdrErr" style="width: 60%;"/>
												<sv:dataTableColumn title="Process Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdrErr" width="30%" />
												
												<c:if test="${files_errorArchive != null}">
													<sv:dataTableColumn title="" styleClass="listCell" value="archive" linkClass="drillLink" linkHref="Javascript:document.moveFileForm.moveCode.value='errorArchive';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.src.value='archived errors';document.moveFileForm.submit()" headerStyleClass="listTblHdrErr" width="10%" />
												</c:if>
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>
			
									<c:if test="${files_errorArchive != null}">
										<span class="fileListHdr">Archived Exception Files</span>
										<sv:dataTable data="${files_errorArchive}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='archived errors';document.downloadForm.submit()" headerStyleClass="listTblHdr" style="width: 60%;"/>
												<sv:dataTableColumn title="Archive Date" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr" width="40%" />
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:if>			
								</td>
								<td valign="top">
									<c:if test='${allowUpload eq "1"}'>
										<form method="post" name="frm" enctype="multipart/form-data">
											<input type="hidden" name="action" value="upload"/>
											<input type="hidden" name="appl" value="${appl}"/>
											<input type="hidden" name="config" value="${config}"/>
											<input type="hidden" name="key" value="${key}"/>
									
										<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
											<span class="fileListHdr">Upload A File</span>
											<tr><th class="listTblHdr">Filename</th></tr>
											<tr><td style="width: 60%;" class="listCell"><input class="fileInput" type="file" name="hrFile"/></td></tr>
										</table>
										<button onclick="frm.submit();">Upload</button>
										<br/><br/><br/>
										</form>
									</c:if>
									
									<c:if test='${hasActions eq "1"}'>
										<span class="fileListHdr">Perform Actions</span>
										<table width="100%" cellspacing="2" cellpadding="2" class="actionTbl">

											<c:forEach items="${actions}" var="a">
												<c:set var="action" value="${a.value}" />

												<form method="post" name="action_${action.name}">
													<input type="hidden" name="action" value="execute"/>
													<input type="hidden" name="appl" value="${appl}"/>
													<input type="hidden" name="config" value="${config}"/>
													<input type="hidden" name="key" value="${key}"/>
													<input type="hidden" name="action_name" value="${action.name}"/>
											

												<tr><th class="listTblHdr">${action.display}</th></tr>
												<c:if test="${action.display != null}">
													<tr><td class="actionDescription">${action.description}</td></tr>
												</c:if>
												
												<tr><td class="actionCell">

													<c:if test='${action.inputs != null}'>
														<table width="100%" cellspacing="2" cellpadding="2">
			
														<c:set var="input" value="${action.inputs}" />
														<c:forEach items="${input}" var="i">
															<c:set var="inputs" value="${i.value}" />
														
															<tr>
																<td width="20%" class="inputCell" style="text-align: right;">${inputs.label}</td>
																<td width="5" class="inputCell"></td>
																<td class="inputCell">
																	<c:if test='${inputs.type eq "select" or inputs.type eq "SQLselect"}'>
																		<sv:select id="${inputs.name}" name="${inputs.name}" items="${inputs.values}" required="1"/>
																	</c:if>
																	<c:if test='${inputs.type eq "text"}'>
																		<sv:input id="${inputs.name}" name="${inputs.name}" type="text" />
																	</c:if>
																	<c:if test='${inputs.type eq "date"}'>
																		<sv:date id="${inputs.name}" name="${inputs.name}" />
																	</c:if>
																</td>
															</tr>
		
														</c:forEach>

														</table>
													</c:if>
													
												</td></tr>
												<tr>
													<td class="actionBtnCell"><button onclick="action_${action.name}.submit();">${action.buttonLabel}</button></td>
												</tr>

												</form>
											</c:forEach>
										</table>
										<br/><br/>
									</c:if>
									
									<span class="fileListHdr">Information Logs</span>
									<iframe frameborder="0" id="runInfoLog" src="runInfo?path=${basedir}/${appl}/${key}" 
										style="border-collapse: collapse; border: 0px; height: 300px; width: 100%;"></iframe>
									<br/><br/><br/>
								</td>
							</tr>
						</table>
						
										
					</c:if>
				</td>
			</tr>
			<tr>
				<td colspan=2><img src="common/images/savvisLogo.jpg" width="300" align="right"/></td>
			</tr>
		</table>		
	</c:otherwise>
</c:choose>
