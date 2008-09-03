<!--
	Copyright 2006 SAVVIS Communications. All rights reserved.

	This is the error page that is used when an error occurs in a servlet

	@author Ted Elrick
	@version $Id$
-->

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>JSP/Servlet Test Project Error Page</title>
	</head>
	<body>
<%  if(request.getAttribute("exception") != null) {
			Throwable e = (Throwable)request.getAttribute("exception");
			out.println("<font color=red>"+e.getClass().getName()+":  "+e.getMessage()+"</font><br>");
			StackTraceElement[] stackTraceElements = e.getStackTrace();
			for( int x = 0 ; x < stackTraceElements.length ; x++ ) {
				out.println(stackTraceElements[x]+"<br>");
			}
			while(e.getCause() != null) {
				e = e.getCause();
				out.println("<br><br>Caused By: <font color=red>"+e.getClass().getName()+":  "+e.getMessage()+"</font><br>");
				stackTraceElements = e.getStackTrace();
				for( int x = 0 ; x < stackTraceElements.length ; x++ ) {
					out.println(stackTraceElements[x]+"<br>");
				}
			}
		} 
%>
	</body>
</html>