/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.*;
import org.apache.log4j.Logger;

import com.savvis.it.db.DBConnection;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.PropertyManager;
import com.savvis.it.util.StringUtil;

/**
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: HartfordUploadServlet.java,v 1.1 2008/05/01 19:53:00 dyoung Exp $
 */
public class HartfordUploadServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(HartfordUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/Attic/HartfordUploadServlet.java,v 1.1 2008/05/01 19:53:00 dyoung Exp $";
	
	private static PropertyManager properties = new PropertyManager(
	"/properties/fileUpload.properties");
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String hartfordDir = properties.getProperty("hartfordDir");
		if (!hartfordDir.endsWith("/"))
			hartfordDir = hartfordDir.concat("/");
		
		logger.info("property->hartfordDir: " + hartfordDir);
		
		String jspPage = "hartfordUpload.jsp";
		
		if (hartfordDir == null) {
			request.setAttribute("message", "ERROR!  Cannot determine correct Hartford destination directory.  Please contact Technical Support.");
		} else {
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory();
	
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
	
			// Check that we have a file upload request
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			
			if (isMultipart) {
				logger.info("isMultipart: " + isMultipart);
				List items = upload.parseRequest(request);
				
				// Process the uploaded items
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
				    FileItem item = (FileItem) iter.next();
		
				 // Process a file upload
				    if (!item.isFormField()) {
				        String fullFileName = item.getName();
				        String fileName = StringUtil.getLastToken(fullFileName, '\\');
				        String contentType = item.getContentType();
				        long sizeInBytes = item.getSize();
				        
				        File uploadedFile = new File(hartfordDir + fileName);
				        item.write(uploadedFile);
				        
				        request.setAttribute("message", "The local file (" + fullFileName + ") has been uploaded to the directory: " + hartfordDir);
				    }
				}
			}
		}
		
		// forward to the page
		forward(jspPage, request, response);		
	}
	
	
	protected void performProtectedDBOperations(DBConnection conn, Object data, String action, 
			HttpServletRequest request, HttpServletResponse response) throws Exception {
	}
}
