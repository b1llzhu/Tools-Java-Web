/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.savvis.it.db.DBConnection;
import com.savvis.it.filter.WindowsAuthenticationFilter;
import com.savvis.it.filter.WindowsAuthenticationFilter.WindowsPrincipal;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.*;

/**
 * This class displays the run info log 
 * 
 * @author David R Young
 * @version $Id: RunInfoDisplayServlet.java,v 1.1 2008/08/25 14:28:33 dyoung Exp $
 */
public class RunInfoDisplayServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(RunInfoDisplayServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/Attic/RunInfoDisplayServlet.java,v 1.1 2008/08/25 14:28:33 dyoung Exp $";
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String path = "";

		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = 
			(WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);;
		
		try {
			
			path = request.getParameter("path") != null ? request.getParameter("path").toString() : request.getAttribute("path").toString();
			
			if (StringUtil.hasValue(path)) {
				if (!path.endsWith("/"))
					path = path + "/";
				
				logger.info("path: " + path);
				
				List<Map<String, String>> contents = RunInfoUtil.getContents(path, true);
				request.setAttribute("contents", contents);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} 
		
		forward("runInfoDisplay.jsp", request, response);
	}
}
