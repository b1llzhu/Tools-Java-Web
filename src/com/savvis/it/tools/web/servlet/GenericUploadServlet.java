/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.rpc.encoding.TypeMapping;

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
import com.savvis.it.tools.RunInfoUtil;
import com.savvis.it.tools.web.bean.InputFieldHandler;
import com.savvis.it.util.*;

/**
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: GenericUploadServlet.java,v 1.21 2008/09/03 17:06:29 telrick Exp $
 */
public class GenericUploadServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(GenericUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/GenericUploadServlet.java,v 1.21 2008/09/03 17:06:29 telrick Exp $";
	
	private static PropertyManager properties = new PropertyManager("/properties/genericUpload.properties");
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String jspPage = "genericUpload.jsp";
		
		String configFileExt = ".xml";
		String configFileDefaultDir = "config/misc/";
		
		Map<String, String> pageMap = new HashMap<String, String>();

		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = null;
		
		String basedir = null;
		if (!ObjectUtil.isEmpty(properties.getProperty("basedir"))) {
			basedir = properties.getProperty("basedir");
		} else {
			basedir = SystemUtil.getProperty("basedir");
		}
		
		try {

			if (basedir == null)
				throw new Exception("BASEDIR not set");
				
			if (basedir.endsWith("/"))
				basedir = basedir.substring(0, basedir.length()-1);
			
			winPrincipal = (WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
					
			//////////////////////////////////////////////////////////////////////////////////////
			// URL validation
			//////////////////////////////////////////////////////////////////////////////////////
			Map<String, Map<String, Object>> uploadMap = new HashMap<String, Map<String, Object>>();
					
			pageMap.put("appl", "".equals(request.getParameter("appl")) ? (String)request.getAttribute("appl") : request.getParameter("appl"));
			if (ObjectUtil.isEmpty(pageMap.get("appl")))
				pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  Missing required parameter (APPL) required.<br/>");

			pageMap.put("config", "".equals(request.getParameter("config")) ? (String)request.getAttribute("config") : request.getParameter("config"));
			if (ObjectUtil.isEmpty(pageMap.get("config"))) {
				pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  Missing required parameter (CONFIG) required.<br/>");
			} else {
				// test to make sure we can find the config file that was handed to us
				File uploadFile = new File(basedir + "/" + pageMap.get("appl") + "/" + configFileDefaultDir + pageMap.get("config") + configFileExt);
				if (!uploadFile.exists()) {
					pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  The supplied config file doesn't exist (" + uploadFile.getAbsolutePath() + ").<br/>");
				} else {
					pageMap.put("uploadFile", uploadFile.getAbsolutePath());
				}
			}
			
			pageMap.put("key", request.getParameter("key"));
			
			// if we have a fatal message - stop processing and skip to the end
			if (!ObjectUtil.isEmpty(pageMap.get("fatalMsg"))) {
				request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			}
			
			//////////////////////////////////////////////////////////////////////////////////////
			// CONFIG validation
			//////////////////////////////////////////////////////////////////////////////////////
			SimpleNode doc = new SimpleNode(XmlUtil.loadDocumentFromFile(pageMap.get("uploadFile").toString()));
			Map<String, Map<String, Object>> configMap = new HashMap<String, Map<String, Object>>();
			
			List<String> messages = validateConfig(doc, pageMap, configMap);
			
			if (messages.size() > 0) {
				for (int i = 0; i < messages.size(); i++) {
					String msg = messages.get(i);
					if (i == 0) {
						pageMap.put("fatalMsg", msg + "<br/>");
					} else {
						pageMap.put("fatalMsg", pageMap.get("fatalMsg").toString().concat(msg + "<br/>"));
					}
				}
				request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			}
				
			
			//////////////////////////////////////////////////////////////////////////////////////
			// UPLOAD key list
			//////////////////////////////////////////////////////////////////////////////////////
			// to make things easier for the display, let's create a list of uploader keys and sort it
			List<Map<String, String>> uploads = new ArrayList<Map<String, String>>();
			
			// properly sort the list by descriptive name
			ArrayList<String> sortedKeys = new ArrayList<String>();
			for (String uploadKey : configMap.keySet()) {
				sortedKeys.add(configMap.get(uploadKey).get("name").toString() + "::" + uploadKey);
			}
			Collections.sort(sortedKeys);
			
			// now, create a list of maps for the display
			for (int i = 0; i < sortedKeys.size(); i++) {
				String uploadCombo = sortedKeys.get(i);

				// split to get the pieces
				String[] uploadData = uploadCombo.split("::");
				
				Map<String, String> upload = new HashMap<String, String>();
				upload.put("name", uploadData[0]);
				upload.put("key", uploadData[1]);
				
				// authorize the user to see the upload
				AuthorizationObject authObject = isAuthorized(configMap.get(uploadData[1]), winPrincipal);
				
				if (authObject.isAuthorized)
					uploads.add(upload);
			}
			request.setAttribute("uploads", uploads);

			
			//////////////////////////////////////////////////////////////////////////////////////
			// PERFORM ACTION functionality
			//////////////////////////////////////////////////////////////////////////////////////
			if (!ObjectUtil.isEmpty(request.getParameter("action")) && "execute".equals(request.getParameter("action"))) {
				Map keyMap = configMap.get(pageMap.get("key"));
				Context inputsContext = new Context();

				// get parms and add into context for command line keyword substitution 
				for (int i = 0; i < request.getParameterMap().keySet().size(); i++) {
					String parm = (String) request.getParameterMap().keySet().toArray()[i];
					inputsContext.add("input", parm, request.getParameter(parm));
				}
				
				// add the username into the context
				inputsContext.add("global", "username", winPrincipal.getName());
				
				// retrieve the commands from the config
				Map actionsMap = (Map) keyMap.get("actions");
				Map actionMap = (Map) actionsMap.get(inputsContext.get("input", "action_name"));
				List<Map<String, Object>> commands = (List<Map<String, Object>>) actionMap.get("cmds");

				// execute the command in another thread
				try {
//					logger.info("actionMap: " + actionMap);
//					logger.info("actionMap.get(\"cmdType\"): " + actionMap.get("cmdType"));
					if ("class".equals(actionMap.get("cmdType"))) {
						
						// only the first one is read here
						logger.info("commands.get(0).get(\"cmdString\"): " + commands.get(0).get("cmdString"));
						String className = inputsContext.keywordSubstitute((String)commands.get(0).get("cmdString"));
						logger.info("className: " + className);
						logger.info("commands.get(0).get(\"argString\"): " + commands.get(0).get("argString"));
						String args = inputsContext.keywordSubstitute((String)commands.get(0).get("argString"));
						logger.info("args: " + args);
						
						Map propertyMap = (Map) commands.get(0).get("properties");
						for (Object key : propertyMap.keySet()) {
							logger.info("Setting System property "+key+" to "+propertyMap.get(key));
							System.setProperty((String) key, (String) propertyMap.get(key));
						}
						
						Class clp = Class.forName(className);
						Method m = clp.getMethod("main", String[].class);
						m.invoke(null, new Object[] { args.split(" ") });

						request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has completed.");
					}
					
					if ("shell".equals(actionMap.get("cmdType"))) {
						// create a string array for the command line processor
						String[] clpCmdArray = new String[commands.size()];

						CommandLineProcess clp = new CommandLineProcess();
						for (int i = 0; i < 1 ; i++) {
							Map<String, Object> cmdMap = commands.get(i);
							String cmd = inputsContext.keywordSubstitute(cmdMap.get("cmdString") + " " + cmdMap.get("argString"));
							logger.info("cmd: " + cmd);
//							clp.setWaitForProcess(false);
							clp.setDir(new File((String) cmdMap.get("startDir")));
							Context envContext = new Context();
							envContext.fillWithEnvAndSystemProperties();
							Map propertyMap = (Map) cmdMap.get("properties");
							List<String> envList = new ArrayList<String>();
							for (Object key : propertyMap.keySet()) 
								envList.add(key+"="+propertyMap.get(key));
							Map<String, String> envMap = System.getenv();
							for (Object key : envMap.keySet()) 
								envList.add(key+"="+envMap.get(key));
							envList.add("CALLED_BY_USER="+winPrincipal.getName());
							clp.setEnvp((String[])envList.toArray(new String[] {}));
							clp.run(cmd);
						}
						
						logger.info("output: "+clp.getOutput());
						logger.info("error: "+clp.getError());

						request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has started execution.");
					}
				

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e);
					request.setAttribute("errMessage", "There was an error executing the action \"" + actionMap.get("display") + "\".  (" + e + ")");
				}
			}
			
			
			//////////////////////////////////////////////////////////////////////////////////////
			// UPLOAD functionality
			//////////////////////////////////////////////////////////////////////////////////////
			// this section is only performed when the action <> choose
			// only when the jsp is trying to upload a file OR the first time the JSP is hit
			// for this reason, we lock down any processing to when the incoming form is multipart content
			
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory(0, null);
			ServletFileUpload upload = new ServletFileUpload(factory);
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			
			if (isMultipart) {

				// verify the authorization (a second check - the first one is upon selecting the upload key)
				Map keyMap = configMap.get(pageMap.get("key"));
				Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");
				List<String> authUserList = (List<String>) keyMap.get("authorizedUserList");
				
				AuthorizationObject authObject = isAuthorized(keyMap, winPrincipal);
				request.setAttribute("authorized", authObject.isAuthorized);
				if (!authObject.isAuthorized) {
					request.setAttribute("errMessage", authObject.getMessage());

//				if (!authUserList.contains(winPrincipal.getName().toLowerCase())) {
//					logger.info("User (" + winPrincipal.getName() + ") is not authorized to upload files to (" + pageMap.get("key") + ")");
//					request.setAttribute("errMessage", "Sorry!  You don't have access to upload files for " + pageMap.get("key") + ".");
//					request.setAttribute("unauthorized", "true");
//					
				} else {

					List items = upload.parseRequest(request);
					
					// Process the uploaded items
					Iterator iter = items.iterator();
					while (iter.hasNext()) {
					    FileItem item = (FileItem) iter.next();
			
					    // Process a file upload
					    if (!item.isFormField()) {
					        String fullFileName = item.getName();
					        String fileName = StringUtil.getLastToken(fullFileName, '\\');
					        
					        String destDir = (String) directoriesMap.get("pending").get("path");
					        if (!destDir.endsWith("/"))
					        	destDir = destDir.concat("/");
					        
					        File fileToCreate = new File(destDir + fileName);
					        
					        // check to see if the file already exists
					        if (fileToCreate.exists()) {
					        	request.setAttribute("errMessage", "ERROR!  File (" + fileName + ") already exists and is waiting to be processed!  It was not uploaded again.");
					        	
					        // otherwise, write the file
					        } else {
						        // if we have a reg ex, check to see if the file name matches it
					        	if (!ObjectUtil.isEmpty(keyMap.get("fileNameRegEx")) && !fileName.matches(keyMap.get("fileNameRegEx").toString())) {
					        		request.setAttribute("errMessage", "ERROR!  Filename Matching Error (" + fileName + ") doesn't match " + keyMap.get("fileNameRegExText") + ".  The file was not uploaded.");
					        	} else {
						        	item.write(fileToCreate);
									RunInfoUtil.addLog(keyMap.get("path").toString(), fileToCreate, winPrincipal.getName(), new java.util.Date(), 
											"File " + fileName + " uploaded");

							        request.setAttribute("message", "The local file (" + fileName + ") has been successfully uploaded.");
					        	}
					        }
					    }
					}
				}
			}
			////// end of multipart upload functionality
			

			
			//////////////////////////////////////////////////////////////////////////////////////
			// DOWNLOAD functionality
			//////////////////////////////////////////////////////////////////////////////////////
			String downloadFlag = "".equals(request.getParameter("download")) ? (String)request.getAttribute("download") : request.getParameter("download");
			String downloadFile = "".equals(request.getParameter("file")) ? (String)request.getAttribute("file") : request.getParameter("file");
			String downloadKey = "".equals(request.getParameter("dir")) ? (String)request.getAttribute("dir") : request.getParameter("dir");
			
			if ("1".equals(downloadFlag)) {
				
				if (ObjectUtil.isEmpty(downloadFile)) {
					request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
				} else {
					if (ObjectUtil.isEmpty(downloadKey)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
					} else {
						Map keyMap = configMap.get(pageMap.get("key"));
						Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");
						logger.info("directoriesMap: " + directoriesMap);
						request.setAttribute("win", winPrincipal);
						request.setAttribute("file", downloadFile);
						request.setAttribute("addtl", downloadKey);
						request.setAttribute("path", keyMap.get("path"));
						request.setAttribute("source", directoriesMap.get(downloadKey).get("description"));
						jspPage = "download";
					}
				}
			}
			

			//////////////////////////////////////////////////////////////////////////////////////
			// MOVE functionality
			//////////////////////////////////////////////////////////////////////////////////////
			String moveType = "".equals(request.getParameter("type")) ? (String)request.getAttribute("type") : request.getParameter("type");
			String moveFile = "".equals(request.getParameter("file")) ? (String)request.getAttribute("file") : request.getParameter("file");
			String moveFilePath = "".equals(request.getParameter("path")) ? (String)request.getAttribute("path") : request.getParameter("path");
			String moveTarget = "".equals(request.getParameter("target")) ? (String)request.getAttribute("target") : request.getParameter("target");
			String moveDescription = "".equals(request.getParameter("description")) ? (String)request.getAttribute("description") : request.getParameter("description");
			
			if (!ObjectUtil.isEmpty(moveType)) {
				if ("move".equals(moveType)) {
					if (ObjectUtil.isEmpty(moveFile)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
					} else {
						if (ObjectUtil.isEmpty(moveFilePath)) {
							request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
						} else {
							Map keyMap = configMap.get(pageMap.get("key"));
							Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");
							
							if (!moveFilePath.endsWith("/"))
								moveFilePath = moveFilePath.concat("/");
							
							// verify the target directory is in the map
							if (ObjectUtil.isEmpty(directoriesMap.get(moveTarget))) {
								request.setAttribute("fatalMsg", "ERROR: Invalid value for target directory (" + moveTarget + ".<br/>");
							} else {
								// log and perform the move
								logger.info("keyMap.get(\"path\") + directoriesMap.get(moveTarget).get(\"path\") + moveFile: " + keyMap.get("path") + directoriesMap.get(moveTarget).get("path") + moveFile);
								FileUtil.moveFile(moveFilePath + moveFile, directoriesMap.get(moveTarget).get("path") + moveFile);
								
								RunInfoUtil.addLog(keyMap.get("path").toString(), new File(moveFilePath + moveFile), winPrincipal.getName(), new java.util.Date(), 
										"File " + moveFile + " moved to \"" + directoriesMap.get(moveTarget).get("description") + "\" directory");
							}
						}
					}
				}
			}
			

			//////////////////////////////////////////////////////////////////////////////////////
			// CURRENT KEY logic
			//////////////////////////////////////////////////////////////////////////////////////
			// functionality to perform if we're in the context of an uploader
			if (!ObjectUtil.isEmpty(pageMap.get("key"))) {
				Map keyMap = configMap.get(pageMap.get("key"));
				
				Map<String, Map<String, Object>> directoriesMap = (Map<String, Map<String, Object>>) keyMap.get("directories");
//				logger.info("directoriesMap: " + directoriesMap);
				
				for (int i = 0; i < directoriesMap.keySet().size(); i++) {
					String key = (String) directoriesMap.keySet().toArray()[i];
					Map<String, Object> directoryMap = (Map<String, Object>) directoriesMap.get(key);

					if (!"0".equals(directoryMap.get("display"))) {
						Integer limit = null;
						if (!ObjectUtil.isEmpty(directoryMap.get("limit")))
							limit = Integer.parseInt(directoryMap.get("limit").toString());
						
						List<Map<String, String>> fileList = getFileList(directoryMap.get("path").toString(), limit, directoryMap.get("sort").toString());
						directoryMap.put("data", fileList);
						
						if (fileList.size() > 0) {
							directoryMap.put("sizeAlgorithm", ((fileList.size() * 23) + 23) > 255 ? 255 : ((fileList.size() * 23) + 23));
						}
					}
				}
				
				if (StringUtil.hasValue(keyMap.get("name").toString()))
					request.setAttribute("uploadKeyDisplay", " - " + keyMap.get("name").toString());
				
				if (!ObjectUtil.isEmpty(keyMap.get("actions"))) {
					request.setAttribute("hasActions", "1");
					request.setAttribute("actions", keyMap.get("actions"));
				}
				
				// one of the last things we'll do is perform an authorization check
				// if we're not authorized, we'll set a flag so that and that will help control
				// the display
				// (there's also a second check during the upload of the file just to make
				// sure nothing slips through)
				AuthorizationObject authObject = isAuthorized(keyMap, winPrincipal);
				if (!authObject.isAuthorized) {
					request.setAttribute("errMessage", authObject.getMessage());
				}
				request.setAttribute("authorized", authObject.isAuthorized);
//				List authUserList = (List) keyMap.get("authorizedUserList");
//				if (!authUserList.contains(winPrincipal.getName().toLowerCase())) {
//					logger.info("current user (" + winPrincipal.getName() + ") is not authorized to upload files to (" + request.getSession().getAttribute("uploadKey") + ")");
//					
//				}
				
				request.setAttribute("allowUpload", keyMap.get("allowUpload"));
				request.setAttribute("keyMap", keyMap);
				request.setAttribute("directories", keyMap.get("directories"));
			}
			////// end of current key logic
			

		} catch (Exception e) {
			logger.error("", e);
		}
			
		// put some things into the request
		request.setAttribute("basedir", basedir);
		request.setAttribute("appl", pageMap.get("appl"));
		request.setAttribute("config", pageMap.get("config"));
		request.setAttribute("key", pageMap.get("key"));

		// always put back in the logged in credentials
		request.setAttribute("isLoggedIn", winPrincipal);
		
		// forward to the page
		forward(jspPage, request, response);		
	}
	
	private String getFatalMsg(Map pageMap) {
		if(pageMap.get("fatalMsg") == null)
			return "";
		return (String) pageMap.get("fatalMsg");
	}
	
	private List<Map<String, String>> getFileList(String path, Integer limit, String sortOrder) {
		File directory = new File(path);
//		logger.info("directory: " + directory);
		
		Integer fileLimit = 50;
		if (!ObjectUtil.isEmpty(limit)) {
			fileLimit = limit;
		}
		
		File[] files = null;
		List<Map<String, String>> fileList = new ArrayList<Map<String, String>>();
		SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy h:mm a");

		if (directory.exists()) {
			files = directory.listFiles();
//			logger.info("files: " + files);
			
			if ("d".toLowerCase().equals(sortOrder)) {
				// sort the list of files by modified date descending
				Arrays.sort( files, new Comparator() {
					public int compare(Object o1, Object o2) {
						if (((File)o1).lastModified() > ((File)o2).lastModified()) {
							return -1;
						} else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
							return +1;
						} else {
							return 0;
						}
					}
				});
			} else {
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
			}

			for (int j = 0; j < files.length; j++) {
				Map<String, String> fileMap = new HashMap<String, String>();
				File file = files[j];
				
				Long fileAge = (System.currentTimeMillis() - file.lastModified()) / (1000 * 60);
				
				if (!file.isDirectory()) {
					fileMap.put("name", file.getName());
					fileMap.put("lastModified", df.format(file.lastModified()));
					fileMap.put("path", file.getParent().replace('\\', '/'));
					fileMap.put("age", fileAge.toString());
					
//					logger.info("fileMap: " + fileMap);
					fileList.add(fileMap);
				}
			}
		}
		
		// trim to the limit
