<!--
	Copyright 2010 SAVVIS Communications. All rights reserved.

	This is the p13nListConfig file

	@author Matt Ramella
	@version $Id: p13nListConfig.jsp,v 1.5 2010/07/13 20:19:30 dmoorhem Exp $
-->

<%@taglib uri="/WEB-INF/savvis.tld" prefix="sv"%> 
<%@taglib uri="/WEB-INF/c.tld" prefix="c"%> 

<script> 

//alert(window.opener.p13ColumnNames);

	function saveComplete()
	{
		if('${saveComplete}' == 'true') {
			${p13nJavaScriptUponSave}
			
			parent.svHide('${p13nKey}PersonalizationFrame')
		}
		
		return true;
	}

</script>

<html>

	<sv:head title="Personalization List Configuration" /> 
	
	<link rel='stylesheet' type='text/css' href='<c:url value="${p13nCssFilePath}"/>'>
	
	<body onload="saveComplete();parent.svResizeIFrameExact('${p13nKey}PersonalizationFrame');">
	
		<center>

			<sv:alertMessages />
			
			<c:if test='${saveComplete == null}'>
	
			<form action="P13NServlet" method="post" name="p13nListConfig">
				<table border=1 cellpadding=3>
					<tr>
						<th>Move the columns that should show up in the table to the right side and click Save to confirm.</th>
					</tr>
					<tr>
						<td>
							<sv:input type="hidden" name="action" value="saveListConfig"/>
				
							<sv:input type="hidden" name="p13nKey" value="${p13nKey}"/>
							
							<sv:input type="hidden" name="p13nUserId" value="${p13nUserId}"/>
							
							<sv:input type="hidden" name="p13nJavaScriptUponSave" value="${p13nJavaScriptUponSave}"/>
							
							<sv:multiSelectDual name="columns" rightList="${rightList}" leftList="${leftList}" 
										required="false" style="width: 250px;" styleClass="formInput" mode="edit" 
										showUpDownArrows="true"
										title="Personalization List Configuration"/>
						    
						    <input type="button" value="Save" class="button" 
						    			onclick="svSubmitAction('saveListConfig');">
						    
						    <input type="button" value="Reset to Default" class="button" 
						    			onclick="dualListRightcolumns.options.length=0;svSubmitAction('saveListConfig');">
						    
						    <input type="button" value="Cancel" class="button" onclick="parent.svHide('${p13nKey}PersonalizationFrame')">
						
						</td>
					</tr>
				</table>
	
			</form>
			
			</c:if>
			
			<c:if test='${saveComplete != null}'>
				<input type="button" value="Close" class="button" onclick="parent.svHide('${p13nKey}PersonalizationFrame')">
			</c:if>
		
		</center>
  
  </body>
</html>