/**
 * Copyright 2008 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.savvis.it.filter.WindowsAuthenticationFilter;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.Context;
import com.savvis.it.util.FileUtil;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.PropertyManager;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.StringUtil;
import com.savvis.it.util.SystemUtil;
import com.savvis.it.util.XmlCreator;
import com.savvis.it.util.XmlUtil;
import com.savvis.it.ws.GenericWebServiceClient;

/**
 * This class generically calls a web service defined by a config file
 *
 * @author theodore.elrick
 * @version $Id$
 */
public class GenericWSClientServlet extends SavvisServlet {
	
	private static Logger logger = Logger.getLogger(GenericWSClientServlet.class);
	
	protected void processRequest(String action, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
		
		WindowsAuthenticationFilter.WindowsPrincipal winPrincipal = 
			(WindowsAuthenticationFilter.WindowsPrincipal) request.getSession()
			.getAttribute(WindowsAuthenticationFilter.AUTHENTICATION_PRINCIPAL_KEY);
		
		Map<String, String> pageMap = new HashMap<String, String>();
		
		if(SystemUtil.getProperty("BASEDIR") == null)
			throw new Exception("BASEDIR not set");
		
		// show the index page
		if((action == null || "authenticate".equals(action)) && !ServletFileUpload.isMultipartContent(request)) {
			List<SimpleNode> index = new SimpleNode(XmlUtil.loadDocumentFromFileAsStream("/properties/wsIndex.xml")
						.getFirstChild()).getChildren("entry");
			Map<String, Set<WebServiceClient>> applMap = new TreeMap<String, Set<WebServiceClient>>();
//			Map<String, WebServiceClient> clients = new HashMap<String, WebServiceClient>();
//			logger.info("clients.size() = "+(clients.size()));
			for (SimpleNode entry : index) {
				WebServiceClient client = new WebServiceClient();
				client.setConfig(entry.getAttribute("config"));
				client.setAppl(entry.getAttribute("appl"));
				File configFile = new File(SystemUtil.getProperty("BASEDIR")+"/"+client.getAppl()+"/etc/"+client.getConfig()+".xml");
				if(!configFile.exists())
					continue;
				Document wsConfig = XmlUtil.loadDocumentFromFile(""+configFile);
				if(wsConfig != null) {
					SimpleNode ws = new SimpleNode(wsConfig.getFirstChild()).getSimpleNode("{ws}");
					client.setTitle(ws.getTextContent("{title}"));
					String title = toTitle(client.getAppl());
					if(applMap.get(title) == null)
						applMap.put(title, new TreeSet());
					applMap.get(title).add(client);
				}
			}
			request.setAttribute("appls", applMap);
		
			forward("/jsp/ws/index.jsp", request, response);
			return;
		}
		
		// Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();
		
		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<FileItem> items = null;
		
		String config = request.getParameter("config");
		String appl = request.getParameter("appl");
		String operation = request.getParameter("operation");
		if(ServletFileUpload.isMultipartContent(request)) {
			// Parse the request
			items = upload.parseRequest(request);
			// Process the uploaded items
			for (FileItem item : items) {
		    if (item.isFormField()) {
		    	if(item.getFieldName().equals("config")) {
		    		config = item.getString();
		    	} else if(item.getFieldName().equals("appl")) {
		    		appl = item.getString();
		    	} else if(item.getFieldName().equals("operation")) {
		    		operation = item.getString();
		    	}
		    }
			}
		}
		request.setAttribute("config", config);
		request.setAttribute("appl", appl);
		request.setAttribute("operation", operation);
		request.setAttribute("winIsLoggedIn", winPrincipal);
		
		File configFile = new File(SystemUtil.getProperty("BASEDIR")+"/"+appl+"/etc/"+config+".xml");
		
		if(config == null)
			pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  Missing required parameter (CONFIG) required.<br/>");
		if(appl == null)
			pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  Missing required parameter (APPL) required.<br/>");
		if(config != null && appl != null && !configFile.exists())
			pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  The supplied config file doesn't exist (" + 
						configFile.getAbsolutePath() + ").<br/>");
		
		// load config
		SimpleNode ws = new SimpleNode(XmlUtil.loadDocumentFromFile(
					""+configFile).getFirstChild()).getSimpleNode("{ws}");
		
		// see if user is authorized
		boolean isAuthorized = false;
		List<SimpleNode> users = ws.getSimpleNode("{authorizedUsers}").getChildren("{user}");
		if(users.size() > 0) {
			for (SimpleNode user : users) {
				if(ObjectUtil.areObjectsEqual(user.getTextContent(), winPrincipal.getName().toLowerCase()))
					isAuthorized = true;
			}
			if(!isAuthorized) {
				pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  You are not authorized to access this page<br/>");
			}
		}
		String input = request.getParameter("xml");
		if("call".equals(action) && (input == null || input.trim().length() == 0)) {
			pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  You must supply input XML to run the web service<br/>");
		}

logger.info("action = "+(action));
		if (!ObjectUtil.isEmpty(pageMap.get("fatalMsg"))) { 
			request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			
			forward("/jsp/ws/genericWebServiceClient.jsp", request, response);
			return;
		}
		
