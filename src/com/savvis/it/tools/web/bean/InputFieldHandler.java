/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import com.savvis.it.db.*;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.XmlUtil;
import com.sun.corba.se.impl.orbutil.closure.Constant;

/**
 * This class handles the processing and creation of generic inputs for web pages. 
 * 
 * @author David R Young
 * @version $Id: InputFieldHandler.java,v 1.7 2008/09/17 18:33:03 dyoung Exp $
 */
public class InputFieldHandler {	
	private static Logger logger = Logger.getLogger(InputFieldHandler.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/bean/Attic/InputFieldHandler.java,v 1.7 2008/09/17 18:33:03 dyoung Exp $";
	
	/*
	 * Valid types of inputs:
	 * 
	 * <sv:date></sv:date>
	 * <sv:input type=""></sv:input>
	 * <sv:select></sv:select>
	 */
	
	private SimpleNode node;
	private String type;
	private String name;
	private String label;
	private List<String> values;
	private String defaultValue;
	
	private Boolean required;
	private Boolean readonly;

	private static final String INPUT_TYPE_SELECT = "select";
	private static final String INPUT_TYPE_SQLSELECT = "sqlselect";
	private static final String INPUT_TYPE_TEXT = "input";
	private static final String INPUT_TYPE_DATE = "date";
	private static final String INPUT_TYPE_NUMERIC = "numeric";

	public InputFieldHandler() {
	}

	public static Map<String, InputFieldHandler> createInputs(SimpleNode node) throws Exception {
		Map<String, InputFieldHandler> inputsMap = new LinkedHashMap<String, InputFieldHandler>();
		
		if (ObjectUtil.isEmpty(node.getChildNodes("input")))
			throw new Exception("no input keywords found");
		
		NodeList inputs = node.getChildNodes("input");
		
		for (int i = 0; i < inputs.getLength(); i++) {
			SimpleNode inputNode = new SimpleNode(inputs.item(i));
			InputFieldHandler input = new InputFieldHandler();
			input.setNode(inputNode);
			
			if (!ObjectUtil.isEmpty(inputNode.getAttribute("mandatory"))) {
				if ("1".equals(inputNode.getAttribute("mandatory"))) {
					input.setRequired(true);
				} else {
					input.setRequired(false);
				}
			}

			if (!ObjectUtil.isEmpty(inputNode.getAttribute("readonly"))) {
				if ("1".equals(inputNode.getAttribute("readonly"))) {
					input.setReadonly(true);
				} else {
					input.setReadonly(false);
				}
			}

			input.setName(inputNode.getTextContent("{name}"));
			input.setType(inputNode.getTextContent("{type}"));
			input.setLabel(inputNode.getTextContent("{label}"));
			input.setDefaultValue(inputNode.getTextContent("{defaultValue}"));

			// simple list
			if (INPUT_TYPE_SELECT.equals(input.getType().toLowerCase())) {
				if (!ObjectUtil.isEmpty(inputNode.getSimpleNode("{values}"))) {
					List<String> values = new ArrayList<String>();
					for (int j = 0; j < inputNode.getSimpleNode("{values}").getChildNodes("{value}").getLength(); j++) {
						SimpleNode valueNode = new SimpleNode(inputNode.getSimpleNode("{values}").getChildNodes("{value}").item(j));
						values.add(valueNode.getTextContent());
					}
					
					if (!ObjectUtil.isEmpty(inputNode.getAttribute("{values}", "sort")) && "1".equals(inputNode.getAttribute("{values}", "sort").toString()))
						Collections.sort(values);
					
					input.setValues(values);
				}
			}
			
			// we will retrieve values from a database during the "get" method, only validation happens here
			if (INPUT_TYPE_SQLSELECT.equals(input.getType().toLowerCase())) {
				if (!ObjectUtil.isEmpty(inputNode.getSimpleNode("{values_sql}"))) {
					String dbDriver = inputNode.getSimpleNode("{values_sql}").getAttribute("dbdriver");
					
					if (ObjectUtil.isEmpty(inputNode.getSimpleNode("{values_sql}").getAttribute("valueCol")))
						throw new Exception("valueCol is required for values_sql");
				}				
			}

			inputsMap.put(input.getName(), input);
		}		
		return inputsMap;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<String> getValues() {
		
		try {
			if (ObjectUtil.isEmpty(this.values) && INPUT_TYPE_SQLSELECT.equals(this.type.toLowerCase())) {
				if (!ObjectUtil.isEmpty(this.node.getNode("{values_sql}"))) {
					String dbDriver = this.node.getSimpleNode("{values_sql}").getAttribute("dbdriver");
					String valueCol = this.node.getSimpleNode("{values_sql}").getAttribute("valueCol");
					String sql = this.node.getTextContent("{values_sql}");
					
					DBUtil.setEnableKeywordSubstitution(true);
					List results = DBUtil.executeQuery(dbDriver, sql);
					if (results.size() > 0) {
						List<String> values = new ArrayList<String>();
						for (int j = 0; j < results.size(); j++) {
							Map result = (Map) results.get(j);
							values.add(result.get(valueCol).toString());
						}
						this.values = values;
					}
				}				
			}
		} catch (Exception e) {
			logger.error(e);
			throw new RuntimeException(e);
		}

		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
	
	public static void main(String[] args) {
		SimpleNode doc = new SimpleNode(XmlUtil.loadDocumentFromFile("C:/tmp/appLaunchTest.txt"));
		NodeList actions = doc.getSimpleNode("{actions}").getChildNodes("action");
//		logger.info("actions: " + actions);
//		logger.info("actions.getLength(): " + actions.getLength());
		
		try {
			for (int i = 0; i < actions.getLength(); i++) {
				SimpleNode actionNode = new SimpleNode(actions.item(i));
//				logger.info("actionNode: " + actionNode);
				
				SimpleNode inputsNode = actionNode.getSimpleNode("{inputs}");
//				logger.info("inputsNode: " + inputsNode);
				
				Map<String, InputFieldHandler> inputMap = createInputs(inputsNode);
//				logger.info("inputMap.toString(): " + inputMap.toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public SimpleNode getNode() {
		return node;
	}

	public void setNode(SimpleNode node) {
		this.node = node;
	}
}
