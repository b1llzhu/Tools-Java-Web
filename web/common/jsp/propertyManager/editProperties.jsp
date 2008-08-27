<!--
	Copyright 2007 SAVVIS Communications. All rights reserved.

	This is the Property Manager page where you can edit properties
	as well as log4j settings

	@author Ted Elrick
	@version $Id$
-->

<%@taglib uri="/WEB-INF/savvis.tld" prefix="sv"%> 
<%@taglib uri="/WEB-INF/c.tld" prefix="c"%> 

<html>
	<sv:head title="Property Management Edit Page" />
	
	<body>
		<form action="PropertyManagerServlet" method="POST" target="editPropertiesIframe">
			<sv:input type="hidden" name="action"/>
			<sv:input type="hidden" name="index"/>
			<sv:input type="hidden" name="newProperty"/>
			<sv:input type="hidden" name="fileName" value="${fileName}"/>


			<table border=0 align=center>
				<c:if test="${not empty message}">
					<tr><td class="header" colspan=7 align=center><font color="#00818D">${message}</font></td></tr> 
				</c:if>
				<c:if test="${not empty errorMessage}">
					<tr><td class="header" colspan=7 align=center><font color="red">${errorMessage}</font></td></tr> 
				</c:if>
				<tr>
					<th colspan=2 align=center>Properties for ${fileName}</th>
				</tr>
				<c:forEach items="${keys}" var="key">
					<tr>
						<th align=left>
							${key}
						</th>
						<td>
							<sv:input type="text" name="property.${key}" value="${properties[key]}" size="50"/>
						</td>
					</tr>
				</c:forEach>
			</table>
			<center>
				<input type="submit" value="Commit" onclick="svSubmitAction('commit');return false;">
				&nbsp;<button onclick="addProperty()" accesskey="A"><u>A</u>dd</button>
			</center>
		</form>
		<script>
			function addProperty() {
				var newProperty = prompt("Enter the property you wish to add ('property=value')", "");
				if(newProperty == "" || !newProperty) 
					return;
					
				svSetInputValue("newProperty", newProperty);
				svSubmitAction("add");
			}
		</script>
	</body>
</html>
