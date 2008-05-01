<%--
	Copyright 2006 SAVVIS Communications. All rights reserved.
            
	The entry login page for the app.  If a user is entering the app
	for the first time or if their browser session expires, they are
	redirected here by the Security filter servlet.

	@author David R Young
	@version $Id: passwordReset.jsp,v 1.1 2008/05/01 16:19:41 dmoorhem Exp $

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%> 
<%@taglib uri="/WEB-INF/c.tld" prefix="c" %> 



<html>
	<sv:head title="Password Reset"></sv:head>
	<body onload="document.forms[0].secUsername.focus();">
	<br /><br />
	<table width="100%" class="tabBase"><tr><td></td></table>
	<br /><br />
<form action="passwordReset" method="post">
<sv:input type="hidden" name="action" />
	<table width="100%">
		<tr>
			<td align="center" valign="top" width="35"></td>
			<td valign="top" width="250">
				<img src="img/padlock_chains_200.jpg"><br /><br /><br />
				<img src="img/savvis.gif">
			</td>

			<td valign="top">
				<table width="100%" cellspacing="0" cellpadding="0">
					<tr>
						<td class="loginHdr">ACCESS SECURITY ADMINISTRATION</td>
					</tr>
				</table>
				<br class="loginCaption"><br class="loginCaption">

				<table cellspacing="2" cellpadding="0">
						<tr>
							<td class="loginCaption">Username</td>
							<td>
								<sv:input name="secUsername" type="text" required="true" title="Username" value="${secUsername}"
									style="width: 125px;" maxlength="30" styleClass="formInput" tabindex="1" /></td>
						</tr>
						<tr>
							<td class="loginCaption">Password</td>
							<td>
								<sv:input name="secPassword" type="password" required="true" title="Password" 
									style="width: 125px;" maxlength="30" styleClass="formInput" tabindex="2" /></td>
						</tr>
						<tr>
							<td class="loginCaption"></td>
							<td><br>
								<input type="submit" onclick="svSubmitAction('reset'); return false;" accesskey="r" class="btn" value="reset" />
							</td>
						</tr>

				</table>
				<br />
				<table cellspacing="0" cellpadding="0" width="300">
					<tr>
						<td class="loginMsgHdr">Messages:</td>
					</tr>
					<tr>
						<td class="loginMsg"><% 
							if (request.getAttribute("login_message") != null) { 
								out.print(request.getAttribute("login_message")); 
							} else {
								out.print("Please log in with your Windows Domain username and password."); 
							} %>
						</td>
					</tr>
					<tr>
						<td>
							<c:forEach items="${systemList}" var="system" varStatus="systemIndex">
									<sv:input name="${system.code}" type="checkbox" required="false" /> ${system.name} <br />
							</c:forEach>
						</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
</form>

	</body>
</html>

