/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import com.savvis.it.db.DBConnection;
import com.savvis.it.db.service.DBService;
import com.savvis.it.servlet.SavvisServlet;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.StringUtil;
import com.savvis.it.util.XmlUtil;

/**
 * 
 * @author Moorhem
 * @version $Id: PasswordResetServlet.java,v 1.1 2008/05/01 16:19:40 dmoorhem Exp $
 */
public class PasswordResetServlet extends SavvisServlet {	
	private static Logger logger = Logger.getLogger(PasswordResetServlet.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/servlet/PasswordResetServlet.java,v 1.1 2008/05/01 16:19:40 dmoorhem Exp $";
	private static SimpleNode passResetRootNode = null;
	
	/** 
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param action the action to perform (typically null when the servlet is first called)
	 * @param request servlet request
	 * @param response servlet response
	 */
	protected void processRequest(String action,
		HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String jspPage = "passwordReset.jsp";
		
		request.setAttribute("secUsername", request.getParameter("secUsername"));
		
		System.out.println("Action=" + action);
		
		if(action != null && action.equals("reset")) {
			performPasswordReset(request);
		}
		
		//set the list of applicable systems
		request.setAttribute("systemList", getSystemList());
		
		forward(jspPage, request, response);
	}
	
	
	private void performPasswordReset(HttpServletRequest request) {
		passResetRootNode = new SimpleNode("/passwordResetConfig.xml", "xml", true);
		
		//loop through the checkboxes
		
			//loop through our systems and perform the reset
	}


	protected void performProtectedDBOperations(DBConnection conn, Object data, String action, 
			HttpServletRequest request, HttpServletResponse response) throws Exception {
	}
	
	private List getSystemList() {
		List systemList = new ArrayList();
		
		passResetRootNode = new SimpleNode("/passwordResetConfig.xml", "xml", true);
		
		SimpleNode systemsNode = passResetRootNode.getSimpleNode("{passwordReset}{systems}");

		if (systemsNode != null) {
			NodeList systems = systemsNode.getChildNodes("system");
			
			for (Integer i = 0; i < systems.getLength(); i++) {
				SimpleNode system = new SimpleNode(systems.item(i));
				Map systemMap = new HashMap();
				
				systemMap.put("code", i.toString() + system.getAttribute("code").trim());
				systemMap.put("name", system.getSimpleNode("{name}").getTextContent().trim());
				
				systemList.add(systemMap);
			}
		}
			
		return systemList;
	}
}