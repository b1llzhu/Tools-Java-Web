/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
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
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.*;

/**
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: GenericUploadServlet.java,v 1.3 2008/06/02 15:33:53 dyoung Exp $
 */
public class GenericUploadServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(GenericUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/GenericUploadServlet.java,v 1.3 2008/06/02 15:33:53 dyoung Exp $";
	
	private static PropertyManager properties = new PropertyManager("/properties/genericUpload.properties");
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Entering servlet - action: " + action);
		
		String jspPage = "genericUpload.jsp";
		
		String configFileExt = ".xml";
		String configFileDefaultDir = "etc/";
		
		String baseDir = properties.getProperty("baseDir");
		logger.info("baseDir: " + baseDir);
		if (!baseDir.endsWith("/"))
			baseDir = baseDir.concat("/");
		
		String uploadAppl = null;
		String uploadCfg = null;
		String uploadKey = null;
		String fatalMsg = "";

		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = 
			(WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
		
		try {
			
			// pull things off of the URL and validate as need be
			
			File fileUploadCfg = null;
			Map<String, Map<String, Object>> uploadMap = new HashMap<String, Map<String, Object>>();
			List<String> uploadKeys = new ArrayList<String>();
					
			uploadAppl = "".equals(request.getParameter("appl")) ? (String)request.getAttribute("appl") : request.getParameter("appl");
			if ("".equals(uploadAppl) || uploadAppl == null)
				fatalMsg = "ERROR:  Missing required parameter (APPL) required.";

			uploadCfg = "".equals(request.getParameter("config")) ? (String)request.getAttribute("config") : request.getParameter("config");
			if ("".equals(uploadCfg) || uploadCfg == null)
				fatalMsg = fatalMsg.concat("ERROR:  Missing required parameter (CONFIG) required.");

			// test to make sure we can find the config file that was handed to us
			if (StringUtil.hasValue(uploadCfg)) {
				fileUploadCfg = new File(baseDir + uploadAppl + "/" + configFileDefaultDir + uploadCfg + configFileExt);
				if (!fileUploadCfg.exists())
					fatalMsg = fatalMsg.concat("ERROR:  The supplied config file doesn't exist (" + fileUploadCfg.getAbsolutePath() + ").");
			}
			
			// if we have a fatal message - stop processing and skip to the end
			if (!"".equals(fatalMsg)) {
				request.setAttribute("fatalMsg", fatalMsg);
			}
			
			uploadKey = request.getParameter("uploadKey");

			//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				
			// for this section, we'll loop through our config file and build a map that will be used in each processing section
			// this is also the part where we'll validate the config file has everything we need
			
			// need to get a list of all the upload keys in our upload file
			SimpleNode doc = new SimpleNode(XmlUtil.loadDocumentFromFile(fileUploadCfg.getAbsolutePath()));
			
			// get all the upload configs
			NodeList uploads;
			try {
				uploads = doc.getSimpleNode("{hrUpload}{uploads}").getChildNodes("upload");
				if (uploads == null)
					throw new Exception();
			} catch (Exception e) {
				throw new Exception("Couldn't get uploads child node list");
			}

			Integer uploadCnt = 0;
			for (int i = 0; i < uploads.getLength(); i++) {
				uploadCnt++;
				Map<String, Object> typeMap = new HashMap<String, Object>();
				
				SimpleNode uploadNode = new SimpleNode(uploads.item(i));
				
				if (uploadNode.getNodeType() != Node.TEXT_NODE) {

					String cfgUploadType;
					try {
						cfgUploadType = uploadNode.getAttribute("type");
						if ("".equals(cfgUploadType) || cfgUploadType == null)
							throw new Exception("");
					} catch (Exception e) {
						throw new Exception("cfgUploadType attribute not found");
					}
					uploadKeys.add(cfgUploadType);
//						logger.info("  adding (" + cfgUploadType + ")");
					
					String cfgName;
					try {
						cfgName = uploadNode.getTextContent("{name}");
//							logger.info("[" + cfgUploadType + "]cfgName: " + cfgName);
						if ("".equals(cfgName) || cfgName == null)
							throw new Exception("");
					} catch (Exception e) {
						throw new Exception("[" + cfgUploadType + "]cfgName value not found");
					}
					typeMap.put("name", cfgName);
					
					String cfgDestDir;
					try {
						cfgDestDir = uploadNode.getTextContent("{destDir}");
//							logger.info("[" + cfgUploadType + "]cfgDestDir: " + cfgDestDir);
						if ("".equals(cfgDestDir) || cfgDestDir == null)
							throw new Exception("[" + cfgUploadType + "]cfgDestDir value not found");
						File destDirObj = new File(cfgDestDir);
						if (!destDirObj.exists()) {
							throw new Exception("[" + cfgUploadType + "]cfgDestDir directory (" + destDirObj.getAbsolutePath() + ") does not exists or cannot be found");
						} else if (destDirObj.exists() && !destDirObj.canWrite()) {
							throw new Exception("[" + cfgUploadType + "]cfgDestDir directory found (" + destDirObj.getAbsolutePath() + ") but is not writable");
						}
					} catch (Exception e) {
						throw new Exception(e.getMessage());
					}
					typeMap.put("destDir", cfgDestDir);
					
					String cfgAuthUsers;
					List<String> cfgAuthUserList = null;
					try {
						cfgAuthUsers = uploadNode.getTextContent("{authorizedUsers}");
//							logger.info("[" + cfgUploadType + "]cfgAuthUsers: " + cfgAuthUsers);
						if ("".equals(cfgAuthUsers) || cfgAuthUsers == null) {
							throw new Exception("[" + cfgUploadType + "]authorizediUsers value not found");
						}
					
						File usersFile = new File(cfgAuthUsers);
						if (!usersFile.exists()) 
							throw new Exception("[" + cfgUploadType + "]userFile does not exist");
						String s = FileUtil.loadFile(usersFile.getAbsolutePath());
						s = s.toLowerCase();
						cfgAuthUserList = StringUtil.toList(s, "\n ");
	//							logger.info("[" + cfgUploadType + "]cfgAuthUserList: " + cfgAuthUserList);
					} catch (Exception e) {
						throw new Exception(e.getMessage());
					}
					typeMap.put("authUserList", cfgAuthUserList);						

					uploadMap.put(cfgUploadType, typeMap);
//						logger.info("adding typeMap to master upload map: " + uploadMap);
				}				
			}
			
			if (uploadCnt == 0) {
				throw new Exception("no uploaders were found");
			}

			Collections.sort(uploadKeys);
			
			request.setAttribute("uploadKeys", uploadKeys);
			request.setAttribute("uploadKey", "");

			//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
			// this section is only performed when the action <> choose
			// only when the jsp is trying to upload a file OR the first time the JSP is hit
			// for this reason, we lock down any processing to when the incoming form is multipart content
			
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			
			if (isMultipart) {

				// verify the authorization (a second check - the first one is upon selecting the upload key)
				Map currentMap = uploadMap.get(uploadKey);
				List authUserList = (List) currentMap.get("authUserList");
				if (!authUserList.contains(winPrincipal.getName().toLowerCase())) {
					logger.info("current user (" + winPrincipal.getName() + ") is not authorized to upload files to (" + request.getSession().getAttribute("uploadKey") + ")");
					request.setAttribute("errMessage", "Sorry!  You don't have access to upload files for " + uploadKey + ".");
					request.setAttribute("unauthorized", "true");
					
				} else {

					List items = upload.parseRequest(request);
					
					// Process the uploaded items
					Iterator iter = items.iterator();
					while (iter.hasNext()) {
					    FileItem item = (FileItem) iter.next();
					    logger.info("item: " + item.getName());
			
					 // Process a file upload
					    if (!item.isFormField()) {
					        String fullFileName = item.getName();
					        String fileName = StringUtil.getLastToken(fullFileName, '\\');
					        
					        String destDir = (String) currentMap.get("destDir");
					        if (!destDir.endsWith("/"))
					        	destDir = destDir.concat("/");
					        
					        File fileToCreate = new File(destDir + fileName);
					        
					        // check to see if the file already exists
					        if (fileToCreate.exists()) {
					        	request.setAttribute("errMessage", "ERROR!  File (" + fileName + ") already exists and is waiting to be processed!  It was not uploaded again.");
					        } else {
					        	item.write(fileToCreate);
						        request.setAttribute("message", "The local file (" + fileName + ") has been successfully uploaded.");
					        }
					    }
					}
				}
			}
			
			//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			
			// always perform this file list if we're in the context of an uploader
			if (!"".equals(uploadKey) && uploadKey != null) {
				
				Map currentMap = uploadMap.get(uploadKey);
				File destDir = new File((String)currentMap.get("destDir"));
					
				File[] files = null;
				List<Map> fileList = new ArrayList<Map>();
				SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm");
	
				if (destDir.exists()) {
					files = destDir.listFiles();
					
					// sort the list of files by modified date ascending
					Arrays.sort( files, new Comparator() {
						public int compare(Object o1, Object o2) {
							if (((File)o1).lastModified() > ((File)o2).lastModified()) {
								return +1;
							} else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
								return -1;
							} else {
								return 0;
							}
						}
					});
					
					for (int j = 0; j < files.length; j++) {
						Map fileMap = new HashMap();
						File file = files[j];
						
						if (!file.isDirectory()) {
							fileMap.put("name", file.getName());
							fileMap.put("lastModified", df.format(file.lastModified()));
							fileList.add(fileMap);
						}
					}
				}
				request.setAttribute("fileList", fileList);
				
				// one of the last things we'll do is perform an authorization check
				// if we're not authorized, we'll set a flag so that and that will help control
				// the display
				// (there's also a second check during the upload of the file just to make
				// sure nothing slips through)
				currentMap = uploadMap.get(uploadKey);
				List authUserList = (List) currentMap.get("authUserList");
				if (!authUserList.contains(winPrincipal.getName())) {
					logger.info("current user (" + winPrincipal.getName() + ") is not authorized to upload files to (" + request.getSession().getAttribute("uploadKey") + ")");
					request.setAttribute("errMessage", "Sorry!  You don't have access to upload files for " + uploadKey + ".");
					request.setAttribute("unauthorized", "true");
				}
			}

		} catch (Exception e) {			
			request.setAttribute("errMessage", "ERROR:  There was an unforeseen error.  Please advise Technical Support (" + e.getMessage() + ").");
			logger.error("", e);
		}
		
		// put some things into the request
//		request.setAttribute("winLoginPageTitle", "File Upload Utility");
//		request.setAttribute("referrer", "genericUpload");
//		request.setAttribute("action", request.getAttribute("action"));
//		request.setAttribute("querystring", queryString);
		request.setAttribute("appl", uploadAppl);
		request.setAttribute("config", uploadCfg);
		request.setAttribute("uploadKey", uploadKey);

		// always put back in the logged in credentials
		request.setAttribute("winIsLoggedIn", winPrincipal);
		
		if (StringUtil.hasValue(uploadKey))
			request.setAttribute("uploadKeyDisplay", " - " + uploadKey);
		
		// forward to the page
		forward(jspPage, request, response);		
	}
}
