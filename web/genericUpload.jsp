<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: genericUpload.jsp,v 1.9 2008/08/27 21:23:27 telrick Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<sv:head title="File Upload Utility"></sv:head>

<link rel="stylesheet" href="css/genericUpload.css"/>

<form name="downloadForm" method="post">
	<input type="hidden" name="action" value="download"/>
	<input type="hidden" name="appl" value="${appl}"/>
	<input type="hidden" name="config" value="${config}"/>
	<input type="hidden" name="key" value="${key}"/>
	<input type="hidden" name="download" value="1"/>
	<input type="hidden" name="file" value=""/>
	<input type="hidden" name="path" value=""/>
	<input type="hidden" name="src" value=""/>
</form>

<form name="moveFileForm" method="post">
	<input type="hidden" name="action" value="move"/>
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
						
									<c:forEach items="${directories}" var="d">
										<c:set var="dir" value="${d.value}" />
										<c:set var="classSuffix" value="" />
										<c:if test='${d.key eq "error"}'>
											<c:set var="classSuffix" value="Err" />
										</c:if>

										<span class="fileListHdr${classSuffix}">${dir.description}</span>
										<c:if test='${!empty dir.subDescription}'>
											<br/><span class="fileListSubHdr">${dir.subDescription}</span>
										</c:if>

										<sv:dataTable data="${dir.data}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
											<c:forEach items="${dir.columns}" var="c">
												<c:set var="column" value="${c.value}" />
												<c:if test='${column.name eq "name"}'>
													<c:choose>
														<c:when test='${column.download eq "1"}'>
															<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.name}" 
																linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.path.value='${row.path}';document.downloadForm.src.value='pending';document.downloadForm.submit()" 
																headerStyleClass="listTblHdr${classSuffix}" style="width: 60%;"/>														
														</c:when>
														<c:otherwise>
															<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.name}" headerStyleClass="listTblHdr${classSuffix}" width="60%" />
														</c:otherwise>
													</c:choose>
													<c:if test='${column.download eq "1"}'>
														
													</c:if>
													
												</c:if>
												<c:if test='${column.name eq "lastModified"}'>
													<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr${classSuffix}" width="30%" />
												</c:if>
											</c:forEach>
											<c:forEach items="${dir.actions}" var="a">
												<c:set var="action" value="${a.value}" />
												<c:if test='${action.level eq "file" and action.type eq "move"}'>
													<c:choose>
														<c:when test="${empty row.age or row.age < action.fileAge}">
															<sv:dataTableColumn title=" " styleClass="listCell" value="" headerStyleClass="listTblHdr" width="10%" />
														</c:when>
														<c:when test="${row.age >= 15}">
															<sv:dataTableColumn title=" " styleClass="listCell" value="${action.description}" linkClass="drillLink" 
																linkHref="Javascript:document.moveFileForm.moveCode.value='resubmit';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.src.value='${action.target}';document.moveFileForm.submit()" 
																headerStyleClass="listTblHdr" width="10%" />
														</c:when>
													</c:choose>
												</c:if>
											</c:forEach>
											<c:if test="${empty dir.actions}">
												<sv:dataTableColumn title=" " styleClass="listCell" value="" headerStyleClass="listTblHdr" width="10%" />
											</c:if>
											</sv:dataTableRows>
										</sv:dataTable>
										<br/><br/><br/>
									</c:forEach>
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
			
														<c:set var="inputs" value="${action.inputs}" />
														<c:forEach items="${inputs}" var="i">
															<c:set var="input" value="${i.value}" />
															<tr>
																<td class="inputCell" style="text-align: right;">${input.label}</td>
																<td width="5" class="inputCell"></td>
																<td class="inputCell">
																	<c:choose>
																		<c:when test='${input.type eq "select" or input.type eq "SQLselect"}'>
																			<sv:select id="${input.name}" name="${input.name}" title="${input.label}" items="${input.values}" 
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"/>
																		</c:when>
																		<c:when test='${input.type eq "date"}'>
																			<sv:date id="${input.name}" name="${input.name}" title="${input.label}" 
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"/>
																		</c:when>
																		<c:otherwise>
																			<sv:input id="${input.name}" name="${input.name}" title="${input.label}" type="${input.type}" 
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"/>
																		</c:otherwise>
																	</c:choose>
																</td>
															</tr>
		
														</c:forEach>

														</table>
													</c:if>
													
												</td></tr>
												<tr>
													<td class="actionBtnCell"><button onclick="svSetMyForm(document.forms['action_${action.name}']); svSubmitAction('execute');">${action.buttonLabel}</button></td>
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
