/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.savvis.it.filter.WindowsAuthenticationFilter;
import com.savvis.it.filter.WindowsAuthenticationFilter.WindowsPrincipal;
import com.savvis.it.job.Job;
import com.savvis.it.job.WebJobRunner;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.tools.RunInfoUtil;
import com.savvis.it.tools.web.bean.InputFieldHandler;
import com.savvis.it.util.CommandLineProcess;
import com.savvis.it.util.Context;
import com.savvis.it.util.FileUtil;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.PropertyManager;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.StringUtil;
import com.savvis.it.util.SystemUtil;
import com.savvis.it.util.XmlUtil;

/**
 * This class handles the home page functionality 
 * 
 * @author David R Young
 * @version $Id: GenericUploadServlet.java,v 1.48 2009/02/19 19:24:54 dyoung Exp $
 */
public class GenericUploadServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(GenericUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/GenericUploadServlet.java,v 1.48 2009/02/19 19:24:54 dyoung Exp $";
	
	private static PropertyManager properties = new PropertyManager("/properties/genericUpload.properties");
	private static Map<String, Thread> threadMap = new HashMap<String, Thread>();
	private static Thread threadChecker = null;
	private Context globalContext = new Context();
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param userName the user 
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
//		Map parmMap = request.getParameterMap();
//		for (int i = 0; i < parmMap.keySet().toArray().length; i++) {
//			Object key = parmMap.keySet().toArray()[i];
//			logger.info("key: " + key);
//			logger.info("parmMap.get(" + key + "): " + request.getParameter(key.toString()));
//		}

		String jspPage = "genericUpload.jsp";
		
		String configFileExt = ".xml";
		String configFileDefaultDir = "config/misc/";
		
		final Map<String, Object> pageMap = new HashMap<String, Object>();

		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = null;
		
		String basedir = SystemUtil.getProperty("BASEDIR");

		/*
		 * instead of only checking on a refresh and removing THAT key's thread
		 * if it's not active anymore, we need to actually loop through EVERY
		 * thread in the hash and check to see if we have any dead threads out
		 * there - it's possible some threads could be left in the map if that		 
		 * particular page isn't refreshed for a while
		 */
		if (threadChecker == null) {
			threadChecker = new Thread() {
				public void run() {
					try {
						while(true) {
							for (String key : threadMap.keySet()) {
								Thread t = threadMap.get(key);
								if (!t.isAlive()) {
									logger.info("removing dead thread from thread map for key(" + key + ")"); 
									threadMap.remove(key);
								}
							}
							sleep(30000);
						}
					} catch (Exception e) {
						logger.error("", e);
						throw new RuntimeException(e);
					}
				}
			};
			threadChecker.start();
		}

		try {

			if (basedir == null)
				throw new Exception("BASEDIR not set");
				
			if (basedir.endsWith("/"))
				basedir = basedir.substring(0, basedir.length()-1);
			
			winPrincipal = (WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
			pageMap.put("winPrincipal", winPrincipal);
					
			// load up the global context
			globalContext.add("global", "username", winPrincipal.getName().toLowerCase());
			globalContext.add("global", "url", request.getRequestURL() + "?" + request.getQueryString());
			
			//////////////////////////////////////////////////////////////////////////////////////
			// URL validation
			//////////////////////////////////////////////////////////////////////////////////////
			Map<String, Map<String, Object>> uploadMap = new HashMap<String, Map<String, Object>>();
					
			pageMap.put("appl", "".equals(request.getParameter("appl")) ? (String)request.getAttribute("appl") : request.getParameter("appl"));
			if (ObjectUtil.isEmpty(pageMap.get("appl")))
				pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  Missing required parameter (APPL) required.<br/>");
			SystemUtil.setAPPL(pageMap.get("appl").toString());

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
			
			// if we have an effective user, store it away
			pageMap.remove("effectiveUsername");

			// check any normal forms
			Object newEffectiveUsername = request.getParameter("frmEffectiveUsername");
			if (ObjectUtil.isEmpty(newEffectiveUsername)) {
				newEffectiveUsername = request.getParameter("effectiveUsername");
			}
			logger.info("frm -> newEffectiveUsername: " + newEffectiveUsername);
			
			// check any multipart forms (need to do this because for an upload, the effective user is hidden inside the encoding
			// we actually will also pick off the upload item and the file name here as well, because by picking off
			// the data to check for the effective user name we are unable to pick it off again later on
			FileItemFactory effFactory = new DiskFileItemFactory(0, null);
			ServletFileUpload effUpload = new ServletFileUpload(effFactory);
			boolean effIsMultipart = ServletFileUpload.isMultipartContent(request);
			if (effIsMultipart) {
				List items = effUpload.parseRequest(request);
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
				    if ("frmEffectiveUsername".equals(item.getFieldName())) {
				    	newEffectiveUsername = item.getString();
				    }
				    
				    // capture the file to upload
				    if (!item.isFormField()) {
				    	pageMap.put("uploadItem", item);
				    }
					    
				    // caputre the filename to upload
				    if ("uploadName".equals(item.getFieldName())) {
				    	pageMap.put("uploadName", item.getString());
				    }
				}
			}
			logger.info("frm multipart -> newEffectiveUsername: " + newEffectiveUsername);
			
			// check the url
			if (ObjectUtil.isEmpty(newEffectiveUsername)) {
				newEffectiveUsername = request.getAttribute("effectiveUsername");
			}
			logger.info("url -> newEffectiveUsername: " + newEffectiveUsername);
			
			// finally, if we have an effective user, add it to the map
			if (!ObjectUtil.isEmpty(newEffectiveUsername)) {
				pageMap.put("effectiveUsername", newEffectiveUsername.toString());
				
				// if we switch back to ourselves, remove the effective user
				if (winPrincipal.getName().equals(pageMap.get("effectiveUsername"))) {
					pageMap.remove("effectiveUsername");
				}
				request.setAttribute("effectiveUsername", pageMap.get("effectiveUsername"));
			}
			
			// if we have a fatal message - stop processing and skip to the end
			if (!ObjectUtil.isEmpty(pageMap.get("fatalMsg"))) {
				request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			}
			
			//////////////////////////////////////////////////////////////////////////////////////
			// CONFIG validation
			//////////////////////////////////////////////////////////////////////////////////////
			SimpleNode doc = new SimpleNode(XmlUtil.loadDocumentFromFile(pageMap.get("uploadFile").toString()));
			Map<String, Map<String, Object>> configMap = new HashMap<String, Map<String, Object>>();
			
			List<String> messages = validateConfig(doc, pageMap, configMap, winPrincipal);
			
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

			if (uploads.size() == 0) {
				request.setAttribute("errMessage", "Sorry!  You have not been added to the authorization lists for any file utilities.  " + 
						"Please contact Access Administration and request to be added to the specific authorization list for the action " +
						"you need to perform.");
			}
			
			//////////////////////////////////////////////////////////////////////////////////////
			// PERFORM ACTION functionality
			//////////////////////////////////////////////////////////////////////////////////////
			Thread lkpThread = threadMap.get(pageMap.get("key"));

			// if we notice the override directory for runInfo, that means
			// we need to try to find a thread a different way
			if (!ObjectUtil.isEmpty(configMap.get(pageMap.get("key")).get("runInfo"))) {
				lkpThread = threadMap.get(pageMap.get("key")+winPrincipal.getName());
			}
			
			Boolean processRunning = false;
			if (!ObjectUtil.isEmpty(lkpThread)) {
				if (lkpThread.isAlive()) {
					// already running a thread for this uploader
					request.setAttribute("message", "A process is current running.  No other actions can be started until it is finished.  " + 
							"Please refresh the page until this message no longer appears.");
					processRunning = true;
				}
			}
			request.setAttribute("processRunning", processRunning);
			
			if (!processRunning) {
				if (!ObjectUtil.isEmpty(request.getParameter("action")) && "execute".equals(request.getParameter("action"))) {
					Map keyMap = configMap.get(pageMap.get("key"));
					Context inputsContext = new Context();
	
					// get parms and add into context for command line keyword substitution 
					for (int i = 0; i < request.getParameterMap().keySet().size(); i++) {
						String parm = (String) request.getParameterMap().keySet().toArray()[i];
						inputsContext.add("input", parm, request.getParameter(parm));
					}
					
					// retrieve the commands from the config
					Map actionsMap = (Map) keyMap.get("actions");
					Map actionMap = (Map) actionsMap.get(inputsContext.get("input", "action_name"));
					List<Map<String, Object>> commands = (List<Map<String, Object>>) actionMap.get("cmds");
	
					// execute the command in another thread
					try {
						if ("job".equals(actionMap.get("cmdType"))) {
							
							// only the first one is read here
							logger.info("commands.get(0).get(\"cmdString\"): " + commands.get(0).get("cmdString"));
							String className = globalContext.keywordSubstitute(inputsContext.keywordSubstitute((String)commands.get(0).get("cmdString")));
							logger.info("className: " + className);
	//						logger.info("commands.get(0).get(\"argString\"): " + commands.get(0).get("argString"));
							final String args = globalContext.keywordSubstitute(inputsContext.keywordSubstitute((String)commands.get(0).get("argString")));
							String jar = (String) commands.get(0).get("jar");
							boolean async = "async".equals(commands.get(0).get("mode")); 
							List<String> classpathList = (List<String>) commands.get(0).get("classpath");
							
							Map propertyMap = (Map) commands.get(0).get("properties");
							for (Object key : propertyMap.keySet()) {
								logger.info("Setting System property "+key+" to "+propertyMap.get(key));
								System.setProperty((String) key, (String) propertyMap.get(key));
							}
	
							List<URL> classpath = new ArrayList<URL>();
							for (String entry : classpathList) {
								if(entry.endsWith(".jar")) {
									classpath.add(new File(entry).toURL());
								} else {
									classpath.add(new File(entry+"/classes").toURL());
									classpath.add(new File(entry+"/build/classes").toURL());
								}
							}
							logger.info("classpath = "+(classpath));
							
							URLClassLoader classLoader = new URLClassLoader(
										classpath.toArray(new URL[] {}), this.getClass().getClassLoader()) {
								@Override
								protected Class<?> findClass(String name) throws ClassNotFoundException {
									logger.info("finding class "+name);
									return super.findClass(name);
								}
							};
							Class clp = classLoader.loadClass(className);
	//						Class clp = Class.forName(className, true, classLoader);
							final Job job = (Job) clp.newInstance();
							if(async) {
								Thread t = new Thread() {
									public void run() {
										try {
											SystemUtil.setAPPL(pageMap.get("appl").toString());
											new WebJobRunner(job, args.split(" ")).start();
											SystemUtil.setAPPL(null);
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									}
								};
								if(pageMap.get("key") != null) {
									String key = pageMap.get("key").toString();
									// if we notice the override directory for runInfo, that means
									// we need to tweak the key a bit
									if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
										key += winPrincipal.getName();
									}
									threadMap.put(key, t);
								}
								t.start();
								request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has been executed and is now running in the background.");
							} else {
								new WebJobRunner(job, args.split(" ")).start();
								request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has completed.");
							}
						}
						
						if ("shell".equals(actionMap.get("cmdType"))) {
							// create a string array for the command line processor
							String[] clpCmdArray = new String[commands.size()];
	
							final CommandLineProcess clp = new CommandLineProcess();
							for (int i = 0; i < 1 ; i++) {
								Map<String, Object> cmdMap = commands.get(i);
								boolean async = "async".equals(commands.get(0).get("mode"));
								final String cmd = globalContext.keywordSubstitute(inputsContext.keywordSubstitute(cmdMap.get("cmdString") + " " + cmdMap.get("argString")));
								logger.info("cmd: " + cmd);
								// always wait for process since async will be kicked off in another thread
								clp.setWaitForProcess(true);
								clp.setDir(new File((String) cmdMap.get("startDir")));
								// set the output stream
//								clp.setOutputStream(new FileOutputStream("", true));
								if (!ObjectUtil.isEmpty(cmdMap.get("logFile")))
									clp.setOutputStream(new FileOutputStream(cmdMap.get("logFile").toString(), true));
								Context envContext = new Context();
								envContext.fillWithEnvAndSystemProperties();
								List<String> envList = new ArrayList<String>();
								Map<String, String> envMap = System.getenv();
								for (Object key : envMap.keySet()) 
									envList.add(key+"="+envMap.get(key));
								envList.add("CALLED_BY_USER="+winPrincipal.getName());
								Map propertyMap = (Map) cmdMap.get("properties");
								for (Object key : propertyMap.keySet()) 
									envList.add(key+"="+propertyMap.get(key));
								clp.setEnvp((String[])envList.toArray(new String[] {}));
								
								if(async) {
									Thread t = new Thread() {
										public void run() {
											try {
												SystemUtil.setAPPL(pageMap.get("appl").toString());
												clp.run(cmd);
												SystemUtil.setAPPL(null);
											} catch (Exception e) {
												throw new RuntimeException(e);
											}
										}
									};
									if(pageMap.get("key") != null) {
										String key = pageMap.get("key").toString();
										// if we notice the override directory for runInfo, that means
										// we need to tweak the key a bit
										if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
											key += winPrincipal.getName();
										}
										threadMap.put(key, t);
									}
									t.start();
									request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has been executed and is now running in the background.");
								} else {
									clp.run(cmd);
									request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has completed.");
								}
								
							}
							
							logger.info("output: "+clp.getOutput());
							if(StringUtil.hasValue(clp.getError()))
								logger.info("error: "+clp.getError());
						}
					
	
					} catch (Exception e) {
						logger.error("", e);
						request.setAttribute("errMessage", "There was an error executing the action \"" + actionMap.get("display") + "\".  (" + e + ")");
					}
				}
			}
			
			
			//////////////////////////////////////////////////////////////////////////////////////
			// UPLOAD functionality
			//////////////////////////////////////////////////////////////////////////////////////
			// this section is only performed when there is a file to upload
			// only when the jsp is trying to upload a file OR the first time the JSP is hit
			// for this reason, we lock down any processing to when the incoming form is multipart content
			
			if (pageMap.containsKey("uploadItem")) {

				// verify the authorization (a second check - the first one is upon selecting the upload key)
				Map keyMap = configMap.get(pageMap.get("key"));
				Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");
				List<String> authUserList = (List<String>) keyMap.get("authorizedUserList");
				
				AuthorizationObject authObject = isAuthorized(keyMap, winPrincipal);
				request.setAttribute("authorized", authObject.isAuthorized);
				if (!authObject.isAuthorized) {
					request.setAttribute("errMessage", authObject.getMessage());

				} else {

					if (!ObjectUtil.isEmpty(pageMap.get("uploadItem"))) {
						FileItem uploadItem = (FileItem) pageMap.get("uploadItem");
				        String fullFileName = uploadItem.getName();
				        String fileName = StringUtil.getLastToken(fullFileName, '\\');

				        // retrieve and use the file upload config, otherwise for legacy purposes, 
				        // we have to use "pending" like we used to
				        String destDir = "";
				        Map fileUploadMap = null;
				        if (!ObjectUtil.isEmpty(pageMap.get("uploadName"))) {
				        	fileUploadMap = (Map)((Map)keyMap.get("fileUploads")).get(pageMap.get("uploadName"));
				        	logger.info("fileUploadMap: " + fileUploadMap);
				        	destDir = keyMap.get("path").toString() + "/" + fileUploadMap.get("target");				        	
				        } else {
				        	destDir = (String) directoriesMap.get("pending").get("path");
				        }

			        	if (!ObjectUtil.isEmpty(fileName)) {
					        if (!destDir.endsWith("/"))
					        	destDir = destDir.concat("/");
					        
					        File fileToCreate = new File(destDir + fileName);
					        
					        // check to see if the file already exists
					        if (fileToCreate.exists()) {
					        	request.setAttribute("errMessage", "ERROR!  File (" + fileName + ") already exists and is waiting to be processed!  It was not uploaded again.");
					        	
					        // otherwise, write the file
					        } else {
					        	// if we have a reg ex, check to see if the file name matches it
					        	Boolean okToWrite = true;
				        		logger.info("fileName: " + fileName);
					        	if (fileUploadMap != null && !ObjectUtil.isEmpty(fileUploadMap.get("fileNameRegEx"))) {
					        		if (!fileName.matches(fileUploadMap.get("fileNameRegEx").toString())) {
					        			request.setAttribute("errMessage", "ERROR!  Filename Matching Error (" + fileName + ") doesn't match " + fileUploadMap.get("fileNameRegExText") + ".  The file was not uploaded.");
					        			okToWrite = false;
					        		}
				        	
				        		// for legacy sake, check the old reg ex
					        	} else if (!ObjectUtil.isEmpty(keyMap.get("fileNameRegEx"))) {
					        		if (!fileName.matches(keyMap.get("fileNameRegEx").toString())) {
					        			request.setAttribute("errMessage", "ERROR!  Filename Matching Error (" + fileName + ") doesn't match " + keyMap.get("fileNameRegExText") + ".  The file was not uploaded.");
					        			okToWrite = false;
					        		}

					        	}
					        	
					        	if (okToWrite) {
						        	uploadItem.write(fileToCreate);
						        	
						        	if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
										RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), fileToCreate, winPrincipal.getName(), new java.util.Date(), 
												"File " + fileName + " uploaded");
						        	} else {
										RunInfoUtil.addLog(keyMap.get("path").toString(), fileToCreate, winPrincipal.getName(), new java.util.Date(), 
												"File " + fileName + " uploaded");
						        	}

							        request.setAttribute("message", "The local file (" + fileName + ") has been successfully uploaded.");
					        	}
					        }
				        } else {
				        	request.setAttribute("errMessage", "No file supplied.");
				        }					
			        } else {
			        	request.setAttribute("errMessage", "No file supplied or target directory not configured.");
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
			// ACTION functionality
			//////////////////////////////////////////////////////////////////////////////////////
			String actionType = "".equals(request.getParameter("type")) ? (String)request.getAttribute("type") : request.getParameter("type");
			String actionFile = "".equals(request.getParameter("file")) ? (String)request.getAttribute("file") : request.getParameter("file");
			String actionFilePath = "".equals(request.getParameter("path")) ? (String)request.getAttribute("path") : request.getParameter("path");
			String actionTarget = "".equals(request.getParameter("target")) ? (String)request.getAttribute("target") : request.getParameter("target");
			
			if (!ObjectUtil.isEmpty(actionType)) {
				if ("move".equals(actionType)) {
					if (ObjectUtil.isEmpty(actionFile)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
					} else {
						if (ObjectUtil.isEmpty(actionFilePath)) {
							request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
						} else {
							Map keyMap = configMap.get(pageMap.get("key"));
							Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");
							
							if (!actionFilePath.endsWith("/"))
								actionFilePath = actionFilePath.concat("/");
							
							// verify the target directory is in the map
							if (ObjectUtil.isEmpty(directoriesMap.get(actionTarget))) {
								request.setAttribute("fatalMsg", "ERROR: Invalid value for target directory (" + actionTarget + ".<br/>");
							} else {
								// log and perform the move
								logger.info("keyMap.get(\"path\") + directoriesMap.get(moveTarget).get(\"path\") + moveFile: " + keyMap.get("path") + directoriesMap.get(actionTarget).get("path") + actionFile);
								FileUtil.moveFile(actionFilePath + actionFile, directoriesMap.get(actionTarget).get("path") + actionFile);
								
								if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
									RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(), new java.util.Date(), 
											"File " + actionFile + " moved to \"" + directoriesMap.get(actionTarget).get("description") + "\" directory");
								} else {
									RunInfoUtil.addLog(keyMap.get("path").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(), new java.util.Date(), 
											"File " + actionFile + " moved to \"" + directoriesMap.get(actionTarget).get("description") + "\" directory");
								}
							}
						}
					}
				} else if ("delete".equals(actionType)) {
					if (ObjectUtil.isEmpty(actionFile)) {
						request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (FILE) required.<br/>");
					} else {
						if (ObjectUtil.isEmpty(actionFilePath)) {
							request.setAttribute("fatalMsg", "ERROR:  Missing required parameter (PATH) required.<br/>");
						} else {
							Map keyMap = configMap.get(pageMap.get("key"));
							Map<String, Map<String, String>> directoriesMap = (Map<String, Map<String, String>>) keyMap.get("directories");

							if (!actionFilePath.endsWith("/"))
								actionFilePath = actionFilePath.concat("/");

							File f = new File(actionFilePath + actionFile);
							f.delete();
							
							if (!f.exists()) {
								// log the delete
								if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
									RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(), new java.util.Date(), 
											"File " + actionFile + " deleted from \"" + actionFilePath + "\" directory");
								} else {
									RunInfoUtil.addLog(keyMap.get("path").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(), new java.util.Date(), 
											"File " + actionFile + " deleted from \"" + actionFilePath + "\" directory");
								}
							} else {
								request.setAttribute("errMsg", "ERROR: Unable to delete " + actionFile + " from \"" + actionFilePath + "\" directory.");
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
							directoryMap.put("sizeAlgorithm", ((fileList.size() * 25) + 25) > 255 ? 255 : ((fileList.size() * 25) + 25));
						}
					}
				}
				
				if (StringUtil.hasValue(keyMap.get("name").toString()))
					request.setAttribute("uploadKeyDisplay", " - " + keyMap.get("name").toString());
				
				if (!ObjectUtil.isEmpty(keyMap.get("fileUploads"))) {
					request.setAttribute("hasFileUploads", "1");
					request.setAttribute("fileUploads", keyMap.get("fileUploads"));
				}

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

				// determine if the user is a generic upload admin for this key
				if (!ObjectUtil.isEmpty(keyMap.get("authorizedAdminList"))) {
					AuthorizationObject authAdminObject = isAdmin(keyMap, winPrincipal);
					if (authAdminObject.isAuthorized) {
						request.setAttribute("isAdmin", "1");

						// if an admin list is specified and
						// if the current user is an admin and
						// if the switch flag is on then
						// retrieve the list of available users for this upload
						if ("1".equals(request.getParameter("switch"))) {
							
							// for now, we'll use the authorizedUserList in the keyMap
							// this might later be changed to pull from other authorization sources
							request.setAttribute("effectiveUsers", keyMap.get("authorizedUserList"));
						}
					}
					
				}
				
				// pass along our switch variable
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
		request.setAttribute("switch", request.getParameter("switch"));

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
//				logger.info("current user (" + winPrincipal.getName() + ") is not authorized to use the file utility for (" + keyMap.get("type") + ")");
				authObject.setMessage("Sorry!  You don't have access to use the file utility for " + keyMap.get("type") + ".");
				authObject.setIsAuthorized(false);
			} 
			return authObject;
		}

		
		return authObject;
	}

	private AuthorizationObject isAdmin(Map keyMap, WindowsPrincipal winPrincipal) throws Exception {
		AuthorizationObject authObject = new AuthorizationObject();
		
		// always check for the authList for legacy compatability, then check for the newer "auth" sections for the different kinds
		List authUserList = (List) keyMap.get("authorizedAdminList");
		if (!ObjectUtil.isEmpty(keyMap.get("authorizedAdminList"))) {
			if (authUserList.contains(winPrincipal.getName().toLowerCase())) {
				authObject.setIsAuthorized(true);
			} else {
				authObject.setIsAuthorized(false);
			} 
			return authObject;
		}

		
		return authObject;
	}
	/**
	 * validate the incoming config
	 * 
	 * whenever we have a directory name, we need run it through the global context
	 * to substitute out any potential usernames
	 * 
	 * @param doc
	 * @param pageMap
	 * @param configMap
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private List<String> validateConfig(SimpleNode doc, Map<String, Object> pageMap, Map<String, Map<String, Object>> configMap, WindowsPrincipal user) throws Exception {
		
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
						
						// now that we have the type - if we're in "key" mode and the user has selected
						// a specific upload page, don't validate the other ones, we don't need them
						// and we could possibly fail for an unrelated directory error
						if (!ObjectUtil.isEmpty(pageMap.get("key"))) {
							if (!type.equals(pageMap.get("key"))) {
								continue;
							}
						}

						// we want to process this section up at the top of this validation method
						// because things below it might depend if they aren't an "admin"
						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("{authorizedAdmins}"))) {
							File file = new File(uploadNode.getTextContent("{authorizedAdmins}"));
							if (!file.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedAdmins file (" + file.getAbsolutePath() + ") does not exist or cannot be found");
							} else {
								String s = FileUtil.loadFile(file.getAbsolutePath());
								s = s.toLowerCase();
								uploadMap.put("authorizedAdminList", StringUtil.toList(s, "\n "));

								// let's go ahead and see if the user is an admin, since things below might
								// depend on it
								AuthorizationObject authAdminObject = isAdmin(uploadMap, (WindowsPrincipal)pageMap.get("winPrincipal"));
								if (!authAdminObject.isAuthorized) {
									pageMap.remove("effectiveUsername");
								}

							}
						} else {
							// without an admin list, we have no admins - so we therefore can have no "effectiveusers"
							// kill it here in case someone snuck it in on the url
							pageMap.remove("effectiveUsername");
						}


						if (!ObjectUtil.isEmpty(uploadNode.getAttribute("allowUpload"))) {
							
							if (!ObjectUtil.isEmpty(pageMap.get("key")))
								logger.warn("WARNING: attribute allowUpload is deprecated, use fileUploads xml section instead");
							
							if (ObjectUtil.isEmpty(uploadNode.getAttribute("allowUpload"))) {
								// default to yes
								uploadMap.put("allowUpload", "1");
							} else {
								uploadMap.put("allowUpload", uploadNode.getAttribute("allowUpload"));
							}
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
							String path = uploadNode.getTextContent("path");
							if (path.endsWith("/")) {
								path = path.substring(0, path.length()-1);
							}
							
							// if we are acting as another user, also need to substitute out here
							if (!ObjectUtil.isEmpty(pageMap.get("effectiveUsername"))) {
								path = path.replace(globalContext.get("global", "username"), pageMap.get("effectiveUsername").toString());
							}
							uploadMap.put("path", globalContext.keywordSubstitute(path));
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("runInfo"))) {
							String runInfo = uploadNode.getTextContent("runInfo");
							
							runInfo = globalContext.keywordSubstitute(runInfo);
							
							// if we are acting as another user, also need to substitute out here
							if (!ObjectUtil.isEmpty(pageMap.get("effectiveUsername"))) {
								runInfo = runInfo.replace(globalContext.get("global", "username"), pageMap.get("effectiveUsername").toString());
							}
							uploadMap.put("runInfo", runInfo);
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
										directoryKey = globalContext.keywordSubstitute(directoryNode.getAttribute("key"));
										
										if (!ObjectUtil.isEmpty(pageMap.get("effectiveUsername"))) {
											directoryKey = directoryKey.replace(globalContext.get("global", "username"), pageMap.get("effectiveUsername").toString());
										}
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
									String path = uploadMap.get("path") + "/" + directoryKey;
									
									File dir = new File(path);
									if (!dir.exists()) {
										if (!dir.mkdir()) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j + " (" + dir.getAbsolutePath() + ") unable to create directory");
										}
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

											// required
											actionMap.put("level", actionNode.getAttribute("level"));
											actionMap.put("type", actionNode.getAttribute("type"));
											actionMap.put("description", actionNode.getAttribute("description"));

											// could be optional
											if (!ObjectUtil.isEmpty(actionNode.getAttribute("fileAge")))
												actionMap.put("fileAge", actionNode.getAttribute("fileAge"));
											if (!ObjectUtil.isEmpty(actionNode.getAttribute("target")))
												actionMap.put("target", actionNode.getAttribute("target"));
											if (!ObjectUtil.isEmpty(actionNode.getAttribute("confirm")))
												actionMap.put("confirm", actionNode.getAttribute("confirm"));
											
											actionsMap.put(ObjectUtil.toString(l), actionMap);
										}
										directoryMap.put("actions", actionsMap);
									}
									
									directoriesMap.put(directoryKey, directoryMap);
								}
							} catch (Exception e) {
								logger.error("", e);
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
										
											if (!ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}").getAttribute("mode"))) {
												String cmdMode = actionNode.getSimpleNode("{cmds}").getAttribute("mode");
												if (!"sync".equals(cmdMode.toLowerCase()) && !"async".equals(cmdMode.toLowerCase())) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog + " cmd attribute mode must be either 'sync' or 'async'");
												} else {
													cmdMap.put("mode", cmdMode.toLowerCase());
												}
											} else {
												cmdMap.put("mode", "sync");
											}
				
											for (int k = 0; k < actionNode.getSimpleNode("{cmds}").getChildNodes("cmd").getLength(); k++) {
												SimpleNode cmdNode = new SimpleNode(actionNode.getSimpleNode("{cmds}").getChildNodes("cmd").item(k));
												cmdMap.put("cmdString", cmdNode.getTextContent("{cmdString}"));
												cmdMap.put("argString", cmdNode.getTextContent("{argString}"));
												cmdMap.put("classpath", cmdNode.getTextContent("{jar}"));
												cmdMap.put("startDir", cmdNode.getTextContent("{startDir}"));
												cmdMap.put("logFile", cmdNode.getTextContent("{logFile}"));
												
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
												
												// get classpath values if present
												List<String> classpathList = new ArrayList<String>();
												if (!ObjectUtil.isEmpty(cmdNode.getSimpleNode("{classpath}"))) {
													for (int l = 0; l < cmdNode.getSimpleNode("{classpath}").getChildNodes("entry").getLength(); l++) {
														SimpleNode classpathNode = new SimpleNode(cmdNode.getSimpleNode("{classpath}").getChildNodes("entry").item(l));
														classpathList.add(classpathNode.getTextContent());
													}
												}
												cmdMap.put("classpath", classpathList);
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
								logger.error("", e);
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " error in actions section (" + ObjectUtil.toString(e) + ")");
							}
						}
						
						// look for file upload sections
						if (!ObjectUtil.isEmpty(uploadNode.getSimpleNode("{fileUploads}"))) {
							NodeList actions = uploadNode.getSimpleNode("{fileUploads}").getChildNodes("fileUpload");
							Map<String, Object> fileUploadsMap = new LinkedHashMap<String, Object>();
							
							try {
								for (int j = 0; j < actions.getLength(); j++) {
									SimpleNode fileUploadNode = new SimpleNode(actions.item(j));
									Map<String, Object> fileUploadMap = new LinkedHashMap<String, Object>();
									
									// get file upload variables
									if (ObjectUtil.isEmpty(fileUploadNode.getAttribute("name"))) {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload keyword must have a name attribute");
									} else {
										fileUploadMap.put("name", fileUploadNode.getAttribute("name").replace(" ", "_"));
									}
									
									if (!ObjectUtil.isEmpty(fileUploadNode.getTextContent("{target}"))) {
										String target = fileUploadNode.getTextContent("{target}");
										logger.info("target: " + target);
										target = globalContext.keywordSubstitute(target);
										
										if (!ObjectUtil.isEmpty(pageMap.get("effectiveUsername"))) {
											target = target.replace(globalContext.get("global", "username"), pageMap.get("effectiveUsername").toString());
										}
										
										logger.info("target: " + target);
										fileUploadMap.put("target", target);
									} else {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload [" + fileUploadNode.getAttribute("name") + "] must have target element");
									}

									fileUploadMap.put("display", fileUploadNode.getTextContent("{display}"));
									fileUploadMap.put("buttonLabel", fileUploadNode.getTextContent("{buttonLabel}"));
									fileUploadMap.put("description", fileUploadNode.getTextContent("{description}"));
									fileUploadMap.put("fileNameRegEx", fileUploadNode.getTextContent("{fileNameRegEx}"));
									fileUploadMap.put("fileNameRegExText", fileUploadNode.getTextContent("{fileNameRegExText}"));
									
									fileUploadsMap.put(fileUploadMap.get("name").toString(), (Object) fileUploadMap);
								}
								
								uploadMap.put("fileUploads", fileUploadsMap);
								
							} catch (Exception e) {
								logger.error("", e);
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " error in fileUploads section (" + ObjectUtil.toString(e) + ")");
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
