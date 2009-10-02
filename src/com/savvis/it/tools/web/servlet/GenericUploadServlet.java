/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.savvis.it.db.DBUtil;
import com.savvis.it.filter.WindowsAuthenticationFilter;
import com.savvis.it.filter.WindowsAuthenticationFilter.WindowsPrincipal;
import com.savvis.it.job.Job;
import com.savvis.it.job.WebJobRunner;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.tools.RunInfoUtil;
import com.savvis.it.util.CommandLineProcess;
import com.savvis.it.util.Context;
import com.savvis.it.util.FileUtil;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.PropertyManager;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.StringUtil;
import com.savvis.it.util.SystemUtil;
import com.savvis.it.util.XmlUtil;
import com.savvis.it.validation.Input;
import com.savvis.it.validation.InputValidator;
import com.savvis.it.web.util.InputFieldHandler;

/**
 * This class handles the home page functionality
 * 
 * @author David R Young
 * @version $Id: GenericUploadServlet.java,v 1.63 2009/10/02 19:32:39 dmoorhem Exp $
 */
public class GenericUploadServlet extends SavvisServlet {
	private static Logger logger = Logger.getLogger(GenericUploadServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/GenericUploadServlet.java,v 1.63 2009/10/02 19:32:39 dmoorhem Exp $";

	private static PropertyManager properties = new PropertyManager("/properties/genericUpload.properties");
	private static Map<String, Thread> threadMap = new HashMap<String, Thread>();
	private static Thread threadChecker = null;
	private static SimpleDateFormat excelToCSVdateFormat = new SimpleDateFormat("MM/dd/yyyy");

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 * 
	 * @param action
	 *            the action to perform (typically null when the servlet is
	 *            first called)
	 * @param userName
	 *            the user
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 */
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response) throws Exception {

		Context globalContext = new Context();

		// Map parmMap = request.getParameterMap();
		// for (int i = 0; i < parmMap.keySet().toArray().length; i++) {
		// Object key = parmMap.keySet().toArray()[i];
		// logger.info("key: " + key);
		// logger.info("parmMap.get(" + key + "): " +
		// request.getParameter(key.toString()));
		// }

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
						while (true) {
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
				basedir = basedir.substring(0, basedir.length() - 1);

			winPrincipal = (WindowsAuthenticationFilter.WindowsPrincipal) request.getSession().getAttribute(
					WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
			pageMap.put("winPrincipal", winPrincipal);

			// load up the global context
			globalContext.add("global", "username", winPrincipal.getName().toLowerCase());
			globalContext.add("global", "url", request.getRequestURL() + "?" + request.getQueryString());

			// ////////////////////////////////////////////////////////////////////////////////////
			// URL validation
			// ////////////////////////////////////////////////////////////////////////////////////
			Map<String, Map<String, Object>> uploadMap = new HashMap<String, Map<String, Object>>();

			pageMap.put("appl", "".equals(request.getParameter("appl")) ? (String) request.getAttribute("appl") : request.getParameter("appl"));
			if (ObjectUtil.isEmpty(pageMap.get("appl")))
				pageMap.put("fatalMsg", getFatalMsg(pageMap) + "ERROR:  Missing required parameter (APPL) required.<br/>");
			SystemUtil.setAPPL(pageMap.get("appl").toString());

			pageMap.put("config", "".equals(request.getParameter("config")) ? (String) request.getAttribute("config") : request
					.getParameter("config"));
			if (ObjectUtil.isEmpty(pageMap.get("config"))) {
				pageMap.put("fatalMsg", getFatalMsg(pageMap) + "ERROR:  Missing required parameter (CONFIG) required.<br/>");
			} else {
				// test to make sure we can find the config file that was handed
				// to us
				File uploadFile = new File(basedir + "/" + pageMap.get("appl") + "/" + configFileDefaultDir + pageMap.get("config") + configFileExt);
				if (!uploadFile.exists()) {
					pageMap.put("fatalMsg", getFatalMsg(pageMap) + "ERROR:  The supplied config file doesn't exist (" + uploadFile.getAbsolutePath()
							+ ").<br/>");
				} else {
					pageMap.put("uploadFile", uploadFile.getAbsolutePath());
				}
			}

			pageMap.put("key", request.getParameter("key"));

			pageMap.remove("contextValue");

			// check any normal forms
			Object newContextValue = request.getParameter("frmContext");
			if (ObjectUtil.isEmpty(newContextValue)) {
				newContextValue = request.getParameter("contextValue");
			}

			// check any multipart forms (need to do this because for an upload,
			// the contex value is hidden inside the encoding
			// we actually will also pick off the upload item and the file name
			// here as well, because by picking off
			// the data to check for the context value name we are unable to
			// pick it off again later on
			FileItemFactory effFactory = new DiskFileItemFactory(0, null);
			ServletFileUpload effUpload = new ServletFileUpload(effFactory);
			boolean effIsMultipart = ServletFileUpload.isMultipartContent(request);
			if (effIsMultipart) {
				List items = effUpload.parseRequest(request);
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					if ("frmContext".equals(item.getFieldName())) {
						newContextValue = item.getString();
					}

					// capture the file to upload
					if (!item.isFormField()) {
						pageMap.put("uploadItem", item);
					}

					// capture the filename to upload
					if ("uploadName".equals(item.getFieldName())) {
						pageMap.put("uploadName", item.getString());
					}

					// capture the filename alias
					if ("alias".equals(item.getFieldName())) {
						pageMap.put("aliasName", item.getString());
					}

				}
			}

			// check the url
			if (ObjectUtil.isEmpty(newContextValue)) {
				newContextValue = request.getAttribute("contextValue");
			}
			// finally, if we have a context value, add it to the map
			if (!ObjectUtil.isEmpty(newContextValue)) {
				pageMap.put("contextValue", newContextValue.toString());
				globalContext.add("global", "context", newContextValue);

				// if we switch back to ourselves, remove the context value
				if (winPrincipal.getName().equals(pageMap.get("contextValue"))) {
					pageMap.remove("contextValue");
				}
				request.setAttribute("contextValue", pageMap.get("contextValue"));
			}

			// if we have a fatal message - stop processing and skip to the end
			if (!ObjectUtil.isEmpty(pageMap.get("fatalMsg"))) {
				request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			}

			// ////////////////////////////////////////////////////////////////////////////////////
			// CONFIG validation
			// ////////////////////////////////////////////////////////////////////////////////////
			SimpleNode doc = new SimpleNode(XmlUtil.loadDocumentFromFile(pageMap.get("uploadFile").toString()));
			Map<String, Map<String, Object>> configMap = new HashMap<String, Map<String, Object>>();

			List<String> messages = validateConfig(doc, pageMap, configMap, winPrincipal, globalContext);

			// account for a default context in the config
			if (!ObjectUtil.isEmpty(pageMap.get("contextDefaultValue")) && ObjectUtil.isEmpty(pageMap.get("contextValue"))) {
				pageMap.put("contextValue", pageMap.get("contextDefaultValue"));
				request.setAttribute("contextValue", pageMap.get("contextDefaultValue"));
			}
			
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

			// ////////////////////////////////////////////////////////////////////////////////////
			// UPLOAD key list
			// ////////////////////////////////////////////////////////////////////////////////////
			// to make things easier for the display, let's create a list of
			// uploader keys and sort it
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
				request.setAttribute("errMessage", "Sorry!  You have not been added to the authorization lists for any file utilities.  "
						+ "Please contact Access Administration and request to be added to the specific authorization list for the action "
						+ "you need to perform.");
			}

			// ////////////////////////////////////////////////////////////////////////////////////
			// PERFORM ACTION functionality
			// ////////////////////////////////////////////////////////////////////////////////////
			Thread lkpThread = threadMap.get(pageMap.get("key"));

			// if we notice the override directory for runInfo, that means
			// we need to try to find a thread a different way
			if (!ObjectUtil.isEmpty(configMap.get(pageMap.get("key")))) {
				if ((configMap.get(pageMap.get("key"))).containsKey("runInfo")) {
					lkpThread = threadMap.get(pageMap.get("key") + winPrincipal.getName());
				}
			}

			Boolean processRunning = false;
			if (!ObjectUtil.isEmpty(lkpThread)) {
				if (lkpThread.isAlive()) {
					// already running a thread for this uploader
					request.setAttribute("message", "A process is current running.  No other actions can be started until it is finished.  "
							+ "Please refresh the page until this message no longer appears.");
					processRunning = true;
				}
			}
			request.setAttribute("processRunning", processRunning);

			if (!processRunning) {
				if (!ObjectUtil.isEmpty(request.getParameter("action")) && "execute".equals(request.getParameter("action"))) {
					Map keyMap = configMap.get(pageMap.get("key"));
					Context inputsContext = new Context();

					// get parms and add into context for command line keyword
					// substitution
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
							String className = globalContext.keywordSubstitute(inputsContext.keywordSubstitute((String) commands.get(0).get(
									"cmdString")));
							logger.info("className: " + className);
							final String args = globalContext.keywordSubstitute(inputsContext.keywordSubstitute((String) commands.get(0).get(
									"argString")));
							String jar = (String) commands.get(0).get("jar");
							boolean async = "async".equals(commands.get(0).get("mode"));
							List<String> classpathList = (List<String>) commands.get(0).get("classpath");

							Map propertyMap = (Map) commands.get(0).get("properties");
							for (Object key : propertyMap.keySet()) {
								logger.info("Setting System property " + key + " to " + propertyMap.get(key));
								System.setProperty((String) key, (String) propertyMap.get(key));
							}

							List<URL> classpath = new ArrayList<URL>();
							for (String entry : classpathList) {
								if (entry.endsWith(".jar")) {
									classpath.add(new File(entry).toURL());
								} else {
									classpath.add(new File(entry + "/classes").toURL());
									classpath.add(new File(entry + "/build/classes").toURL());
								}
							}
							logger.info("classpath = " + (classpath));

							URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[] {}), this.getClass().getClassLoader()) {
								@Override
								protected Class<?> findClass(String name) throws ClassNotFoundException {
									logger.info("finding class " + name);
									return super.findClass(name);
								}
							};
							Class clp = classLoader.loadClass(className);
							// Class clp = Class.forName(className, true,
							// classLoader);
							final Job job = (Job) clp.newInstance();
							if (async) {
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
								if (pageMap.get("key") != null) {
									String key = pageMap.get("key").toString();
									// if we notice the override directory for
									// runInfo, that means
									// we need to tweak the key a bit
									if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
										key += winPrincipal.getName();
									}
									threadMap.put(key, t);
								}
								t.start();
								request.setAttribute("message", "The action \"" + actionMap.get("display")
										+ "\" has been executed and is now running in the background.");
							} else {
								new WebJobRunner(job, args.split(" ")).start();
								request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has completed.");
							}
						}

						if ("shell".equals(actionMap.get("cmdType"))) {
							// create a string array for the command line
							// processor
							String[] clpCmdArray = new String[commands.size()];

							final CommandLineProcess clp = new CommandLineProcess();
							for (int i = 0; i < 1; i++) {
								Map<String, Object> cmdMap = commands.get(i);
								boolean async = "async".equals(commands.get(0).get("mode"));
								final String cmd = globalContext.keywordSubstitute(inputsContext.keywordSubstitute(cmdMap.get("cmdString") + " "
										+ cmdMap.get("argString")));
								logger.info("cmd: " + cmd);
								// always wait for process since async will be
								// kicked off in another thread
								clp.setWaitForProcess(true);
								clp.setDir(new File((String) cmdMap.get("startDir")));
								if (!ObjectUtil.isEmpty(cmdMap.get("logFile")))
									clp.setOutputStream(new FileOutputStream(cmdMap.get("logFile").toString(), true));
								Context envContext = new Context();
								envContext.fillWithEnvAndSystemProperties();
								List<String> envList = new ArrayList<String>();
								Map<String, String> envMap = System.getenv();
								for (Object key : envMap.keySet())
									envList.add(key + "=" + envMap.get(key));
								envList.add("CALLED_BY_USER=" + winPrincipal.getName());
								Map propertyMap = (Map) cmdMap.get("properties");
								for (Object key : propertyMap.keySet())
									envList.add(key + "=" + propertyMap.get(key));
								clp.setEnvp((String[]) envList.toArray(new String[] {}));

								final String[] inputs = (String[]) new ArrayList((List) cmdMap.get("inputs")).toArray(new String[] {});
								for (int x = 0; x < inputs.length; x++) {
									inputs[x] = globalContext.keywordSubstitute(inputsContext.keywordSubstitute(inputs[x]));
								}
								logger.info("inputs = " + (ObjectUtil.toString(inputs)));

								if (async) {
									Thread t = new Thread() {
										public void run() {
											try {
												SystemUtil.setAPPL(pageMap.get("appl").toString());
												clp.run(cmd, inputs);
												SystemUtil.setAPPL(null);
											} catch (Exception e) {
												throw new RuntimeException(e);
											}
										}
									};
									if (pageMap.get("key") != null) {
										String key = pageMap.get("key").toString();
										// if we notice the override directory
										// for runInfo, that means
										// we need to tweak the key a bit
										if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
											key += winPrincipal.getName();
										}
										threadMap.put(key, t);
									}
									t.start();
									request.setAttribute("message", "The action \"" + actionMap.get("display")
											+ "\" has been executed and is now running in the background.");
								} else {
									clp.run(cmd, inputs);
									request.setAttribute("message", "The action \"" + actionMap.get("display") + "\" has completed.");
								}

							}

							logger.info("output: " + clp.getOutput());
							if (StringUtil.hasValue(clp.getError()))
								logger.info("error: " + clp.getError());
						}

					} catch (Exception e) {
						logger.error("", e);
						request.setAttribute("errMessage", "There was an error executing the action \"" + actionMap.get("display") + "\".  (" + e
								+ ")");
					}
				}
			}

			// ////////////////////////////////////////////////////////////////////////////////////
			// UPLOAD functionality
			// ////////////////////////////////////////////////////////////////////////////////////
			// this section is only performed when there is a file to upload
			// only when the jsp is trying to upload a file OR the first time
			// the JSP is hit
			// for this reason, we lock down any processing to when the incoming
			// form is multipart content

			if (pageMap.containsKey("uploadItem")) {

				// verify the authorization (a second check - the first one is
				// upon selecting the upload key)
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

						if (!ObjectUtil.isEmpty(pageMap.get("aliasName"))) {
							fileName = (String) pageMap.get("aliasName");
						}

						// retrieve and use the file upload config, otherwise
						// for legacy purposes,
						// we have to use "pending" like we used to
						String destDir = "";
						Map fileUploadMap = null;
						if (!ObjectUtil.isEmpty(pageMap.get("uploadName"))) {
							fileUploadMap = (Map) ((Map) keyMap.get("fileUploads")).get(pageMap.get("uploadName"));
							destDir = keyMap.get("path").toString() + "/" + fileUploadMap.get("target");
						} else {
							destDir = (String) directoriesMap.get("pending").get("path");
						}

						if (!ObjectUtil.isEmpty(fileName)) {
							if (!destDir.endsWith("/"))
								destDir = destDir.concat("/");

							File fileToCreate = new File(destDir + fileName);
							File fileToRead = null;

							// check to see if the file already exists
							if (fileToCreate.exists()) {
								request.setAttribute("errMessage", "ERROR!  File (" + fileName
										+ ") already exists and is waiting to be processed!  It was not uploaded again.");

								// otherwise, write the file
							} else {
								// if we have a reg ex, check to see if the file
								// name matches it
								Boolean okToWrite = true;
								if (fileUploadMap != null && !ObjectUtil.isEmpty(fileUploadMap.get("fileNameRegEx"))) {
									if (!fileName.matches(fileUploadMap.get("fileNameRegEx").toString())) {
										String msg = "ERROR!  Filename Matching Error (" + fileName + ") doesn't match "
												+ fileUploadMap.get("fileNameRegExText") + ".  The file was not uploaded.";
										if (!StringUtil.getLastToken(fullFileName, '\\').equals(fileName)) {
											msg += "  The file was originally named (" + StringUtil.getLastToken(fullFileName, '\\') + ").";
										}
										request.setAttribute("errMessage", msg);
										okToWrite = false;
									}

									// for legacy sake, check the old reg ex
								} else if (!ObjectUtil.isEmpty(keyMap.get("fileNameRegEx"))) {
									if (!fileName.matches(keyMap.get("fileNameRegEx").toString())) {
										String msg = "ERROR!  Filename Matching Error (" + fileName + ") doesn't match "
												+ keyMap.get("fileNameRegExText") + ".  The file was not uploaded.";
										if (!StringUtil.getLastToken(fullFileName, '\\').equals(fileName)) {
											msg += "  The file was originally named (" + StringUtil.getLastToken(fullFileName, '\\') + ").";
										}
										request.setAttribute("errMessage", msg);
										okToWrite = false;
									}

								}

								// as a file check, validate the file if we have been configured to do so
								if (okToWrite && !ObjectUtil.isEmpty(fileUploadMap) && !ObjectUtil.isEmpty(fileUploadMap.get("validations"))) {
									Map validationsMap = (Map) fileUploadMap.get("validations");

									// create a temp file object to write the file to for validation
									String extension = StringUtil.getLastToken(fileName, '.');
									
									fileToRead = File.createTempFile(fileName, "."+extension);
								    
									// write the file to a temp location for validation
									uploadItem.write(fileToRead);
									
									// handle excel files specially
									File tempFile = null;
									logger.info("extension: " + extension);
									if ("xls".equals(extension)) {
										tempFile = createDelimitedFileFromXls(fileToRead, ".csv", validationsMap.get("delimiter").toString(), 0);
									} else {
										tempFile = fileToRead;
									}

									NodeList inputs = (NodeList) validationsMap.get("inputs");
									
									List<String> inputList = new ArrayList<String>();
									for (int j = 0; j < inputs.getLength(); j++) {
										SimpleNode inputNode = new SimpleNode(inputs.item(j));
										inputList.add(inputNode.getAttribute("name"));
									}
									request.setAttribute("inputList", inputList);
									
									BufferedReader fileInput = new BufferedReader(new FileReader(tempFile));
									Integer lineIndex = -1;
									String line = null;
									
									List<LineValidationObject> lineValidations = new ArrayList<LineValidationObject>();
									Boolean passedValdiation = true;
									Map<String, Boolean> cacheKeys = new HashMap<String, Boolean>();
									while ((line = fileInput.readLine()) != null) {
										lineIndex++;
										
										Boolean skipLineValidation = false;
										Context c = new Context();

										char delim = validationsMap.get("delimiter").toString().toCharArray()[0];
										String[] lineValues = StringUtil.split(line, delim, '"');

										LineValidationObject validationObject = new LineValidationObject();
										
										// set true until we have row rule validations
										validationObject.setValid(true);
										
										if (ObjectUtil.isEmpty(line.trim())) {
											validationObject.addMessage("Skipping, line is blank");
											skipLineValidation = true;
										}
										
										if (!ObjectUtil.isEmpty(validationsMap.get("startIndex")) && 
												lineIndex < Integer.parseInt(validationsMap.get("startIndex").toString())) {
											validationObject.addMessage("Skipping, assumed column headers");
											skipLineValidation = true;
										}

										// pad the line in case we were given less than we need
										if (inputs.getLength() > lineValues.length) {
											// loop starting at the end of the lineValues array to 
											// the max number of cols we should have from the inputs list
											// and add on blanks
											for (int i = lineValues.length; i < inputs.getLength(); i++) {
												lineValues = (String[]) ArrayUtils.add(lineValues, "");
											}
										}

										// validate the line
										List<Input> inputObjs = InputValidator.validate(inputs, lineValues);
										
										// parse the return
										for (int i = 0; i < inputObjs.size(); i++) {
											Input inputObj = inputObjs.get(i);
											String cacheKey = "COL"+inputObj.getName();
											c.add("ROW", inputObj.getName(), inputObj.getValue());
											
											if (skipLineValidation) {
												validationObject.addDataColumn(inputObj.getName(), inputObj.getValue(), true, null);
												continue;
											} else {
												if (inputObj.getReturnCode() != 0) {
													okToWrite = false;
													passedValdiation = false;
													String msg = "";
													for (int j = 0; j < inputObj.getMessages().size(); j++) {
														msg += inputObj.getMessages().get(j) + "|";
														validationObject.addMessage(inputObj.getMessages().get(j));
													}
													if (msg.length() > 0)
														msg = msg.substring(0, msg.length()-1);
													
													validationObject.addDataColumn(inputObj.getName(), inputObj.getValue(), false, msg);
													cacheKeys.put(inputObj.getName(), false);
												} else {
													validationObject.addDataColumn(inputObj.getName(), inputObj.getValue(), true, null);
													cacheKeys.put(inputObj.getName(), true);
												}
												
											}
										}
										
										/*
										 * now perform any row-level validations
										 */
										if (!skipLineValidation && !ObjectUtil.isEmpty(validationsMap.get("rowRules"))) {
											
											List<Map<String, Object>> rowRules = (List<Map<String, Object>>) validationsMap.get("rowRules");
											for (Map<String, Object> rule : rowRules) {
												String code = c.keywordSubstitute(""+rule.get("code"));
												String cacheKey = c.keywordSubstitute(""+rule.get("cacheKey"));
												String errorText = c.keywordSubstitute(""+rule.get("errorText"));
												
												// if we've already processed this rule for the cacheKey values, skip it
												// and use the previously validated messages
												logger.info("cacheKeys.get(" + cacheKey + "): " + cacheKeys.get(cacheKey));
												if (!ObjectUtil.isEmpty(cacheKeys.get(cacheKey))) {
													logger.info("pulling from cacheKeys");
													
													if (cacheKeys.get(cacheKey) == false) {
														validationObject.addMessage(errorText);
														validationObject.setValid(false);
													}
													
													continue;
												}
												
												// otherwise, process all supported rules
												if ("js".equals(rule.get("type"))) {
													
													ScriptEngineManager manager = new ScriptEngineManager();
													ScriptEngine engine = manager.getEngineByName("js");
													engine.put("result", 0);
													engine.eval(code);
													String result = engine.get("result").toString();
													if (!"0".equals(result)) {
														validationObject.addMessage(errorText);
														validationObject.setValid(false);
														cacheKeys.put(cacheKey, false);
													} else {
														cacheKeys.put(cacheKey, true);
													}
												} else if ("sql".equals(rule.get("type"))) {
													try {
														List results = DBUtil.executeQuery(rule.get("dbDriver").toString(), code);
														
														if (results != null && !rule.get("rowsFoundGood").equals(results.size())) {
															validationObject.addMessage(errorText);
															cacheKeys.put(cacheKey, false);
														} else {
															cacheKeys.put(cacheKey, true);
														}
													} catch (Exception e) {
														logger.error("Error running sql row rule", e);
														validationObject.addMessage(e.getMessage());
													}
												} else {
													validationObject.addMessage("Unsupported rowRule type (" + rule.get("type") +")");
												}
												
											}
										}
										
										lineValidations.add(validationObject);
									}
									fileInput.close();
									
									request.setAttribute("validationOK", passedValdiation);
									if (!passedValdiation) {
										request.setAttribute("lineValidations", lineValidations);
									}
								}
								
								if (okToWrite) {
									
									if (!ObjectUtil.isEmpty(fileUploadMap) && !ObjectUtil.isEmpty(fileUploadMap.get("validations"))) {
										FileUtil.moveFile(fileToRead, fileToCreate);
									} else {
										uploadItem.write(fileToCreate);
									}

									String msg = "File " + fileName + " uploaded";
									if (!StringUtil.getLastToken(fullFileName, '\\').equals(fileName)) {
										msg += "  (orig. name [" + StringUtil.getLastToken(fullFileName, '\\') + "])";
									}

									if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
										RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), fileToCreate, winPrincipal.getName(),
												new java.util.Date(), msg);
									} else {
										RunInfoUtil.addLog(keyMap.get("path").toString(), fileToCreate, winPrincipal.getName(), new java.util.Date(),
												msg);
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
			// //// end of multipart upload functionality

			// ////////////////////////////////////////////////////////////////////////////////////
			// DOWNLOAD functionality
			// ////////////////////////////////////////////////////////////////////////////////////
			String downloadFlag = "".equals(request.getParameter("download")) ? (String) request.getAttribute("download") : request
					.getParameter("download");
			String downloadFile = "".equals(request.getParameter("file")) ? (String) request.getAttribute("file") : request.getParameter("file");
			String downloadKey = "".equals(request.getParameter("dir")) ? (String) request.getAttribute("dir") : request.getParameter("dir");

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

			// ////////////////////////////////////////////////////////////////////////////////////
			// ACTION functionality
			// ////////////////////////////////////////////////////////////////////////////////////
			String actionType = "".equals(request.getParameter("type")) ? (String) request.getAttribute("type") : request.getParameter("type");
			String actionFile = "".equals(request.getParameter("file")) ? (String) request.getAttribute("file") : request.getParameter("file");
			String actionFilePath = "".equals(request.getParameter("path")) ? (String) request.getAttribute("path") : request.getParameter("path");
			String actionTarget = "".equals(request.getParameter("target")) ? (String) request.getAttribute("target") : request
					.getParameter("target");

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
								logger.info("keyMap.get(\"path\") + directoriesMap.get(moveTarget).get(\"path\") + moveFile: " + keyMap.get("path")
										+ directoriesMap.get(actionTarget).get("path") + actionFile);
								FileUtil.moveFile(actionFilePath + actionFile, directoriesMap.get(actionTarget).get("path") + actionFile);

								if (!ObjectUtil.isEmpty(keyMap.get("runInfo"))) {
									RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), new File(actionFilePath + actionFile), winPrincipal
											.getName(), new java.util.Date(), "File " + actionFile + " moved to \""
											+ directoriesMap.get(actionTarget).get("description") + "\" directory");
								} else {
									RunInfoUtil.addLog(keyMap.get("path").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(),
											new java.util.Date(), "File " + actionFile + " moved to \""
													+ directoriesMap.get(actionTarget).get("description") + "\" directory");
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
									RunInfoUtil.addLogExplicit(keyMap.get("runInfo").toString(), new File(actionFilePath + actionFile), winPrincipal
											.getName(), new java.util.Date(), "File " + actionFile + " deleted from \"" + actionFilePath
											+ "\" directory");
								} else {
									RunInfoUtil.addLog(keyMap.get("path").toString(), new File(actionFilePath + actionFile), winPrincipal.getName(),
											new java.util.Date(), "File " + actionFile + " deleted from \"" + actionFilePath + "\" directory");
								}
							} else {
								request.setAttribute("errMsg", "ERROR: Unable to delete " + actionFile + " from \"" + actionFilePath
										+ "\" directory.");
							}
						}
					}
				}
			}

			// ////////////////////////////////////////////////////////////////////////////////////
			// CURRENT KEY logic
			// ////////////////////////////////////////////////////////////////////////////////////
			// functionality to perform if we're in the context of an uploader
			if (!ObjectUtil.isEmpty(pageMap.get("key"))) {
				Map keyMap = configMap.get(pageMap.get("key"));
				
				if (Context.isCompletelySubstituted(keyMap.get("path").toString())) {
					keyMap.put("pathValid", true);
				}

				if (!ObjectUtil.isEmpty(keyMap.get("runInfo")) && Context.isCompletelySubstituted(keyMap.get("runInfo").toString())) {
					keyMap.put("runInfoValid", true);
				}

				Map<String, Map<String, Object>> directoriesMap = (Map<String, Map<String, Object>>) keyMap.get("directories");

				Boolean allDirPathsValid = true;
				
				for (int i = 0; i < directoriesMap.keySet().size(); i++) {
					String key = (String) directoriesMap.keySet().toArray()[i];
					Map<String, Object> directoryMap = (Map<String, Object>) directoriesMap.get(key);
					if (!"0".equals(directoryMap.get("display"))) {
						Integer limit = null;
						if (!ObjectUtil.isEmpty(directoryMap.get("limit")))
							limit = Integer.parseInt(directoryMap.get("limit").toString());

						if (Context.isCompletelySubstituted(directoryMap.get("path").toString())) {
							directoryMap.put("valid", true);
							List<Map<String, String>> fileList = getFileList(directoryMap.get("path").toString(), limit, directoryMap.get("sort")
									.toString());
							directoryMap.put("data", fileList);
	
							if (fileList.size() > 0) {
								if (ObjectUtil.isEmpty(directoryMap.get("size")) || "-999".equals(directoryMap.get("size"))) {
									int alg = (fileList.size() * 25) + 25;
	
									// test 'all' integer code
									if ("-999".equals(directoryMap.get("size"))) {
										// as big as contents
										directoryMap.put("size", alg);
									} else {
										// as big as max
										if (alg > 255) {
											directoryMap.put("size", 255);
										} else {
											directoryMap.put("size", alg);
										}
									}
								}
							} else {
								directoryMap.put("size", 60);
							}
						} else {
							allDirPathsValid = false;
						}
					}
				}
				
				keyMap.put("dirPathsValid", allDirPathsValid);
				
				// determine if a help directory exists, using the directories
				// base path element
				String helpPath = keyMap.get("path") + "/_help";
				
				if (Context.isCompletelySubstituted(helpPath)) {
					if (new File(helpPath).exists()) {
						Map<String, Object> helpDirectoryMap = new HashMap<String, Object>();
	
						String directoryKey = "_help";
	
						helpDirectoryMap.put("display", "1");
						helpDirectoryMap.put("writable", "0");
						helpDirectoryMap.put("sort", "a");
						helpDirectoryMap.put("description", "Help Documents");
	
						helpDirectoryMap.put("path", helpPath);
	
						List<Map<String, String>> fileList = getFileList(helpDirectoryMap.get("path").toString(), null, helpDirectoryMap.get("sort")
								.toString());
						helpDirectoryMap.put("data", fileList);
	
						request.setAttribute("helpDir", "1");
						request.setAttribute("_help", helpDirectoryMap);
					}
				}

				if (StringUtil.hasValue(keyMap.get("name").toString())) {
					request.setAttribute("uploadKeyDisplay", " - " + keyMap.get("name").toString());
					request.setAttribute("uploadName", keyMap.get("name").toString());
				}

				if (!ObjectUtil.isEmpty(keyMap.get("fileUploads"))) {
					request.setAttribute("hasFileUploads", "1");
					request.setAttribute("fileUploads", keyMap.get("fileUploads"));
				}

				if (!ObjectUtil.isEmpty(keyMap.get("actions"))) {
					request.setAttribute("hasActions", "1");
					request.setAttribute("actions", keyMap.get("actions"));
				}

				// one of the last things we'll do is perform an authorization
				// check
				// if we're not authorized, we'll set a flag so that and that
				// will help control
				// the display
				// (there's also a second check during the upload of the file
				// just to make
				// sure nothing slips through)
				AuthorizationObject authObject = isAuthorized(keyMap, winPrincipal);
				if (!authObject.isAuthorized) {
					request.setAttribute("errMessage", authObject.getMessage());
				}
				request.setAttribute("authorized", authObject.isAuthorized);
				// List authUserList = (List) keyMap.get("authorizedUserList");
				// if
				// (!authUserList.contains(winPrincipal.getName().toLowerCase()))
				// {
				// logger.info("current user (" + winPrincipal.getName() + ") is
				// not authorized to upload files to (" +
				// request.getSession().getAttribute("uploadKey") + ")");
				//					
				// }

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

							// first check for the contextValues list in the key
							// map
							if (!ObjectUtil.isEmpty(keyMap.get("contextValues"))) {
								request.setAttribute("contextValues", keyMap.get("contextValues"));

							} else {

								// for legacy and as a default, we'll use the
								// authorizedUserList in the keyMap
								// this might later be changed to pull from
								// other authorization sources
								request.setAttribute("contextValues", keyMap.get("authorizedUserList"));
							}
						}
					}

				}

				// pass along our switch variable
				request.setAttribute("allowUpload", keyMap.get("allowUpload"));
				request.setAttribute("keyMap", keyMap);
				request.setAttribute("directories", keyMap.get("directories"));
			}
			// //// end of current key logic

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
		if (pageMap.get("fatalMsg") == null)
			return "";
		return (String) pageMap.get("fatalMsg");
	}

	private List<Map<String, String>> getFileList(String path, Integer limit, String sortOrder) {
		File directory = new File(path);
		// logger.info("directory: " + directory);

		Integer fileLimit = 50;
		if (!ObjectUtil.isEmpty(limit)) {
			fileLimit = limit;
		}

		File[] files = null;
		List<Map<String, String>> fileList = new ArrayList<Map<String, String>>();
		SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy h:mm a");

		if (directory.exists()) {
			files = directory.listFiles();
			// logger.info("files: " + files);

			if ("d".toLowerCase().equals(sortOrder)) {
				// sort the list of files by modified date descending
				Arrays.sort(files, new Comparator() {
					public int compare(Object o1, Object o2) {
						if (((File) o1).lastModified() > ((File) o2).lastModified()) {
							return -1;
						} else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
							return +1;
						} else {
							return 0;
						}
					}
				});
			} else {
				// sort the list of files by modified date ascending
				Arrays.sort(files, new Comparator() {
					public int compare(Object o1, Object o2) {
						if (((File) o1).lastModified() > ((File) o2).lastModified()) {
							return +1;
						} else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
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
					fileMap.put("size", FileUtils.byteCountToDisplaySize(file.length()));
					fileMap.put("age", StringUtil.pad(fileAge.toString(), "0", 20));

					// logger.info("fileMap: " + fileMap);
					fileList.add(fileMap);
				}
			}
		}

		// trim to the limit
		// while (fileList.size() > fileLimit) {
		// fileList.remove(fileList.size()-1);
		// }
		// logger.info("fileList: " + fileList);
		return fileList;
	}

	private AuthorizationObject isAuthorized(Map keyMap, WindowsPrincipal winPrincipal) throws Exception {
		AuthorizationObject authObject = new AuthorizationObject();

		// always check for the authList for legacy compatability, then check
		// for the newer "auth" sections for the different kinds
		List authUserList = (List) keyMap.get("authorizedUserList");
		if (!ObjectUtil.isEmpty(keyMap.get("authorizedUserList"))) {
			if (authUserList.contains(winPrincipal.getName().toLowerCase())) {
				authObject.setIsAuthorized(true);
			} else {
				// logger.info("current user (" + winPrincipal.getName() + ") is
				// not authorized to use the file utility for (" +
				// keyMap.get("type") + ")");
				authObject.setMessage("Sorry!  You don't have access to use the file utility for " + keyMap.get("type") + ".");
				authObject.setIsAuthorized(false);
			}
			return authObject;
		}

		return authObject;
	}

	private AuthorizationObject isAdmin(Map keyMap, WindowsPrincipal winPrincipal) throws Exception {
		AuthorizationObject authObject = new AuthorizationObject();

		// always check for the authList for legacy compatability, then check
		// for the newer "auth" sections for the different kinds
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
	 * whenever we have a directory name, we need run it through the global
	 * context to substitute out any potential usernames
	 * 
	 * @param doc
	 * @param pageMap
	 * @param configMap
	 * @param user
	 * @param globalContext 
	 * @return
	 * @throws Exception
	 */
	private List<String> validateConfig(SimpleNode doc, Map<String, Object> pageMap, Map<String, 
			Map<String, Object>> configMap, WindowsPrincipal user, Context globalContext)
			throws Exception {

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

						// now that we have the type - if we're in "key" mode
						// and the user has selected
						// a specific upload page, don't validate the other
						// ones, we don't need them
						// and we could possibly fail for an unrelated directory
						// error
						if (!ObjectUtil.isEmpty(pageMap.get("key"))) {
							if (!type.equals(pageMap.get("key"))) {
								continue;
							}
						}

						if (ObjectUtil.isEmpty(uploadNode.getTextContent("{authorizedUsers}"))) {
							messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedUsers keyword not found");
						} else {
							File file = new File(uploadNode.getTextContent("{authorizedUsers}"));
							if (!file.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedUsers file (" + file.getAbsolutePath()
										+ ") does not exist or cannot be found");
							} else {
								String s = FileUtil.loadFile(file.getAbsolutePath());
								s = s.toLowerCase();
								List<String> sList = StringUtil.toList(s, "\n ");
								List<String> usersList = new ArrayList<String>();
								for (String sL : sList) {
									if (StringUtil.hasValue(sL) && !sL.startsWith("#"))
										usersList.add(sL);
								}
								Collections.sort(usersList);

								uploadMap.put("authorizedUserList", usersList);
							}
						}

						// we want to process this section up at the top of this
						// validation method
						// because things below it might depend if they aren't
						// an "admin"
						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("{authorizedAdmins}"))) {
							File file = new File(uploadNode.getTextContent("{authorizedAdmins}"));
							if (!file.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " authorizedAdmins file (" + file.getAbsolutePath()
										+ ") does not exist or cannot be found");
							} else {
								String s = FileUtil.loadFile(file.getAbsolutePath());
								s = s.toLowerCase();
								uploadMap.put("authorizedAdminList", StringUtil.toList(s, "\n "));

								// let's go ahead and see if the user is an
								// admin, since things below might
								// depend on it
								AuthorizationObject authAdminObject = isAdmin(uploadMap, (WindowsPrincipal) pageMap.get("winPrincipal"));
								if (!authAdminObject.isAuthorized) {
									pageMap.remove("contextValue");
								}

							}
						} else {
							// without an admin list, we have no admins - so we
							// therefore can have no "contextValue"
							// kill it here in case someone snuck it in on the
							// url
							pageMap.remove("contextValue");
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("{contextList}"))) {
							File file = new File(uploadNode.getTextContent("{contextList}"));
							if (!file.exists()) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " contextList file (" + file.getAbsolutePath()
										+ ") does not exist or cannot be found");
							} else {
								String s = FileUtil.loadFile(file.getAbsolutePath());
								List<String> sList = StringUtil.toList(s, "\n ");
								for (String sL : sList) {
									if (sL.startsWith("#"))
										sList.remove(sL);
								}
								uploadMap.put("contextValues", sList);
							}
						} else if (!ObjectUtil.isEmpty(uploadNode.getTextContent("{contextValues}"))) {
							List<String> contextValuesList = new ArrayList<String>();
							SimpleNode contextValuesNode = uploadNode.getSimpleNode("{contextValues}");
							for (int l = 0; l < contextValuesNode.getChildNodes("contextValue").getLength(); l++) {
								SimpleNode contextValueNode = new SimpleNode(contextValuesNode.getChildNodes("contextValue").item(l));
								contextValuesList.add(contextValueNode.getTextContent());
							}
							uploadMap.put("contextValues", contextValuesList);
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("{defaultContext}"))) {
							pageMap.put("contextDefaultValue", uploadNode.getTextContent("{defaultContext}"));
							uploadMap.put("contextDefaultValue", uploadNode.getTextContent("{defaultContext}"));
							globalContext.add("global", "context", uploadMap.get("contextDefaultValue"));
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
								path = path.substring(0, path.length() - 1);
							}

							// if we are acting as another user, also need to
							// substitute out here
							if (!ObjectUtil.isEmpty(pageMap.get("contextValue"))) {
								path = path.replace(globalContext.get("global", "username"), pageMap.get("contextValue").toString());
							}
							uploadMap.put("path", globalContext.keywordSubstitute(path));
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("runInfo"))) {
							String runInfo = uploadNode.getTextContent("runInfo");

							runInfo = globalContext.keywordSubstitute(runInfo);

							// if we are acting as another user, also need to
							// substitute out here
							if (!ObjectUtil.isEmpty(pageMap.get("contextValue"))) {
								runInfo = runInfo.replace(globalContext.get("global", "username"), pageMap.get("contextValue").toString());
							}
							uploadMap.put("runInfo", runInfo);
						}

						if (!ObjectUtil.isEmpty(uploadNode.getTextContent("runInfoRefresh"))) {
							Integer runInfoRefresh = 10;
							try {
								runInfoRefresh = Integer.parseInt(uploadNode.getTextContent("runInfoRefresh"));
							} catch (Exception e) {
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " runInfoRefresh must be an integer");
							}
							uploadMap.put("runInfoRefresh", runInfoRefresh);
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

										if (!ObjectUtil.isEmpty(pageMap.get("contextValue"))) {
											directoryKey = directoryKey.replace(globalContext.get("global", "username"), pageMap.get("contextValue")
													.toString());
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
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j
													+ " - 'limit' value must be an integer");
										}
									}

									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("size"))) {
										String s = directoryNode.getAttribute("size");
										Integer size = 0;
										if ("short".equals(s.toLowerCase())) {
											size = 65;
										} else if ("medium".equals(s.toLowerCase())) {
											size = 150;
										} else if ("tall".equals(s.toLowerCase())) {
											size = 255;
										} else if ("all".equals(s.toLowerCase())) {
											size = -999;
										} else {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j
													+ " - 'size' value must be 'short', 'medium', or 'tall'");
										}
										directoryMap.put("size", size);
									}

									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("sort")))
										directoryMap.put("sort", ("a".equals(directoryNode.getAttribute("sort")) ? "a" : "d"));

									if (!ObjectUtil.isEmpty(directoryNode.getTextContent("{description}")))
										directoryMap.put("description", directoryNode.getTextContent("{description}"));

									if (!ObjectUtil.isEmpty(directoryNode.getTextContent("{subDescription}")))
										directoryMap.put("subDescription", directoryNode.getTextContent("{subDescription}"));

									if (!ObjectUtil.isEmpty(directoryNode.getAttribute("error")))
										directoryMap.put("error", ("1".equals(directoryNode.getAttribute("error")) ? "1" : "0"));

									// path validation
									String path = uploadMap.get("path") + "/" + directoryKey;

									// don't validate directories if they have context vars that we can't sub out yet
									if (Context.isCompletelySubstituted(path)) {
										File dir = new File(path);
										if (!dir.exists()) {
											if (!dir.mkdir()) {
												messages.add("[Upload #" + uploadCnt + "]" + typeLog + " directory " + j + " (" + dir.getAbsolutePath()
														+ ") unable to create directory");
											}
										} else if (dir.exists() && !dir.canWrite() && "1".equals(directoryMap.get("writable"))) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " destDir " + j + " (" + dir.getAbsolutePath()
													+ ") exists but is not writable");
										}
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
												actionMap.put("fileAge", StringUtil.pad(actionNode.getAttribute("fileAge"), "0", 20));
											if (!ObjectUtil.isEmpty(actionNode.getAttribute("target"))) {
												String target = globalContext.keywordSubstitute(actionNode.getAttribute("target"));
												actionMap.put("target", target);
											}
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

									if (!ObjectUtil.isEmpty(actionNode.getAttribute("confirm")))
										actionMap.put("confirm", ("1".equals(actionNode.getAttribute("confirm")) ? "1" : "0"));

									actionMap.put("display", actionNode.getTextContent("{display}"));
									actionMap.put("buttonLabel", actionNode.getTextContent("{buttonLabel}"));
									actionMap.put("description", actionNode.getTextContent("{description}"));

									// get command(s)
									if (!ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}"))) {
										List<Map<String, Object>> cmds = new ArrayList<Map<String, Object>>();

										if (ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}").getAttribute("type"))) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog
													+ " cmds keyword must have a type attribute of 'class' or 'shell'");
										} else {
											actionMap.put("cmdType", actionNode.getSimpleNode("{cmds}").getAttribute("type"));
											Map<String, Object> cmdMap = new HashMap<String, Object>();

											if (!ObjectUtil.isEmpty(actionNode.getSimpleNode("{cmds}").getAttribute("mode"))) {
												String cmdMode = actionNode.getSimpleNode("{cmds}").getAttribute("mode");
												if (!"sync".equals(cmdMode.toLowerCase()) && !"async".equals(cmdMode.toLowerCase())) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog
															+ " cmd attribute mode must be either 'sync' or 'async'");
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
												cmdMap.put("startDir", cmdNode.getTextContent("{startDir}"));
												cmdMap.put("logFile", cmdNode.getTextContent("{logFile}"));

												// get input values if present
												List<String> inputList = new ArrayList<String>();
												SimpleNode inputsNode = cmdNode.getSimpleNode("{inputs}");
												if (!ObjectUtil.isEmpty(inputsNode)) {
													for (int l = 0; l < inputsNode.getChildNodes("input").getLength(); l++) {
														SimpleNode inputNode = new SimpleNode(inputsNode.getChildNodes("input").item(l));
														inputList.add(inputNode.getTextContent());
													}
												}
												cmdMap.put("inputs", inputList);

												// get property values if
												// present
												Map<String, String> propertyMap = new HashMap<String, String>();
												if (!ObjectUtil.isEmpty(cmdNode.getSimpleNode("{properties}"))) {
													for (int l = 0; l < cmdNode.getSimpleNode("{properties}").getChildNodes("property").getLength(); l++) {
														SimpleNode propertyNode = new SimpleNode(cmdNode.getSimpleNode("{properties}").getChildNodes(
																"property").item(l));
														if (ObjectUtil.isEmpty(propertyNode.getAttribute("name"))) {
															messages.add("[Upload #" + uploadCnt + "]" + typeLog
																	+ " property keyword must have a name attribute");
														} else {
															propertyMap.put(propertyNode.getAttribute("name"), propertyNode.getTextContent());
														}
													}
												}
												cmdMap.put("properties", propertyMap);

												// get classpath values if
												// present
												List<String> classpathList = new ArrayList<String>();
												if (!ObjectUtil.isEmpty(cmdNode.getSimpleNode("{classpath}"))) {
													for (int l = 0; l < cmdNode.getSimpleNode("{classpath}").getChildNodes("entry").getLength(); l++) {
														SimpleNode classpathNode = new SimpleNode(cmdNode.getSimpleNode("{classpath}").getChildNodes(
																"entry").item(l));
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
										target = globalContext.keywordSubstitute(target);

										if (!ObjectUtil.isEmpty(pageMap.get("contextValue"))) {
											target = target.replace(globalContext.get("global", "username"), pageMap.get("contextValue").toString());
										}

										fileUploadMap.put("target", target);
									} else {
										messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload [" + fileUploadNode.getAttribute("name")
												+ "] must have target element");
									}

									fileUploadMap.put("display", fileUploadNode.getTextContent("{display}"));
									fileUploadMap.put("buttonLabel", fileUploadNode.getTextContent("{buttonLabel}"));
									fileUploadMap.put("description", fileUploadNode.getTextContent("{description}"));
									fileUploadMap.put("fileNameRegEx", fileUploadNode.getTextContent("{fileNameRegEx}"));
									fileUploadMap.put("fileNameRegExText", fileUploadNode.getTextContent("{fileNameRegExText}"));

									if (!ObjectUtil.isEmpty(fileUploadNode.getTextContent("alias")))
										fileUploadMap.put("alias", ("1".equals(fileUploadNode.getTextContent("alias")) ? "1" : "0"));

									
									// look for file validations
									if (!ObjectUtil.isEmpty(fileUploadNode.getSimpleNode("{validations}"))) {
										SimpleNode validationsNode = fileUploadNode.getSimpleNode("{validations}");
										Map<String, Object> validationsMap = new HashMap<String, Object>();
										
										if (ObjectUtil.isEmpty(validationsNode.getTextContent("delimiter"))) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " must have a delimiter child node in the validations node");
										} else {
											validationsMap.put("delimiter", validationsNode.getTextContent("delimiter"));
										}
										
										if (ObjectUtil.isEmpty(validationsNode.getTextContent("{startIndex}"))) {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " must have a startIndex child node in the validations node");
										} else {
											Integer startIndex = null;
											try {
												startIndex = Integer.parseInt(validationsNode.getTextContent("startIndex"));
											} catch (Exception e) {
												messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " startIndex must be an integer");
											}
											validationsMap.put("startIndex", startIndex);
										}

										// look for input config
										if (!ObjectUtil.isEmpty(validationsNode.getSimpleNode("{inputs}"))) {
											NodeList inputs = validationsNode.getSimpleNode("{inputs}").getChildNodes("input");
											validationsMap.put("inputs", inputs);
										} else {
											messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " must have inputs defined for validations");
										}

										// look for row rules
										if (!ObjectUtil.isEmpty(validationsNode.getSimpleNode("{rowRules}"))) {
											NodeList ruleNodes = validationsNode.getSimpleNode("{rowRules}").getChildNodes("rule");
											List<Object> rowRules = new ArrayList<Object>();
											
											for (int l = 0; l < ruleNodes.getLength(); l++) {
												SimpleNode ruleNode = new SimpleNode(ruleNodes.item(l));
												Map<String, String> ruleMap = new HashMap<String, String>();

												// rules must have a name, error text, and a service section
												if (ObjectUtil.isEmpty(ruleNode.getTextContent("name"))) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have a name");
												} else {
													ruleMap.put("name", ruleNode.getTextContent("name"));
												}

												if (ObjectUtil.isEmpty(ruleNode.getTextContent("errorText"))) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have an errorText node");
												} else {
													ruleMap.put("errorText", ruleNode.getTextContent("errorText"));
												}

												if (ObjectUtil.isEmpty(ruleNode.getTextContent("cacheKey"))) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have a cacheKey node");
												} else {
													ruleMap.put("cacheKey", ruleNode.getTextContent("cacheKey"));
												}

												// inspect the service node
												if (ObjectUtil.isEmpty(ruleNode.getTextContent("service"))) {
													messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have a service node");
												} else {
													SimpleNode serviceNode = ruleNode.getSimpleNode("service");
													
													if (ObjectUtil.isEmpty(serviceNode.getTextContent("type"))) {
														messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have a service type");
													} else {
														ruleMap.put("type", serviceNode.getTextContent("type").toLowerCase());
														
														if ("js".equals(ruleMap.get("type"))) {
															
														} else if ("sql".equals(ruleMap.get("type"))) {
															
															// driver is required
															logger.info("serviceNode: " + serviceNode);
															logger.info("serviceNode.getTextContent(\"dbDriver\"): " + serviceNode.getTextContent("dbDriver"));
															if (ObjectUtil.isEmpty(serviceNode.getTextContent("dbDriver"))) {
																messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " of sql type must have a service dbDriver node");
															} else {
																ruleMap.put("dbDriver", serviceNode.getTextContent("dbDriver"));
															}

															if (ObjectUtil.isEmpty(serviceNode.getTextContent("rowsFoundGood"))) {
																messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " of sql type must have a service rowsFoundGood node");
															} else {
																ruleMap.put("rowsFoundGood", serviceNode.getTextContent("rowsFoundGood"));
															}
														} else {
															messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " of contains unsupported rowRule type " + ruleMap.get("type"));
														}
													}

													if (ObjectUtil.isEmpty(serviceNode.getTextContent("code"))) {
														messages.add("[Upload #" + uploadCnt + "]" + typeLog + " fileUpload " + fileUploadMap.get("name") + " rule " + l + " must have a service code node");
													} else {
														ruleMap.put("code", serviceNode.getTextContent("code"));
													}
												}

												rowRules.add(ruleMap);
											}
											
											validationsMap.put("rowRules", rowRules);
										}

										fileUploadMap.put("validations", validationsMap);
									}
									
									fileUploadsMap.put(fileUploadMap.get("name").toString(), (Object) fileUploadMap);
								}

								uploadMap.put("fileUploads", fileUploadsMap);

							} catch (Exception e) {
								logger.error("", e);
								messages.add("[Upload #" + uploadCnt + "]" + typeLog + " error in fileUploads section (" + ObjectUtil.toString(e)
										+ ")");
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
	
	public class LineValidationObject {
		/** list of messages for the row */
		private List<String> messages = new ArrayList<String>();
		/** ordered map of row data */
		private LinkedHashMap<String, Object> content = new LinkedHashMap<String, Object>();
		/** flag denoting if the row passed validation */
		String valid;
		
		public void addDataColumn(String columnName, String data, Boolean valid, String message) {
			Map<String, Object> columnMap = new HashMap<String, Object>();
			columnMap.put("column", convertStringToProper(columnName));
			columnMap.put("data", data);
			
			if (valid) {
				columnMap.put("valid", "0");
			} else {
				columnMap.put("valid", "1");
			}
			
			if (!ObjectUtil.isEmpty(message))
				columnMap.put("message", message);
			
			content.put(columnName, columnMap);
		}
		public void addMessage(String message) {
			messages.add(message);
		}
		public List<String> getMessages() {
			return messages;
		}
		public void setMessages(List<String> messages) {
			this.messages = messages;
		}
		public LinkedHashMap<String, Object> getContent() {
			return content;
		}
		public void setContent(LinkedHashMap<String, Object> content) {
			this.content = content;
		}
		public String getValid() {
			return valid;
		}
		public void setValid(Boolean valid) {
			if (valid) {
				this.valid = "0";
			} else {
				this.valid = "1";
			}
		}
	}

	private String convertStringToProper(String s) {
		String s2 = "";
		s = StringUtil.upperCaseFirstLetter(s);
		if (!s.contains(" ")) {
			// skip beginning caps, then replace all other caps with space-caps
			Boolean begin = false;
			for (int i = 0; i < s.length(); i++) {
				if (s.substring(i, i+1).matches("[a-z]")) {
					begin = true;
				}
				if (begin) {
					if (s.substring(i, i+1).matches("[A-Z]")) {
						s2 += " " + s.substring(i, i+1);
					} else {
						s2 += s.substring(i, i+1);
					}
				}else {
					s2 += s.substring(i, i+1);
				}
			}
		} else {
			s2 = s;
		}
		return s2;
	}

	private File createDelimitedFileFromXls(File file, String extension, String delim, Integer sheetNum) {
		File csvFile = null;

		try {
			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
			HSSFSheet sheet = new HSSFWorkbook(fs).getSheetAt(sheetNum);
			List<String> rowList = new ArrayList<String>();

			for (int i = 0; i <= sheet.getLastRowNum(); i++) {
				HSSFRow row = sheet.getRow(i);
				String rowString = "";
				for (int j = 0; j <= row.getLastCellNum(); j++) {
					String cellValue = getCellValue(sheet, i, j);
					rowString += "\"" + cellValue + "\"" + delim;
				}
				rowList.add(rowString);
			}
			
			// create the csv file
			String csvPath = file.getAbsolutePath().replace(file.getName(), "");
			if (!csvPath.endsWith("/"))
				csvPath += "/";
			String csvFileName = file.getName().replace(".xls", extension);
			
			csvFile = new File(csvPath + csvFileName);
			FileUtil.saveFile(csvFile, rowList);
			
		} catch (Exception e) {
			logger.error("", e);
		}

		return csvFile;
	}
	
	public static String getCellValue(HSSFSheet sheet, int row, int column) {
		if(sheet.getRow(row) == null || sheet.getRow(row).getCell((short)column) == null)
			return "";
		int type = sheet.getRow(row).getCell((short)column).getCellType();
		if(type == HSSFCell.CELL_TYPE_NUMERIC) {
			if(HSSFDateUtil.isCellDateFormatted(sheet.getRow(row).getCell((short)column))) {
				try{
					return excelToCSVdateFormat.format(sheet.getRow(row).getCell((short)column).getDateCellValue());
				}catch(Exception e) {
					if(sheet.getRow(row).getCell((short)column).getRichStringCellValue() == null)
						return "";
					return sheet.getRow(row).getCell((short)column).getRichStringCellValue().getString();
				}
			}else{
				Double value = sheet.getRow(row).getCell((short)column).getNumericCellValue();
				try {
					return ""+value.longValue();
				} catch (Exception e) {
					// the value apparently wasn't an integer
				}
				if(value == null)
					return "";
				return ""+value;
			}
		} 
		if(sheet.getRow(row).getCell((short)column).getRichStringCellValue() == null)
			return "";
		return sheet.getRow(row).getCell((short)column).getRichStringCellValue().getString();
	}

}
