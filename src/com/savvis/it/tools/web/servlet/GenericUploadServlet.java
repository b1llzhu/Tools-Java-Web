/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import com.savvis.it.util.*;

/**
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: GenericUploadServlet.java,v 1.7 2008/08/12 19:23:50 dyoung Exp $
 */
public class GenericUploadServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(GenericUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/GenericUploadServlet.java,v 1.7 2008/08/12 19:23:50 dyoung Exp $";
	
	private static PropertyManager properties = new PropertyManager("/properties/genericUpload.properties");
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {
//		logger.info("Entering servlet - action: " + action);
		
		SimpleDateFormat timestampDf = new SimpleDateFormat("yyyyMMdd-HHmm");
		
		String jspPage = "genericUpload.jsp";
		
		String configFileExt = ".xml";
		String configFileDefaultDir = "etc/";
		
		Map<String, String> pageMap = new HashMap<String, String>();

		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = null;
		
		try {

			String basedir = properties.getProperty("basedir");
			if (basedir == null)
				throw new Exception("BASEDIR not set in properties file");
				
			if (!basedir.endsWith("/"))
				basedir = basedir.concat("/");
			
			winPrincipal = (WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
					
			//////////////////////////////////////////////////////////////////////////////////////
			// URL validation
			//////////////////////////////////////////////////////////////////////////////////////
			Map<String, Map<String, Object>> uploadMap = new HashMap<String, Map<String, Object>>();
					
			pageMap.put("appl", "".equals(request.getParameter("appl")) ? (String)request.getAttribute("appl") : request.getParameter("appl"));
			if (ObjectUtil.isEmpty(pageMap.get("appl")))
				pageMap.put("fatalMsg", "ERROR:  Missing required parameter (APPL) required.<br/>");

			pageMap.put("config", "".equals(request.getParameter("config")) ? (String)request.getAttribute("config") : request.getParameter("config"));
			if (ObjectUtil.isEmpty(pageMap.get("config"))) {
				pageMap.put("fatalMsg", pageMap.get("fatalMsg").toString().concat("ERROR:  Missing required parameter (CONFIG) required.<br/>"));
			} else {
				// test to make sure we can find the config file that was handed to us
				File uploadFile = new File(basedir + pageMap.get("appl") + "/" + configFileDefaultDir + pageMap.get("config") + configFileExt);
				if (!uploadFile.exists()) {
					pageMap.put("fatalMsg", pageMap.get("fatalMsg").toString().concat("ERROR:  The supplied config file doesn't exist (" + uploadFile.getAbsolutePath() + ").<br/>"));
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
			List<String> uploadKeys = new ArrayList<String>();
			for (int i = 0; i < configMap.keySet().toArray().length; i++) {
				String uploadKey = configMap.keySet().toArray()[i].toString();
				uploadKeys.add(uploadKey);
			}
			Collections.sort(uploadKeys);
			request.setAttribute("uploadKeys", uploadKeys);

			
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
				List<String> authUserList = (List<String>) keyMap.get("authorizedUserList");
				if (!authUserList.contains(winPrincipal.getName().toLowerCase())) {
					logger.info("User (" + winPrincipal.getName() + ") is not authorized to upload files to (" + pageMap.get("key") + ")");
					request.setAttribute("errMessage", "Sorry!  You don't have access to upload files for " + pageMap.get("key") + ".");
					request.setAttribute("unauthorized", "true");
					
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
					        
					        String destDir = (String) keyMap.get("destDir");
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
						        	appendToRunInfo(winPrincipal, keyMap, fileName, fileName, null, "upload");
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
			String downloadPath = "".equals(request.getParameter("path")) ? (String)request.getAttribute("path") : request.getParameter("path");
			String downloadSrc = "".equals(request.getParameter("src")) ? (String)request.getAttribute("src") : request.getParameter("src");
			
			if ("1".equals(downloadFlag)) {
				
				if (ObjectUtil.isEmpty(downloadFile)) {
					request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
				} else {
					if (ObjectUtil.isEmpty(downloadPath)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
					} else {
						Map keyMap = configMap.get(pageMap.get("key"));
						
						if (!ObjectUtil.isEmpty(keyMap.get("runInfoDir"))) {
							// need to figure out which run info file our download belongs to
							File[] runInfoFiles = (File[]) (new File(keyMap.get("runInfoDir").toString()).listFiles());
							String runInfoFile = "";
							for (int i = 0; i < runInfoFiles.length; i++) {
								File rif = runInfoFiles[i];
								logger.info("rif: " + rif.getName());
								if (downloadFile.startsWith(rif.getName().replace(".runInfo", ""))) {
									runInfoFile = rif.getName().replace(".runInfo", "");
								}
							}					
							logger.info("runInfoFile: " + runInfoFile);
							
							if (!"".equals(runInfoFile)) {
								// skip logging run info file downloads
								if (!downloadFile.endsWith(".runInfo")) {
									appendToRunInfo(winPrincipal, keyMap, runInfoFile, downloadFile, downloadSrc, "download");
								}
							}
						}
							
						request.setAttribute("file", downloadFile);
						request.setAttribute("path", downloadPath);
						jspPage = "download";
					}
				}
			}
			

			//////////////////////////////////////////////////////////////////////////////////////
			// MOVE functionality
			//////////////////////////////////////////////////////////////////////////////////////
			String moveCode = "".equals(request.getParameter("moveCode")) ? (String)request.getAttribute("moveCode") : request.getParameter("moveCode");
			String eAFile = "".equals(request.getParameter("file")) ? (String)request.getAttribute("file") : request.getParameter("file");
			String eAPath = "".equals(request.getParameter("path")) ? (String)request.getAttribute("path") : request.getParameter("path");
			
			if (!ObjectUtil.isEmpty(moveCode)) {
				if (ObjectUtil.isEmpty(eAFile)) {
					request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
				} else {
					if (ObjectUtil.isEmpty(eAPath)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
					} else {
						Map keyMap = configMap.get(pageMap.get("key"));
						
						if (!eAPath.endsWith("/"))
							eAPath = eAPath.concat("/");
						
						// need to figure out which run info file our download belongs to
						File[] runInfoFiles = (File[]) (new File(keyMap.get("runInfoDir").toString()).listFiles());
						String runInfoFile = "";
						for (int i = 0; i < runInfoFiles.length; i++) {
							File rif = runInfoFiles[i];
							logger.info("rif: " + rif.getName());
							if (downloadFile.startsWith(rif.getName().replace(".runInfo", ""))) {
								runInfoFile = rif.getName().replace(".runInfo", "");
							}
						}
						logger.info("runInfoFile: " + runInfoFile);
	
						// log and perform the move
						logger.info("moveCode: " + moveCode);
						if ("errorArchive".equals(moveCode)) {
							if (!ObjectUtil.isEmpty(runInfoFile))
								appendToRunInfo(winPrincipal, keyMap, runInfoFile, eAFile, null, "archive_error");
		
							FileUtil.moveFile(eAPath + eAFile, keyMap.get("errorArchiveDir").toString() + eAFile);
						}
						
						if ("resubmit".equals(moveCode)) {
							if (!ObjectUtil.isEmpty(runInfoFile))
								appendToRunInfo(winPrincipal, keyMap, runInfoFile, eAFile, null, "resubmit");
		
							FileUtil.moveFile(eAPath + eAFile, keyMap.get("destDir").toString() + eAFile);
						}
					}
				}
			}
			
			
			
			//////////////////////////////////////////////////////////////////////////////////////
			// CURRENT KEY logic
			//////////////////////////////////////////////////////////////////////////////////////
			// always perform a file list retrieval if we're in the context of an uploader
			if (!ObjectUtil.isEmpty(pageMap.get("key"))) {
				Map keyMap = configMap.get(pageMap.get("key"));
				
				if (!ObjectUtil.isEmpty(keyMap.get("destDir"))) {
					request.setAttribute("files_pending", getFileList("destDir", keyMap, "a"));
				}

				if (!ObjectUtil.isEmpty(keyMap.get("workingDir"))) {
					request.setAttribute("files_working", getFileList("workingDir", keyMap, "a"));
				}

				if (!ObjectUtil.isEmpty(keyMap.get("errorDir"))) {
					request.setAttribute("files_error", getFileList("errorDir", keyMap, "a"));
				}
				
				if (!ObjectUtil.isEmpty(keyMap.get("errorArchiveDir"))) {
					request.setAttribute("files_errorArchive", getFileList("errorArchiveDir", keyMap, "a"));
				}
				
				if (!"".equals(keyMap.get("archiveDir")) && keyMap.get("archiveDir") != null) {
					request.setAttribute("files_archive", getFileList("archiveDir", keyMap, "a"));
				}

				if (!ObjectUtil.isEmpty(keyMap.get("runInfoDir"))) {
					request.setAttribute("files_runInfo", getFileList("runInfoDir", keyMap, "d"));
				}
				
				if (StringUtil.hasValue(keyMap.get("name").toString()))
					request.setAttribute("uploadKeyDisplay", " - " + keyMap.get("name").toString());
				
				// one of the last things we'll do is perform an authorization check
				// if we're not authorized, we'll set a flag so that and that will help control
				// the display
				// (there's also a second check during the upload of the file just to make
				// sure nothing slips through)
				List authUserList = (List) keyMap.get("authorizedUserList");
				if (!authUserList.contains(winPrincipal.getName().toLowerCase())) {
					logger.info("current user (" + winPrincipal.getName() + ") is not authorized to upload files to (" + request.getSession().getAttribute("uploadKey") + ")");
					request.setAttribute("errMessage", "Sorry!  You don't have access to upload files for " + pageMap.get("key") + ".");
					request.setAttribute("unauthorized", "true");
				}
				
				request.setAttribute("allowUpload", keyMap.get("allowUpload"));
			}
			////// end of current key logic
			

		} catch (Exception e) {
			logger.error("", e);
		}
			
		// put some things into the request
		request.setAttribute("appl", pageMap.get("appl"));
		request.setAttribute("config", pageMap.get("config"));
		request.setAttribute("key", pageMap.get("key"));

		// always put back in the logged in credentials
		request.setAttribute("isLoggedIn", winPrincipal);
		
		// forward to the page
		forward(jspPage, request, response);		
	}
	
	private List<Map> getFileList(String mapKey, Map map, String sortOrder) {
		File directory = new File((String)map.get(mapKey));
		
		Integer fileLimit = 10;
		if (!ObjectUtil.isEmpty(map.get(mapKey + "Limit"))) {
			fileLimit = Integer.parseInt(map.get(mapKey + "Limit").toString());
		}
		
		File[] files = null;
		List<Map> fileList = new ArrayList<Map>();
		SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy h:mm a");

		if (directory.exists()) {
			files = directory.listFiles();
			
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
					
					fileList.add(fileMap);
				}
			}
		}
		
		// trim to the limit
		while (fileList.size() > fileLimit) {
			fileList.remove(fileList.size()-1);
		}
		return fileList;
	}
	
	private String runInfoPrefix() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.format(new java.util.Date()) + "  ";
	}
	
	private void appendToRunInfo(WindowsPrincipal win, Map map, String runInfoFile, String fileName, String source, String action) {
		
		String message = "";
		if ("upload".equals(action)) {
			message = "File " + fileName + " uploaded to server";
		} else if ("download".equals(action)) {
			message = "File " + fileName + " downloaded from " + source + " directory";
		} else if ("archive_error".equals(action)) {
			message = "Exception file " + fileName + " archived to archvied errors directory";
		} else if ("resubmit".equals(action)) {
			message = "Working file " + fileName + " resubmitted to pending directory";
		} else {
			message = "Unknown action (" + action + ")";
		}
		
		try {
			File runInfo = new File(map.get("runInfoDir") + runInfoFile + ".runInfo");
	    	FileWriter fileWriter = new FileWriter(runInfo, true);
	    	fileWriter.append(runInfoPrefix() + win.getName() + "  " + message + "\n");
	    	fileWriter.flush();
	    	fileWriter.close();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	private List<String> validateConfig(SimpleNode doc, Map<String, String> pageMap, Map<String, Map<String, Object>> configMap) throws Exception {
		
		List<String> messages = new ArrayList<String>();
		
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
						
						if (ObjectUtil.isEmpty(uploadNode.getTextContent("destDir"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir keyword not found");
						} else {
							File dir = new File(uploadNode.getTextContent("destDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							} else if (dir.exists() && !dir.canWrite()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir (" + dir.getAbsolutePath() + ") exists but is not writable");
							}
							if (uploadNode.getTextContent("destDir").endsWith("/")) {
								uploadMap.put("destDir", uploadNode.getTextContent("destDir"));
							} else {
								uploadMap.put("destDir", uploadNode.getTextContent("destDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{destDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{destDir}", "displayLimit").toString());
									uploadMap.put("destDirLimit", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir displayLimit attribute must be an integer");
								}
							}
						}

						if (ObjectUtil.isEmpty(uploadNode.getTextContent("workingDir"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " workingDir keyword not found");
						} else {
							File dir = new File(uploadNode.getTextContent("workingDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " workingDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							}
							if (uploadNode.getTextContent("destDir").endsWith("/")) {
								uploadMap.put("workingDir", uploadNode.getTextContent("workingDir"));
							} else {
								uploadMap.put("workingDir", uploadNode.getTextContent("workingDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{workingDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{workingDir}", "displayLimit").toString());
									uploadMap.put("workingDir", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " workingDir displayLimit attribute must be an integer");
								}
							}
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("errorDir"))) {
							File dir = new File(uploadNode.getTextContent("errorDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " errorDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							}
							if (uploadNode.getTextContent("errorDir").endsWith("/")) {
								uploadMap.put("errorDir", uploadNode.getTextContent("errorDir"));
							} else {
								uploadMap.put("errorDir", uploadNode.getTextContent("errorDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{errorDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{errorDir}", "displayLimit").toString());
									uploadMap.put("errorDirLimit", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " errorDir displayLimit attribute must be an integer");
								}
							}
						}
						
						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("errorArchiveDir"))) {
							File dir = new File(uploadNode.getTextContent("errorArchiveDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " errorArchiveDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							} else if (dir.exists() && !dir.canWrite()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " errorArchiveDir (" + dir.getAbsolutePath() + ") exists but is not writable");
							}
							if (uploadNode.getTextContent("errorArchiveDir").endsWith("/")) {
								uploadMap.put("errorArchiveDir", uploadNode.getTextContent("errorArchiveDir"));
							} else {
								uploadMap.put("errorArchiveDir", uploadNode.getTextContent("errorArchiveDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{errorArchiveDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{errorArchiveDir}", "displayLimit").toString());
									uploadMap.put("errorArchiveDirLimit", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " errorArchiveDir displayLimit attribute must be an integer");
								}
							}
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("archiveDir"))) {
							File dir = new File(uploadNode.getTextContent("archiveDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " archiveDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							}
							if (uploadNode.getTextContent("archiveDir").endsWith("/")) {
								uploadMap.put("archiveDir", uploadNode.getTextContent("archiveDir"));
							} else {
								uploadMap.put("archiveDir", uploadNode.getTextContent("archiveDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{archiveDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{archiveDir}", "displayLimit").toString());
									uploadMap.put("archiveDirLimit", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " archiveDir displayLimit attribute must be an integer");
								}
							}
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("runInfoDir"))) {
							File dir = new File(uploadNode.getTextContent("runInfoDir"));
							if (!dir.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " runInfoDir (" + dir.getAbsolutePath() + ") does not exist or cannot be found");
							} else if (dir.exists() && !dir.canWrite()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " runInfoDir (" + dir.getAbsolutePath() + ") exists but is not writable");
							}
							if (uploadNode.getTextContent("runInfoDir").endsWith("/")) {
								uploadMap.put("runInfoDir", uploadNode.getTextContent("runInfoDir"));
							} else {
								uploadMap.put("runInfoDir", uploadNode.getTextContent("runInfoDir") + "/");
							}
							
							if (!ObjectUtil.isEmpty(uploadNode.getAttribute("{runInfoDir}", "displayLimit"))) {
								try {
									Integer fileLimit = Integer.parseInt(uploadNode.getAttribute("{runInfoDir}", "displayLimit").toString());
									uploadMap.put("runInfoDirLimit", fileLimit);
								} catch (Exception e) {
									messages.add("[Upload #" + uploadCnt + "]" + typeLog + " runInfoDir displayLimit attribute must be an integer");
								}
							}
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

						configMap.put(type, uploadMap);
					}				
				}				
			}
		}
		return messages;
	}
}
