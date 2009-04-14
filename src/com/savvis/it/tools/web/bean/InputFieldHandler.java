/*
 * Copyright 2006 SAVVIS Communications. All rights reserved.
 */
package com.savvis.it.tools.web.bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import com.savvis.it.db.DBUtil;
import com.savvis.it.util.ObjectUtil;
import com.savvis.it.util.SimpleNode;
import com.savvis.it.util.XmlUtil;

/**
 * This class handles the processing and creation of generic inputs for web pages. 
 * 
 * @author David R Young
 * @version $Id: InputFieldHandler.java,v 1.9 2009/04/14 18:21:47 dyoung Exp $
 */
public class InputFieldHandler {	
	private static Logger logger = Logger.getLogger(InputFieldHandler.class);
	private static String scVersion = "$Header: /opt/devel/cvsroot/SAVVISRoot/CRM/tools/java/Web/src/com/savvis/it/tools/web/bean/Attic/InputFieldHandler.java,v 1.9 2009/04/14 18:21:47 dyoung Exp $";
	
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
	private String regex;
	private String regexValidationText;
	private String format;
	private int maxlength;

	private static final String INPUT_TYPE_SELECT = "select";
	private static final String INPUT_TYPE_SQLSELECT = "sqlselect";
	private static final String INPUT_TYPE_FILESELECT = "fileselect";
	private static final String INPUT_TYPE_CFGSELECT = "cfgselect";
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
			
			// regexp
			if (!ObjectUtil.isEmpty(inputNode.getTextContent("regEx"))) {
				input.setRegex(inputNode.getTextContent("regEx"));
				
				if (!ObjectUtil.isEmpty(inputNode.getTextContent("regExText"))) {
					input.setRegexValidationText(inputNode.getTextContent("regExText"));
				}
			}

			if (!ObjectUtil.isEmpty(inputNode.getAttribute("format"))) {
				input.setFormat(inputNode.getAttribute("format"));
			}
			if (!ObjectUtil.isEmpty(inputNode.getAttribute("maxlength"))) {
				try {
					Integer maxlen = Integer.parseInt(inputNode.getAttribute("maxlength"));
					input.setMaxlength(maxlen);
				} catch (Exception e) {
					throw new Exception("maxlength must be an integer");
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
					if (ObjectUtil.isEmpty(inputNode.getSimpleNode("{values_sql}").getAttribute("valueCol")))
						throw new Exception("valueCol is required for values_sql");
				} else {
					throw new Exception("values_sql is required for input type sqlselect");
				}
			}

			// we will retrieve values from filesystem during the "get" method, only validation happens here
			if (INPUT_TYPE_FILESELECT.equals(input.getType().toLowerCase())) {
				if (ObjectUtil.isEmpty(inputNode.getSimpleNode("{values_dir}"))) {
					throw new Exception("values_dir is required for input type fileselect");
				}				
			}

			// we will retrieve values from cfg during the "get" method, only validation happens here
			if (INPUT_TYPE_CFGSELECT.equals(input.getType().toLowerCase())) {
				if (ObjectUtil.isEmpty(inputNode.getSimpleNode("{values_cfg}"))) {
					throw new Exception("values_cfg is required for input type cfgselect");
				} else {
					// validate cfg exists
					File cfgFile = new File(inputNode.getTextContent("{values_cfg}"));
					if (!cfgFile.exists()) {
						throw new Exception ("config file in values_cfg doesn't exist (" + cfgFile + ")");
					}
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
		logger.info("this.type: " + this.type);
		
		try {
			if (ObjectUtil.isEmpty(this.values)) {
				
				if (INPUT_TYPE_SQLSELECT.equals(this.type.toLowerCase())) {
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
				
				if (INPUT_TYPE_FILESELECT.equals(this.type.toLowerCase())) {
					if (!ObjectUtil.isEmpty(this.node.getNode("{values_dir}"))) {
						File dir = new File(this.node.getTextContent("{values_dir}"));
						logger.info("dir: " + dir);
						File[] fileList = dir.listFiles();
						logger.info("fileList.length: " + fileList.length);
						if (fileList.length > 0) {
							List<String> values = new ArrayList<String>();
							for (int j = 0; j < fileList.length; j++) {
								File f = fileList[j];
								values.add(f.getName());
							}
							Collections.sort(values);
							this.values = values;
						}
					}				
				}
				
				if (INPUT_TYPE_CFGSELECT.equals(this.type.toLowerCase())) {
					if (!ObjectUtil.isEmpty(this.node.getNode("{values_cfg}"))) {
						File cfg = new File(this.node.getTextContent("{values_cfg}"));
						
						if (cfg.exists()) {
							List<String> values = new ArrayList<String>();
							
							FileReader reader = new FileReader(cfg);
							BufferedReader br = new BufferedReader(reader);
							String s;
							int lineCnt = 0;
							while((s = br.readLine()) != null) { 
								if (s.startsWith("#"))
									continue;
								values.add(s);
							} 
							reader.close();
							
							Collections.sort(values);
							this.values = values;
						}
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

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getRegexValidationText() {
		return regexValidationText;
	}

	public void setRegexValidationText(String regexValidationText) {
		this.regexValidationText = regexValidationText;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public int getMaxlength() {
		return maxlength;
	}

	public void setMaxlength(int maxLength) {
		this.maxlength = maxLength;
	}
}
