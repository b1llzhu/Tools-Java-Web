<%--
        Copyright 2006 SAVVIS Communications. All rights reserved.

		Generic Windows User Authentication page
		
		Imports:
			winLoginPageTitle		--> page title
			referrer				--> page (or url) to return to once authenticated
			referrerName			--> display name for the referring application
			querystring				--> optional additional querypath parameters to concatenate
										to the referrer
										
		Exports:
			winIsLoggedIn			--> equal to "true" if successfully authenticated
			winUser					--> name of the windows user that is currently logged in
			
        @author David R Young
        @version $Id$

--%>

<%@taglib uri="/WEB-INF/savvis" prefix="sv"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.savvis.it.util.*" %>

<html>
<sv:head title="SAVVIS Authentication - ${referrerTitle}"></sv:head>
<link rel="stylesheet" href="common/css/winLogin.css"/>

<form method="post" action="${referrer}">
	<input type="hidden" name="action" value="authenticate"/>
	<input type="hidden" name="referrer" value="${referrer}"/>
	
	<p align="center">
	<table class="loginTbl">
		<tr valign="top">
			<td class="loginHdr"><em>SAVVIS</em> Application Authentication</td>
		</tr>
		<tr height="2px" bgcolor="#003399"><td colspan=2></td></tr>
		<tr height="20px"><td></td></tr>
		<tr>
			<td>
				<table width="100%" cellpadding="5">
					<tr valign="top">
						<td width=65 valign="middle"><img src="common/images/padlock_50.jpg"></td>
						<td class="appTitle" valign="middle">
							<span class="appTitle">${referrerTitle}</span>
							<span class="reqCaption"><br>All fields required.</span>
						</td>
					</tr>
					<tr height="10px"><td></td></tr>
				</table>	
				
				<br/>
				
				<table width="85%" cellpadding=0 cellspacing=0 border=0>
					<tr>
						<td class="loginCaption">Windows Username</td>
						<td width="15"></td>
						<td><sv:input type="text" name="winUser" size="30" /></td>
					</tr>
					<tr>
						<td class="loginCaption">Password</td>
						<td width="15"></td>
						<td><sv:input type="password" name="winPass" size="30" /></td>
					</tr>
					<tr height="15px"><td></td></tr>
					<tr>
						<td colspan=2></td>
						<td>
							<input class="loginBtn" type="submit" value="Login">
							<input class="loginBtn" type="button" onclick="Javascript:if (confirm('Clicking the Cancel button will close this session.  Continue?')) { window.opener = self; window.close(); }" value="Cancel">
							<input class="loginBtn" type="reset" value="Reset">
						</td>
					</tr>
					
					<tr height="30px"><td></td></tr>
					<tr>
						<td colspan="2"></td>
						<td class="msgText">${errorMessage}</td>
					</tr>
				</table>
						
				<br/>
				
			</td>
		</tr>
	</table>
	<br/><br/>
	<img src="common/images/savvisLogo.jpg" width="210" align="center">
	</p>
</form>
