<%--
	Copyright 2008 SAVVIS Communications. All rights reserved.
            
	The page calls a web service

	@author Ted Elrick
	@version $Id$

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
	<sv:head title="Web Service Client Utility"></sv:head>
	
	<link rel="stylesheet" href="css/genericUpload.css"/>
	
	<c:choose>
		<c:when test='${fatalMsg ne "" and fatalMsg != null}'>
			${fatalMsg}
		</c:when>
		
		<c:otherwise>
			<form method="post" >
			${sample}
				<input type="hidden" name="action" value="call"/>
				<input type="hidden" name="appl" value="${appl}"/>
				<input type="hidden" name="config" value="${config}"/>

				<table width="100%" cellspacing="0" cellpadding="0">
					<tr><td class="uploadHdr" colspan=3><strong>${title}</strong></td></tr>
					<tr>
						<td width="10%"><b><a class="pageNav" href='<c:url value="/genericWSClient"/>'>Client Index</a></b></td>
						<td class="uploadTitle" align="center">
							Welcome ${winIsLoggedIn.name}!  <br>
							Enter the input xml and click Execute<br>
							WSDL:  <a href="${wsdl}">${wsdl}</a><br>
							Operation:  
							<sv:select name="operation" items="${operations}" addEmptyEntry="false"/>&nbsp;
							<button onclick="svSubmitAction('sample', 'sampleWindow'); focusPopup('sampleWindow')"
								accesskey="S"><u>S</u>how Sample XML</button>&nbsp;
							<button onclick="svSubmitAction('useSample');"
								accesskey="U"><u>U</u>se Sample XML</button><br>
							<input type="file" name="xmlUpload" onchange="toggleUploadXmlButton(this.value)" size="75" class="button">&nbsp;
							<button id='uploadXmlButton' disabled="disabled" onclick="uploadXml();"
										>Upload Xml from File</button>&nbsp;
						</td>
						<td width="10%"></td>
					</tr>
				</table>
				
				<table width="100%" height="90%" cellspacing="0" cellpadding="0">
					<tr>
						<td valign="top" class="bodyText" align="center">
							<sv:textarea name="xml" cols="100" rows="15" value="${xml}"/><br>
							<button onclick="execute()"
								accesskey="E"><u>E</u>xecute</button>
							<sv:input type="checkbox" name="attachment" value="1" />Attachment
							<sv:input type="checkbox" name="pretty" value="1" />Pretty
						</td>
					</tr>
					<tr>
						<td colspan=2><img src="common/images/savvisLogo.jpg" width="300" align="right"/></td>
					</tr>
				</table>		
			</form>
		</c:otherwise>
	</c:choose>
	<script>
		function focusPopup(windowName) {
			open('', windowName).focus()
		}
		function execute() {
			if(svGetInputValue("attachment") == "1") {
				svSubmitAction('call'); 
			} else {
				svSubmitAction('call', 'outputWindow');
				focusPopup('outputWindow')
			}
		}
		function toggleUploadXmlButton(value) {
			svGetElement('uploadXmlButton').disabled = !svEndsWith(value, '.xml');
		}
		function uploadXml() {
			document.forms[0].encoding = "multipart/form-data";
			svSubmitAction('uploadXml');
		}
	</script>
</html>