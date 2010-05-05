<!--
	Copyright 2010 SAVVIS Communications. All rights reserved.

	This is the p13nListConfig file

	@author Matt Ramella
	@version $Id: p13nListConfig.jsp,v 1.2 2010/06/18 20:01:37 dyoung Exp $
-->

<%@taglib uri="/WEB-INF/savvis.tld" prefix="sv"%> 
<%@taglib uri="/WEB-INF/c.tld" prefix="c"%> 

<script> 

//alert(window.opener.p13ColumnNames);

</script>

<html>

	<sv:head title="Personalization List Configuration" /> 
	
	<link rel=stylesheet type='text/css' href='<c:url value="${p13nCssFilePath}"/>'>
	
	<body>

		<sv:alertMessages />

		<form action="P13NServlet" method="post" name="p13nListConfig">

			<sv:input type="hidden" name="action" value="saveListConfig"/>

			<sv:input type="hidden" name="p13nKey" value="${p13nKey}"/>
			
			<sv:multiSelectDual name="columns" rightList="${rightList}" leftList="${leftList}" required="false" style="width: 250px;" styleClass="formInput" mode="edit" title="Personalization List Configuration"/>
		    
		    <input type="button" value="Save" class="button" onclick="svSubmitAction('saveListConfig');<c:out value="${p13nJavaScriptUponSave}"/>;window.close();">
		    
		    <input type="button" value="Reset to Default" class="button" onclick="dualListRightcolumns.options.length=0;svSubmitAction('saveListConfig');<c:out value="${p13nJavaScriptUponSave}"/>;window.close();">
		    
		    <input type="button" value="Cancel" class="button" onclick="window.close();">
		
		</form>
  
  	</body>
</html>