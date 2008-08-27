<!--
	Copyright 2007 SAVVIS Communications. All rights reserved.

	This is the Property Manager page where you can add, remove and edit properties
	as well as log4j settings

	@author Ted Elrick
	@version $Id$
-->

<%@taglib uri="/WEB-INF/savvis.tld" prefix="sv"%> 
<%@taglib uri="/WEB-INF/c.tld" prefix="c"%> 

<html>
	<sv:head title="Property Management Page" />
	
	<body>
		<form action="PropertyManagerServlet" method="POST" target="editPropertiesIframe">
			<sv:input type="hidden" name="action"/>
			<sv:input type="hidden" name="index"/>

			<table width="100%" border=0>
				<tr>
					<th>Currently registered property files</th>
				</tr>
				<tr>
					<td>
						<c:forEach items="${propertyManagers}" var="pm">
							<a href="javascript:svSubmitActionAndIndex('edit', '${pm.resourceFileName}')">${pm.resourceFileName}</a><br>
						</c:forEach>
						<a href="javascript:svSubmitActionAndIndex('edit', 'log4j')">Edit log4j settings</a><br>
					</td>
				</tr>
			</table>
			<HR>
			<iframe name="editPropertiesIframe" id="editPropertiesIframe" width="100%" height="500" 
						frameborder="0" marginwidth="0" marginheight="0" src="" ></iframe>
		</form>
	</body>
</html>
