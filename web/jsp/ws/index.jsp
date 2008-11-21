<%--
	Copyright 2008 SAVVIS Communications. All rights reserved.
            
	The page calls a web service

	@author Ted Elrick
	@version $Id$

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
	<sv:head title="Web Service Client Index"></sv:head>
	
	<link rel="stylesheet" href="css/genericUpload.css"/>
	
	<c:choose>
		<c:when test='${fatalMsg ne "" and fatalMsg != null}'>
			${fatalMsg}
		</c:when>
		
		<c:otherwise>
			<form method="post" >
				<input type="hidden" name="action" />
				<input type="hidden" name="appl" />
				<input type="hidden" name="config" />
				
				<table width="100%" cellspacing="0" cellpadding="0">
					<tr><td class="uploadHdr" colspan=2><strong>Web Service Clients</strong></td></tr>
					<tr><td>&nbsp;</td></tr>
					<c:forEach items="${appls}" var="appl">
					<tr>
						<td style="padding: 3px;">
						<span class="fileListHdr" >${appl.key}</span>
						<table width="100%" cellspacing="2" cellpadding="2" class="actionTbl" style="border-bottom: none;">
							<c:forEach items="${appl.value}" var="client" varStatus="x" >
								<td align="center" width="50%">
									<a class="pageNav" href="javascript:gotoClient('${client.appl}', '${client.config}')">${client.title}</a>
								</td>
								<c:if test="${x.index % 2 eq 1}">
									</tr>
									<tr>
								</c:if>
								<c:if test="${x.index % 2 eq 0 and x.index == sv:sizeOf(appl.value)-1}">
									<td width="50%">&nbsp;
									</td>
								</c:if>
							</c:forEach>
							</tr>
						</table>
						
						</td>
					</tr>
					</c:forEach>
				</table>
				
			</form>
		</c:otherwise>
	</c:choose>
	<script>
		function gotoClient(appl, config) {
			svSetInputValue("appl", appl);
			svSetInputValue("config", config);
			svSubmitAction("clientHome");
		}
	</script>
</html>