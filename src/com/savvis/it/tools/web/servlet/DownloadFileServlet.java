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
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: DownloadFileServlet.java,v 1.2 2008/08/25 14:29:38 dyoung Exp $
 */
public class DownloadFileServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(DownloadFileServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/Attic/DownloadFileServlet.java,v 1.2 2008/08/25 14:29:38 dyoung Exp $";
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String fileName = "";
		String path = "";
		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = 
			(WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);;
		
		try {
			
			fileName = request.getParameter("file") != null ? request.getParameter("file").toString() : request.getAttribute("file").toString();
			path = request.getParameter("path") != null ? request.getParameter("path").toString() : request.getAttribute("path").toString();

			if (StringUtil.hasValue(fileName) && StringUtil.hasValue(path)) {
				response.setContentType("APPLICATION/OCTET-STREAM");
				String disHeader = "Attachment;Filename=" + fileName ;
				response.setHeader("Content-Disposition", disHeader);
				response.setHeader("Content-type", "application/force-download");
				File file = new File(path + "/" + fileName);
				FileInputStream fileInputStream = new FileInputStream(file);
				int i;
				while ((i=fileInputStream.read())!=-1) {
					response.getOutputStream().write(i);
				}
				response.getOutputStream().flush();
				response.getOutputStream().close();
				fileInputStream.close();
				
				// add a log comment (if the file downloaded is not a standard log file
				if (!fileName.equals(RunInfoUtil.STD_LOG_NAME) && !path.endsWith(RunInfoUtil.STD_LOG_NAME))
					RunInfoUtil.addLog(file, winPrincipal.getName(), new java.util.Date(), "File " + file.getName() + " downloaded");
			}
			
		} catch (SocketException se) {
			logger.info("User cancelled download of file " + fileName + " from path " + path);
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}
}