//		while (fileList.size() > fileLimit) {
//			fileList.remove(fileList.size()-1);
//		}
//		logger.info("fileList: " + fileList);
		return fileList;
	}
	
	private AuthorizationObject isAuthorized(Map keyMap, WindowsPrincipal winPrincipal) throws Exception {
		AuthorizationObject authObject = new AuthorizationObject();
		
		// always check for the authList for legacy compatability, then check for the newer "auth" sections for the different kinds
			List authUserList = (List) keyMap.get("authorizedUserList");
			if (!ObjectUtil.isEmpty(keyMap.get("authorizedUserList"))) {
			if (authUserList.contains(winPrincipal.getName().toLowerCase())) {
				authObject.setIsAuthorized(true);
			} else {
				logger.info("current user (" + winPrincipal.getName() + ") is not authorized to use the file utility for (" + keyMap.get("type") + ")");
				authObject.setMessage("Sorry!  You don't have access to use the file utility for " + keyMap.get("type") + ".");
				authObject.setIsAuthorized(false);
			} 
			return authObject;
		}

		
		return authObject;
	}
	
	private List<String> validateConfig(SimpleNode doc, Map<String, String> pageMap, Map<String, Map<String, Object>> configMap) throws Exception {
		
		List<String> messages = new ArrayList<String>();
		
		SimpleNode.setEnableKeywordSubstitution(true);
		
		// get all the upload configs
		NodeList uploads;
		
		if (ObjectUtil.isEmpty(doc.getSimpleNode("{genericUpload}{uploads}"))) {
			messages.add("No uploads found.  Please configure at least one upload in the " + pageMap.get("uploadFile") + " config file.");
		} else {
			if (doc.getSimpleNode("{genericUpload}{uploads}").getChildNodes("upload").getLength() == 0) {
				messages.add("No uploads found.  Please configure at least one upload in the " + pageMap.get("uploadFile") + " config file.");
			} else {
				uploads = doc.getSimpleNode("{genericUpload}{uploads}").getChildNodes("upload");
				
				// loop through the uploaders, validate and cache the info
				Integer uploadCnt = 0;
				for (int i = 0; i < uploads.getLength(); i++) {
					uploadCnt++;
					Map<String, Object> uploadMap = new HashMap<String, Object>();
					
					SimpleNode uploadNode = new SimpleNode(uploads.item(i));
					
					if (uploadNode.getNodeType() != Node.TEXT_NODE) {
						String type = "";
						String typeLog = "";
						
						if (ObjectUtil.isEmpty(uploadNode.getAttribute("type"))) {
							messages.add("[Upload #" + uploadCnt + "] type attribute not found");
							return messages;
						} else {
							uploadMap.put("type", uploadNode.getAttribute("type"));
							typeLog = "[" + uploadNode.getAttribute("type") + "]";
							type = uploadNode.getAttribute("type");
						}
						
						if (ObjectUtil.isEmpty(uploadNode.getAttribute("allowUpload"))) {
							// default to yes
							uploadMap.put("allowUpload", "1");
						} else {
							uploadMap.put("allowUpload", uploadNode.getAttribute("allowUpload"));
						}
						
						if (!ObjectUtil.isEmpty(uploadNode.getAttribute("fileNameRegEx"))) {
							uploadMap.put("fileNameRegEx", uploadNode.getAttribute("fileNameRegEx"));
						}
						
						if (!ObjectUtil.isEmpty(uploadNode.getAttribute("fileNameRegExText"))) {
							uploadMap.put("fileNameRegExText", uploadNode.getAttribute("fileNameRegExText"));
						}
						
						if (ObjectUtil.isEmpty(uploadNode.getTextContent("name"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " name keyword not found");
						} else {
							uploadMap.put("name", uploadNode.getTextContent("name"));
						}

						if (ObjectUtil.isEmpty(uploadNode.getTextContent("path"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " path keyword not found");
						} else {
							if (uploadNode.getTextContent("path").endsWith("/")) {
								uploadMap.put("path", uploadNode.getTextContent("path").substring(0, uploadNode.getTextContent("path").length()-1));
							} else {
								uploadMap.put("path", uploadNode.getTextContent("path"));
							}
						}

						// process the directories
						if (!ObjectUtil.isEmpty(uploadNode.getSimpleNode("{directories}"))) {
							NodeList diretoriesNode = uploadNode.getSimpleNode("{directories}").getChildNodes("directory");
							Map<String, Map<String, Object>> directoriesMap = new LinkedHashMap<String, Map<String, Object>>();
							
							try {
								for (int j = 0; j < diretoriesNode.getLength(); j++) {
									SimpleNode directoryNode = new SimpleNode(diretoriesNode.item(j));
									Map<String, Object> directoryMap = new HashMap<String, Object>();
									
									String directoryKey = null;
									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("key"))) {
										directoryKey = directoryNode.getAttribute("key");
									} else {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j + " required a 'key' attribute");
										continue;
									}
									
									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("display")))
										directoryMap.put("display", ("1".equals(directoryNode.getAttribute("display")) ? "1" : "0"));
										
									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("writable")))
										directoryMap.put("writable", ("1".equals(directoryNode.getAttribute("writable")) ? "1" : "0"));

									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("limit"))) {
										try {
											Integer fileLimit = Integer.parseInt(directoryNode.getAttribute("limit").toString());
											directoryMap.put("limit", fileLimit.toString());
										} catch (Exception e) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j + " - 'limit' value must be an integer");
										}
									}

									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("sort")))
										directoryMap.put("sort", ("a".equals(directoryNode.getAttribute("sort")) ? "a" : "d"));

									if (!ObjectUtil.isEmpty(directoryNode.getTextContent("{description}")))
										directoryMap.put("description", directoryNode.getTextContent("{description}"));

									if (!ObjectUtil.isEmpty(directoryNode.getTextContent("{subDescription}")))
										directoryMap.put("subDescription", directoryNode.getTextContent("{subDescription}"));

									// path validation
									String path = uploadMap.get("path") + "/" + directoryNode.getAttribute("key");
									File dir = new File(path);
									if (!dir.exists()) {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j + " (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
									} else if (dir.exists() && !dir.canWrite() && "1".equals(directoryMap.get("writable"))) {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir " + j + " (" + dir.getAbsolutePath() + ") exists but is not writable");
									}
									if (!path.endsWith("/"))
										path = path + "/";

									directoryMap.put("path", path);
									
									// columns
									if (!ObjectUtil.isEmpty(directoryNode.getSimpleNode("{columns}"))) {
										NodeList columnsNode = directoryNode.getSimpleNode("{columns}").getChildNodes("column");
										Map<String, Map<String, String>> columnsMap = new LinkedHashMap<String, Map<String, String>>();

										for (int k = 0; k < columnsNode.getLength(); k++) {
											SimpleNode columnNode = new SimpleNode(columnsNode.item(k));
											Map<String, String> columnMap = new HashMap<String, String>();

											columnMap.put("name", columnNode.getAttribute("name"));
											columnMap.put("title", columnNode.getAttribute("title"));
											columnMap.put("download", columnNode.getAttribute("download"));
											
											columnsMap.put(columnMap.get("name"), columnMap);
										}
										directoryMap.put("columns", columnsMap);
									}
									
									
									// actions
									if (!ObjectUtil.isEmpty(directoryNode.getSimpleNode("{actions}"))) {
										NodeList actionsNode = directoryNode.getSimpleNode("{actions}").getChildNodes("action");
										Map<String, Map<String, String>> actionsMap = new LinkedHashMap<String, Map<String, String>>();

										for (int l = 0; l < actionsNode.getLength(); l++) {
											SimpleNode actionNode = new SimpleNode(actionsNode.item(l));
											Map<String, String> actionMap = new HashMap<String, String>();

											actionMap.put("level", actionNode.getAttribute("level"));
											actionMap.put("type", actionNode.getAttribute("type"));
											actionMap.put("fileAge", actionNode.getAttribute("fileAge"));
											actionMap.put("target", actionNode.getAttribute("target"));
											actionMap.put("description", actionNode.getAttribute("description"));
											
											actionsMap.put(ObjectUtil.toString(l), actionMap);
										}
										directoryMap.put("actions", actionsMap);
									}
									
									directoriesMap.put(directoryKey, directoryMap);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							uploadMap.put("directories", directoriesMap);
						}
						
						if (ObjectUtil.isEmpty(uploadNode.getTextContent("{authorizedUsers}"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedUsers keyword not found");
						} else {
							File file = new File(uploadNode.getTextContent("{authorizedUsers}"));
							if (!file.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedUsers file (" + file.getAbsolutePath() + ") does not exist or cannot be found");
							} else {
								String s = FileUtil.loadFile(file.getAbsolutePath());
								s = s.toLowerCase();
								uploadMap.put("authorizedUserList", StringUtil.toList(s, "\n "));
							}
						}

						// look for actions
						if (!ObjectUtil.isEmpty(uploadNode.getSimpleNode("{actions}"))) {
							NodeList actions = uploadNode.getSimpleNode("{actions}").getChildNodes("action");
							Map<String, Object> actionsMap = new LinkedHashMap<String, Object>();
							
							try {
								for (int j = 0; j < actions.getLength(); j++) {
									SimpleNode actionNode = new SimpleNode(actions.item(j));
									Map<String, Object> actionMap = new LinkedHashMap<String, Object>();
									
									// get action variables
									actionMap.put("name", actionNode.getAttribute("name").replace(" ", "_"));
									actionMap.put("type", actionNode.getAttribute("type"));
									
									actionMap.put("display", actionNode.getTextContent("{display}"));
									actionMap.put("buttonLabel", actionNode.getTextContent("{buttonLabel}"));
									actionMap.put("description", actionNode.getTextContent("{description}"));

									// get command(s)
									if (!ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}"))) {
										List<Map<String,Object>> cmds = new ArrayList<Map<String, Object>>();
										
										if (ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}").getAttribute("type"))) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " cmds keyword must have a type attribute of 'class' or 'shell'");
										} else {
											actionMap.put("cmdType", actionNode.getSimpleNode("{cmds}").getAttribute("type"));
											Map<String, Object> cmdMap = new HashMap<String, Object>();
										
											for (int k = 0; k < actionNode.getSimpleNode("{cmds}").getChildNodes("cmd").getLength(); k++) {
												SimpleNode cmdNode = new SimpleNode(actionNode.getSimpleNode("{cmds}").getChildNodes("cmd").item(k));
												cmdMap.put("cmdString", cmdNode.getTextContent("{cmdString}"));
												cmdMap.put("argString", cmdNode.getTextContent("{argString}"));
												cmdMap.put("startDir", cmdNode.getTextContent("{startDir}"));
												
												// get property values if present
												Map<String, String> propertyMap = new HashMap<String, String>();
												if (!ObjectUtil.isEmpty(cmdNode.getSimpleNode("{properties}"))) {
													for (int l = 0; l < cmdNode.getSimpleNode("{properties}").getChildNodes("property").getLength(); l++) {
														SimpleNode propertyNode = new SimpleNode(cmdNode.getSimpleNode("{properties}").getChildNodes("property").item(l));
														if (ObjectUtil.isEmpty(propertyNode.getAttribute("name"))) {
															messages.add("[Upload #" + uploadCnt + "]" + typeLog + " property keyword must have a name attribute");
														} else {
															propertyMap.put(propertyNode.getAttribute("name"), propertyNode.getTextContent());
														}
													}
												}
												cmdMap.put("properties", propertyMap);
											}
											cmds.add(cmdMap);
										}
										actionMap.put("cmds", cmds);
									}
									
									// get input variables
									if (!ObjectUtil.isEmpty(actionNode.getSimpleNode("{inputs}"))) {
										SimpleNode inputsNode = actionNode.getSimpleNode("{inputs}");
										Map<String, InputFieldHandler> inputsMap = InputFieldHandler.createInputs(inputsNode);
										actionMap.put("inputs", inputsMap);
									}
									
									actionsMap.put(actionMap.get("name").toString(), (Object) actionMap);
								}
								
								uploadMap.put("actions", actionsMap);
								
							} catch (Exception e) {
								e.printStackTrace();
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " error in actions section (" + ObjectUtil.toString(e) + ")");
							}
						}
						configMap.put(type, uploadMap);
					}				
				}				
			}
		}
		return messages;
	}
	
	private class AuthorizationObject {
		private Boolean isAuthorized;
		private String message;
		
		public Boolean getIsAuthorized() {
			return isAuthorized;
		}
		public void setIsAuthorized(Boolean isAuthorized) {
			this.isAuthorized = isAuthorized;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
	}
}