		Context context = new Context();
		String tomcatBasedir = System.getProperty("catalina.home");
		PropertyManager properties = new PropertyManager(false, true, false, tomcatBasedir+"/etc/appServer.properties");
		context.add("APP_SERVER", properties.getProperties());
		String wsdl = context.keywordSubstitute(ws.getTextContent("{wsdl}"));
		String endpoint = ws.getTextContent("{endpoint}");
		GenericWebServiceClient client = new GenericWebServiceClient(wsdl, endpoint);
		request.setAttribute("operations", client.getAvailableOperations());
		
		if(wsdl.indexOf("localhost") != -1)
			wsdl = StringUtil.replaceSubstring(wsdl, "localhost", java.net.InetAddress.getLocalHost().getCanonicalHostName());
		
		if("call".equals(action)) {
			if(input == null || input.trim().length() == 0) {
				pageMap.put("fatalMsg", getFatalMsg(pageMap)+"ERROR:  You are not authorized to access this page<br/>");
			}
			input = input.substring(input.indexOf('<'));
			String output = client.invoke(operation, input).getOutput();

			if("1".equals(request.getParameter("attachment")))
				response.setHeader("Content-Disposition", "attachment; filename=\""+operation+ ".xml\"");
			response.setContentType("application/xml");
			if("1".equals(request.getParameter("pretty"))) {
				XmlCreator xml = XmlCreator.parseXml(output);
				xml.setSorted(true);
				response.getWriter().write(xml.toString());
			} else {
				response.getWriter().write(output);
			}
			return;
		} else if("sample".equals(action)) {
			response.setContentType("application/xml");

			String xml = getSampleXml(config, appl, operation);
			response.getWriter().write(xml);
			return;
		} else if("useSample".equals(action)) {
			String xml = getSampleXml(config, appl, operation);
			request.setAttribute("xml", xml);
		} else if(ServletFileUpload.isMultipartContent(request)) {
			
			// Process the uploaded items
			for (FileItem item : items) {
		    if (!item.isFormField()) {
		    	try {
		    		request.setAttribute("xml", FileUtil.loadFileFromStream(item.getInputStream()));
						break;
		    	} catch (Exception e) {
						logger.error("", e);
						throw new RuntimeException("The file is either corrupted or is not of the correct type.  " +
								"Make sure you have saved the export as file type .xml and try to upload again.");
					}
		    }
			}
		}

		request.setAttribute("title", ws.getTextContent("{title}"));
		request.setAttribute("wsdl", wsdl);
		request.setAttribute("operation", operation);
		
		forward("/jsp/ws/genericWebServiceClient.jsp", request, response);
	}

	/**
	 * @param response
	 * @param config
	 * @param appl
	 * @param operation
	 * @throws IOException
	 */
	private String getSampleXml(String config, String appl, String operation)
				throws IOException {
		List<SimpleNode> configs = new SimpleNode(XmlUtil.loadDocumentFromFileAsStream("/properties/wsSamples.xml")
					.getFirstChild()).getChildren("client");

		for (SimpleNode configSample : configs) {
			if(config.equals(configSample.getAttribute("config")) && appl.equals(configSample.getAttribute("appl"))) {
				for (SimpleNode sample : configSample.getChildren("sample")) {
					if(operation.equals(sample.getAttribute("operation"))) {
						for (SimpleNode sampleXml : sample.getChildren()) {
							if(sampleXml.getNodeType() != Node.TEXT_NODE) {
								String xml = ""+sampleXml;
								return xml.substring(xml.indexOf("?>")+2);
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	private String getFatalMsg(Map pageMap) {
		if(pageMap.get("fatalMsg") == null)
			return "";
		return (String) pageMap.get("fatalMsg");
	}
	
	public class WebServiceClient implements Comparable {
		private String appl;
		private String config;
		private String title;
		public String getAppl() {
			return appl;
		}
		public void setAppl(String appl) {
			this.appl = appl;
		}
		public String getConfig() {
			return config;
		}
		public void setConfig(String config) {
			this.config = config;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public int compareTo(Object o) {
			WebServiceClient wsc = (WebServiceClient) o;
			return getTitle().compareTo(wsc.getTitle());
		}
	}
	
	private static String toTitle(String appl) {
		appl = (appl.substring(0, 1).toUpperCase()+appl.substring(1)).trim();
		while(appl.indexOf(' ') != -1)
			appl = appl.substring(0, appl.indexOf(' '))+" "
						+appl.substring(appl.indexOf(' ')+1, appl.indexOf(' ')+2).toUpperCase()
						+appl.substring(appl.indexOf(' ')+2);
		return appl;
	}

}
