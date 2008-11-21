/**
 * Copyright 2008 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.savvis.it.filter.WindowsAuthenticationFilter;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.SystemUtil;
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
		if(action == null || "authenticate".equals(action)) {
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
				SimpleNode ws = new SimpleNode(XmlUtil.loadDocumentFromFile(
							""+configFile).getFirstChild()).getSimpleNode("{ws}");
				client.setTitle(ws.getTextContent("{title}"));
				String title = toTitle(client.getAppl());
				if(applMap.get(title) == null)
					applMap.put(title, new TreeSet());
				applMap.get(title).add(client);
				request.setAttribute("appls", applMap);
			}
		
			forward("/jsp/ws/index.jsp", request, response);
			return;
		}
		
		String config = request.getParameter("config");
		String appl = request.getParameter("appl");
		request.setAttribute("config", config);
		request.setAttribute("appl", appl);
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

logger.info("action = "+(action));
		if (!ObjectUtil.isEmpty(pageMap.get("fatalMsg"))) { 
			request.setAttribute("fatalMsg", pageMap.get("fatalMsg"));
			
			forward("/jsp/ws/genericWebServiceClient.jsp", request, response);
			return;
		}
		
		String wsdl = ws.getTextContent("{wsdl}");
		String endpoint = ws.getTextContent("{endpoint}");
		GenericWebServiceClient client = new GenericWebServiceClient(wsdl, endpoint);
		request.setAttribute("operations", client.getAvailableOperations());
		String operation = request.getParameter("operation");
		
		if("call".equals(action)) {
			String input = request.getParameter("xml");
			input = input.substring(input.indexOf('<'));
			String output = client.invoke(operation, input).getOutput();

			response.setContentType("application/xml");
			response.getWriter().write(output);
			return;
		} else if("sample".equals(action)) {
			response.setContentType("application/xml");
			
			List<SimpleNode> configs = new SimpleNode(XmlUtil.loadDocumentFromFileAsStream("/properties/wsSamples.xml")
						.getFirstChild()).getChildren("client");

			outerLoop:
			for (SimpleNode configSample : configs) {
				if(config.equals(configSample.getAttribute("config")) && appl.equals(configSample.getAttribute("appl"))) {
					for (SimpleNode sample : configSample.getChildren("sample")) {
						if(operation.equals(sample.getAttribute("operation"))) {
							for (SimpleNode sampleXml : sample.getChildren()) {
								if(sampleXml.getNodeType() != Node.TEXT_NODE) {
									String xml = ""+sampleXml;
									xml = xml.substring(xml.indexOf("?>")+2);
									response.getWriter().write(xml);
									break outerLoop;
								}
							}
						}
					}
				}
			}
			return;
		}
		
		request.setAttribute("title", ws.getTextContent("{title}"));
		request.setAttribute("wsdl", wsdl);
		request.setAttribute("operation", operation);
		
		forward("/jsp/ws/genericWebServiceClient.jsp", request, response);
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
