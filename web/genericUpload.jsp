<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The home page contains dataTables that serve as data portals to
	give a snapshot of important data as well as a spot to welcome
	the user into the application.

	@author David R Young
	@version $Id: genericUpload.jsp,v 1.25 2009/04/14 18:38:55 dyoung Exp $

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
	<input type="hidden" name="dir" value=""/>
	<input type="hidden" name="frmContext" value="${contextValue}"/>
</form>

<form name="moveFileForm" method="post">
	<input type="hidden" name="action" value="move"/>
	<input type="hidden" name="appl" value="${appl}"/>
	<input type="hidden" name="config" value="${config}"/>
	<input type="hidden" name="key" value="${key}"/>
	<input type="hidden" name="type" value=""/>
	<input type="hidden" name="file" value=""/>
	<input type="hidden" name="path" value=""/>
	<input type="hidden" name="description" value=""/>
	<input type="hidden" name="target" value=""/>
	<input type="hidden" name="frmContext" value="${contextValue}"/>
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
								<c:if test='${key eq "" or key == null}'>
									<a class="pageNav" href="?appl=${appl}&config=${config}">Refresh This Page</a>
								</c:if>
								<c:if test='${key ne "" and key != null and authorized}'>
									<c:if test='${isAdmin eq "1"}'>
										<a class="pageNav" href="?appl=${appl}&config=${config}&contextValue=${contextValue}&key=${key}">Refresh This Page</a>
									</c:if>
									<c:if test='${isAdmin ne "1"}'>
										<a class="pageNav" href="?appl=${appl}&config=${config}&key=${key}">Refresh This Page</a>
									</c:if>
									<br/><br/>
									<a class="pageNav" href="?appl=${appl}&config=${config}">Return to Upload List</a>
								</c:if>
							</td>
					</table>
				</td>
				<td valign="top" class="bodyText">
					<c:if test='${(key eq "" or key == null) or !authorized}'>
						<form method="post" name="frm">
							<input type="hidden" name="action" value="choose"/>
							<input type="hidden" name="appl" value="${appl}"/>
							<input type="hidden" name="config" value="${config}"/>
							<input type="hidden" name="key" value="${key}">
							<input type="hidden" name="frmContext" value="${contextValue}">
					</c:if>

					<table width="100%" cellspacing="0" cellpadding="0">
						<tr><td class="uploadHdr"><strong>File Upload Utility ${uploadKeyDisplay}</strong></td></tr>
						<tr><td class="uploadTitle">Welcome ${winIsLoggedIn.name}!</td></tr>
						<c:if test='${isAdmin eq "1" and !empty contextValue}'>
							<tr><td class="uploadSubTitle">You are acting within the context [${contextValue}].</td></tr>
						</c:if>
					</table>
					<br/>

					<c:if test='${(key eq "" or key == null) or !authorized}'>
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
					
					<c:if test='${key ne "" and key != null and authorized}'>
					
						<table width="100%" cellspacing="0" cellpadding="5">
							<tr>
								<td colspan="2" valign="top">
									<c:if test='${message ne "" and message != null}'>
										<table width="100%" cellspacing="0" cellpadding="0">
											<tr><td class="bodyText">Messages:</td></tr>
											<tr><td class="messageText">${message}</td></tr>
										</table>
										<br/>
									</c:if>	
									<c:if test='${errMessage ne "" and errMessage != null}'>
										<table width="100%" cellspacing="0" cellpadding="0">
											<tr><td class="bodyText">Messages:</td></tr>
											<tr><td class="messageTextError">${errMessage}</td></tr>
										</table>
										<br/>
									</c:if>		
								</td>
							</tr>
							<tr>
								<td width="50%" valign="top">
								
									<c:forEach items="${directories}" var="d">
										<c:set var="dir" value="${d.value}" />
										<c:set var="classSuffix" value="" />
										<c:if test='${d.key eq "error" or (!empty dir.error and dir.error eq "1")}'>
											<c:set var="classSuffix" value="Err" />
										</c:if>
										
										<c:if test="${dir.valid}">
											<span class="fileListHdr${classSuffix}">${dir.description}</span>
											<c:if test='${!empty dir.subDescription}'>
												<br/><span class="fileListSubHdr">${dir.subDescription}</span>
											</c:if>
	
											<div style="overflow-y: scroll; overflow-x: none; height: ${dir.size}px;" width="100%">
											<sv:dataTable data="${dir.data}" cellpadding="2" cellspacing="2" styleClass="listTbl">
												<sv:dataTableRows rowVar="row">
												<c:forEach items="${dir.columns}" var="c">
													<c:set var="column" value="${c.value}" />
													<c:if test='${column.name eq "name"}'>
														<c:choose>
															<c:when test='${column.download eq "1"}'>
																<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.name}" 
																	linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.dir.value='${d.key}';document.downloadForm.submit()" 
																	headerStyleClass="listTblHdr${classSuffix}" width="60%"/>														
															</c:when>
															<c:otherwise>
																<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.name}" headerStyleClass="listTblHdr${classSuffix}" width="60%" />
															</c:otherwise>
														</c:choose>
													</c:if>
													<c:if test='${column.name eq "lastModified"}'>
														<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr${classSuffix}" />
													</c:if>
													<c:if test='${column.name eq "size"}'>
														<sv:dataTableColumn title="${column.title}" styleClass="listCell" value="${row.size}" headerStyleClass="listTblHdr${classSuffix}" />
													</c:if>
												</c:forEach>
												<c:forEach items="${dir.actions}" var="a">
													<c:set var="action" value="${a.value}" />
													<c:if test='${action.level eq "file"}'>
														<c:if test="${empty row}">
															<sv:dataTableColumn title="" styleClass="listCell" value="" linkClass="drillLink" 
																headerStyleClass="listTblHdr" width="10%" />
														</c:if>
														<c:if test="${!empty row}">
															<c:choose>
																<c:when test="${empty action.fileAge}">
																	<c:choose>
																		<c:when test="${action.confirm eq '1'}">
																			<sv:dataTableColumn title=" " styleClass="listCell" value="${action.description}" linkClass="drillLink" 
																				linkHref="Javascript:if (confirm('Are you sure you want to ${action.description} this file?')) { document.moveFileForm.type.value='${action.type}';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.description.value='${action.description}';document.moveFileForm.target.value='${action.target}';document.moveFileForm.submit(); }" 
																				headerStyleClass="listTblHdr" width="10%" />
																		</c:when>
																		<c:otherwise>
																			<sv:dataTableColumn title=" " styleClass="listCell" value="${action.description}" linkClass="drillLink" 
																				linkHref="Javascript:document.moveFileForm.type.value='${action.type}';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.description.value='${action.description}';document.moveFileForm.target.value='${action.target}';document.moveFileForm.submit()" 
																				headerStyleClass="listTblHdr" width="10%" />
																		</c:otherwise>
																	</c:choose>
																</c:when>
																<c:when test="${!empty action.fileAge and row.age < action.fileAge}">
																	<sv:dataTableColumn title=" " styleClass="listCell" value="" headerStyleClass="listTblHdr" width="10%" />
																</c:when>
																<c:when test="${!empty action.fileAge and row.age >= action.fileAge}">
																	<c:choose>
																		<c:when test="${row.confirm == 1}">
																			<sv:dataTableColumn title=" " styleClass="listCell" value="${action.description}" linkClass="drillLink" 
																				linkHref="Javascript:if (confirm('Are you sure you want to ${action.description} this file?')) { document.moveFileForm.type.value='${action.type}';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.description.value='${action.description}';document.moveFileForm.target.value='${action.target}';document.moveFileForm.submit(); }" 
																				headerStyleClass="listTblHdr" width="10%" />
																		</c:when>
																		<c:otherwise>
																			<sv:dataTableColumn title=" " styleClass="listCell" value="${action.description}" linkClass="drillLink" 
																				linkHref="Javascript:document.moveFileForm.type.value='${action.type}';document.moveFileForm.file.value='${row.name}';document.moveFileForm.path.value='${row.path}';document.moveFileForm.description.value='${action.description}';document.moveFileForm.target.value='${action.target}';document.moveFileForm.submit()" 
																				headerStyleClass="listTblHdr" width="10%" />
																		</c:otherwise>
																	</c:choose>
																</c:when>
															</c:choose>
														</c:if>
													</c:if>
												</c:forEach>
												</sv:dataTableRows>
											</sv:dataTable>
											</div>
											<br/><br/><br/>
										</c:if>
										
									</c:forEach>
								</td>
								<td valign="top">
									<c:if test='${allowUpload eq "1" and keyMap.pathValid}'>
										<form method="post" name="frm" enctype="multipart/form-data">
											<input type="hidden" name="action" value="upload"/>
											<input type="hidden" name="appl" value="${appl}"/>
											<input type="hidden" name="config" value="${config}"/>
											<input type="hidden" name="key" value="${key}"/>
											<input type="hidden" name="frmContext" value="${contextValue}"/>
									
										<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
											<span class="fileListHdr">Upload A File</span>
											<tr><th class="listTblHdr">Filename</th></tr>
											<tr><td style="width: 60%;" class="listCell"><input class="fileInput" type="file" name="hrFile"/></td></tr>
										</table>
										<button onclick="frm.submit();">Upload</button>
										<br/><br/>
										</form>
									</c:if>
									
									<c:if test='${hasFileUploads eq "1" and keyMap.dirPathsValid}'>
										<% try { %>
											<c:forEach items="${fileUploads}" var="fU">
												<c:set var="fileUpload" value="${fU.value}" />

												<form method="post" name="frm_${fileUpload.name}" enctype="multipart/form-data">
													<input type="hidden" name="action" value="upload"/>
													<input type="hidden" name="appl" value="${appl}"/>
													<input type="hidden" name="config" value="${config}"/>
													<input type="hidden" name="key" value="${key}"/>
													<input type="hidden" name="uploadName" value="${fileUpload.name}"/>
													<input type="hidden" name="frmContext" value="${contextValue}"/>
													
											
													<span class="fileListHdr">${fileUpload.display}</span>
													<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
														<tr><th class="listTblHdr" colspan="3">Filename</th></tr>

														<c:if test="${action.description != null}">
															<tr><td class="actionDescription" colspan="3">${fileUpload.description}</td></tr>
														</c:if>
			
														<tr>
															<td colspan="3">
																<input class="fileInput" type="file" name="file"/>
															</td>
														</tr>
														
														<c:if test="${!empty fileUpload.alias}">
															<tr>
																<td class="inputCell" style="text-align: right;">Alias (optional)</td>
																<td width="5" class="inputCell"></td>
																<td><input class="inputCell" type="text" name="alias" size="30" maxlength="40"/></td>
															</tr>
														</c:if>
													</table>
													<button onclick="frm_${fileUpload.name}.submit();">${fileUpload.buttonLabel}</button>
													<br/><br/><br/>
												</form>
											</c:forEach>
										<%  } catch(Exception e) {
												e.printStackTrace(new java.io.PrintWriter(out));
											} 
										%>
									</c:if>
									
									<c:if test='${hasActions eq "1" and keyMap.dirPathsValid}'>
										<span class="fileListHdr">Perform Actions</span>
										<table width="100%" cellspacing="2" cellpadding="2" class="actionTbl">

											<% try { %>
											<c:forEach items="${actions}" var="a">
												<c:set var="action" value="${a.value}" />

												<form method="post" name="action_${action.name}">
													<input type="hidden" name="action" value="execute"/>
													<input type="hidden" name="appl" value="${appl}"/>
													<input type="hidden" name="config" value="${config}"/>
													<input type="hidden" name="key" value="${key}"/>
													<input type="hidden" name="action_name" value="${action.name}"/>
													<input type="hidden" name="frmContext" value="${contextValue}"/>

												<tr><th class="listTblHdr">${action.display}</th></tr>
												<c:if test="${action.description != null}">
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
																		<c:when test='${input.type eq "select" or input.type eq "sqlselect" or input.type eq "fileselect" or input.type eq "cfgselect"}'>
																			<sv:select id="${input.name}" name="${input.name}" title="${input.label}" items="${input.values}" 
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"/>
																		</c:when>
																		<c:when test='${input.type eq "date"}'>
																			<sv:date id="${input.name}" name="${input.name}" title="${input.label}"
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"/>
																		</c:when>
																		<c:otherwise>
																			<sv:input id="${input.name}" name="${input.name}" title="${input.label}" type="${input.type}" 
																						required="${input.required}" readonly="${input.readonly}" value="${input.defaultValue}"
																						maxlength="${input.maxlength}" format="${input.format}" regex="${input.regex}" 
																						regexValidationText="${input.regexValidationText}" />
																		</c:otherwise>
																	</c:choose>
																</td>
															</tr>
		
														</c:forEach>

														</table>
													</c:if>
													
												</td></tr>
												<tr>
													<c:choose>
														<c:when test="${processRunning}">
															<td class="actionBtnCell">A process is currently running... please wait...</td>
														</c:when>
														<c:otherwise>
															<td>
															<c:choose>
																<c:when test="${action.confirm eq '1'}">
																	<button onclick="Javascript:if (confirm('Are you sure you want to perform this action?')) { svSetMyForm(document.forms['action_${action.name}']); svSubmitAction('execute'); }">${action.buttonLabel}</button>
																</c:when>
																<c:otherwise>
																	<button onclick="svSetMyForm(document.forms['action_${action.name}']); svSubmitAction('execute');">${action.buttonLabel}</button>
																</c:otherwise>
															</c:choose>
															</td>
															
														</c:otherwise>
													</c:choose>
													
												</tr>

												</form>
											</c:forEach>
											<%  } catch(Exception e) {
													e.printStackTrace(new java.io.PrintWriter(out));
												} 
											%>
										</table>
										<br/><br/>
									</c:if>
									
									<c:if test='${isAdmin eq "1" and switch ne "1"}'>
										<form method="post" name="frmSwitch">
											<input type="hidden" name="action" value="switch"/>
											<input type="hidden" name="appl" value="${appl}"/>
											<input type="hidden" name="config" value="${config}"/>
											<input type="hidden" name="key" value="${key}"/>
											<input type="hidden" name="switch" value="1"/>
											<input type="hidden" name="frmContext" value="${contextValue}"/>
									
											<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
												<span class="fileListHdr">Switch to Other Context</span>
												<tr><th class="listTblHdr">Context</th></tr>
												<tr>
													<td style="width: 60%;" class="listCell">
														<button onclick="frmSwitch.submit();">Switch</button>
													</td>
												</tr>
											</table>
											<br/><br/>
										</form>
									</c:if>
									<c:if test='${isAdmin eq "1" and switch eq "1"}'>
										<form method="post" name="frmSwitchContext">
											<input type="hidden" name="action" value="switch"/>
											<input type="hidden" name="appl" value="${appl}"/>
											<input type="hidden" name="config" value="${config}"/>
											<input type="hidden" name="key" value="${key}"/>
									
											<table with="100%" cellspacing="2" cellpadding="2" class="listTbl">
												<span class="fileListHdr">Switch to Other Context</span>
												<tr><th class="listTblHdr">Context</th></tr>
												<tr>
													<td style="width: 60%;" class="listCell">
														<sv:select id="frmContext" name="frmContext" items="${contextValues}"/>
														&nbsp;&nbsp;<button onclick="frmSwitchContext.submit();">Switch To</button>
													</td>
												</tr>
											</table>
											<br/><br/>
										</form>
									</c:if>

									<c:if test="${keyMap.pathValid and keyMap.dirPathsValid and (empty keyMap.runInfoValid or (!empty keyMap.runInfoValid and keyMap.runInfoValid))}">
										<span class="fileListHdr">Information Logs</span>
										<iframe frameborder="0" id="runInfoLog" src="runInfo?path=${keyMap.path}&runInfo=${keyMap.runInfo}" 
											style="border-collapse: collapse; border: 0px; height: 300px; width: 100%;"></iframe>
										<br/><br/><br/>
	
										<c:if test="${!empty keyMap.runInfoRefresh}">
											<script language="javascript">
												function refreshRunInfo() {
													var runInfoFrame = document.getElementById('runInfoLog');
													runInfoFrame.src = "runInfo?path=${keyMap.path}&runInfo=${keyMap.runInfo}";
													setTimeout("refreshRunInfo()", ${keyMap.runInfoRefresh} * 1000);
												}
												refreshRunInfo();
											</script>									
										</c:if>
									</c:if>
									
									<c:if test='${helpDir eq "1"}'>
										<c:set var="help" value="${_help}" />
										<span class="fileListHdr${classSuffix}">Help Documents</span>
										<br/><span class="fileListSubHdr">Help documents, template descriptions, etc.</span>
										<div style="height: 50px;" width="100%">
										<sv:dataTable data="${help.data}" cellpadding="2" cellspacing="2" styleClass="listTbl">
											<sv:dataTableRows rowVar="row">
												<sv:dataTableColumn title="Filename" styleClass="listCell" value="${row.name}" 
													linkClass="drillLink" linkHref="Javascript:document.downloadForm.file.value='${row.name}';document.downloadForm.dir.value='${d.key}';document.downloadForm.submit()" 
													headerStyleClass="listTblHdr${classSuffix}" width="60%"/>														
												<sv:dataTableColumn title="Size" styleClass="listCell" value="${row.size}" headerStyleClass="listTblHdr${classSuffix}" />
												<sv:dataTableColumn title="Last Modified" styleClass="listCell" value="${row.lastModified}" headerStyleClass="listTblHdr${classSuffix}" />
											</sv:dataTableRows>
										</sv:dataTable>
										</div>
										<br/><br/><br/>
									</c:if>
									
									
								</td>
							</tr>
						</table>
						
										
					</c:if>
				</td>
			</tr>
			<tr>
				<td colspan=2><img src="common/images/savvisLogoRebranded.png" width="300" align="right"/></td>
			</tr>
		</table>		
	</c:otherwise>
</c:choose>
