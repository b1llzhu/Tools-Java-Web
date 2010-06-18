document.write('<script type="text/javascript" src="dwr/interface/P13NService.js"></script>');
document.write('<script type="text/javascript" src="dwr/engine.js"></script>');
document.write('<script type="text/javascript" src="dwr/util.js"></script>');

// submit form[0] with the specified action and optional target
function svSubmitAction(action, target) {
	if(!svValidateForm()) 
		return false;
	var temp;
	if(target) {
		temp = svGetMyForm().target;
		svGetMyForm().target = target;
	}
	svGetMyForm().action.value = action;
	svPrepareFormForSubmit(svGetMyForm());
	svGetMyForm().submit();
	if(target)
		svGetMyForm().target = temp;
	return true;
}

// submit form[0] with the specified action and index and optional target
function svSubmitActionAndIndex(action, index, target) {
	if(!svValidateForm()) 
		return false;
	var temp;
	if(target) {
		temp = svGetMyForm().target;
		svGetMyForm().target = target;
	}
	svGetMyForm().action.value = action;
	svGetMyForm().index.value = index;
	svGetMyForm().submit();
	if(target) 
		svGetMyForm().target = temp;
	return true;
}

// submit form[0] with the specified action, setting an Id field and optional target
function svSubmitActionAndId(action, index, target) {
	if(!svValidateForm()) 
		return false;
	var temp;
	if(target) {
		temp = svGetMyForm().target;
		svGetMyForm().target = target;
	}
	svGetMyForm().action.value = action;
	svGetMyForm().id.value = index;
	svGetMyForm().submit();
	if(target) 
		svGetMyForm().target = temp;
}

// submit form[0] with the specified action and optional target to a popup window
function svSubmitToPopup(action, target, width, height, menubar, location, status, scrollbars, resizable, bReplace, initialPage) {
	var options = "";
	options += (width ? "width="+width+"," : "");
	options += (height ? "height="+height+"," : "");
	options += (menubar ? "menubar="+(menubar ? "yes" : "no")+"," : "");
	options += (location ? "location="+(location ? "yes" : "no")+"," : "");
	options += (status ? "status="+(status ? "yes" : "no")+"," : "");
	options += (scrollbars ? "scrollbars="+(scrollbars ? "yes" : "no")+"," : "scrollbars=yes,");
	options += (resizable ? "resizable="+(resizable ? "yes" : "no")+"," : "resizable=yes,");
	options += (bReplace ? "bReplace="+(bReplace ? "yes" : "no")+"," : "");
	var popup = window.open(initialPage, target, svTrimCommas(options));
	svSubmitAction(action, target);
	return popup;
}

// submit form[0] with the specified action and optional target to a simple popup window
function svSubmitToStrippedDownPopup(action, target, width, height, initialPage) {
	return svSubmitToPopup(action, target, width, height, false, false, false, "yes", "yes", null, initialPage);
}

// shows or hides a div tag or element
function svShowOrHide(id, newVisibility) {
	var obj = document.getElementsByTagName("div");
	if(!obj[id]) {
		svShowOrHideElement(svGetInput(id), newVisibility);
	} else {		
		svShowOrHideElement(obj[id], newVisibility);
	}
}

// centers the div in the center of the browser
function svCenterDiv(id) {
	xMoveTo(id, (xClientWidth()/2)-(xWidth(id)/2)+xScrollLeft(), (xClientHeight()/2)-(xHeight(id)/2)+xScrollTop());
}

// shows or hides a div tag or element based on whether isVisible is true or false
function svSetVisible(id, isVisible) {
	var newVisibility = ( isVisible ? "visible" : "hidden" );
	svShowOrHide(id, newVisibility);
}

function svShowOrHideElement(element, newVisibility) {
	if(element.length) {
		// element is an array
		for( var x = 0 ; x < element.length ; x++ ) {
			svShowOrHideElement(element[x], newVisibility);
		}
	} else {
		if (newVisibility == 'visible' && element.style) {
			element.style.visibility = 'visible';
			element.style.display = 'inline';
		} else if(element.style) {
			element.style.visibility = 'hidden';
			element.style.display = 'none';
		}
	}
}

// shows a div tag or element
function svShow(id) {
	svShowOrHide(id, 'visible');
}

// hides a div tag or element
function svHide(id) {
	svShowOrHide(id, 'hidden');
}

// toggles the visibility of a div tag or element
function svToggleVisibility(id) {
	var obj = document.getElementsByTagName("div");
		
	if(!obj[id]) {
		obj = svGetInput(id);
		if(!obj) 
			obj = document.getElementById(id);
		if (obj.length) {
			for(var x=0; x<obj.length; x++) {
				if(obj[x].style.visibility == 'hidden') {
					svShowOrHideElement(obj[x], 'visible');
				} else {
					svShowOrHideElement(obj[x], 'hidden');
				}
			}
		}
		else {
			if(obj.style.visibility == 'hidden') {
				svShowOrHideElement(obj, 'visible');
			} else {
				svShowOrHideElement(obj, 'hidden');
			}
		}
	} else {
		if(obj[id].style.visibility == 'hidden') {
			svShowOrHideElement(obj[id], 'visible');
		} else {
			svShowOrHideElement(obj[id], 'hidden');
		}
	}
}

// gets the value of an input field by name
function svGetInputValue(fieldName, elements) {
	return svGetInputValueForInput(svGetInput(fieldName, elements));
}

// gets the value of an input field by name
function svGetInputValueById(fieldId, elements) {
	return svGetInputValueForInput(svGetElement(fieldId, elements));
}

// gets the value of an input field by name
function svGetInputValueForInput(input) {
		if(!input)
			return null;
		if(input.type && (input.type == 'text' || input.type == 'hidden' || input.type == 'textarea')){
			if(svTrimString(input.value).length == 0)
				return '';
			else
				return input.value;
		}

		if(input.type && input.type == 'select-one') {
				if(input.selectedIndex == -1)
						return null;
				else 
						return input.options[input.selectedIndex].value;
		}

		if(input.type && input.type == 'select-multiple') {
				var select = input;
				var selectedString = "";
				for( var x = 0 ; x < select.options.length ; x++ ) {
						if(select.options[x].selected)
								selectedString += select.options[x].value+",";
				}
				return svTrimCommas(selectedString);
		}

		if(input.type && input.type == 'radio') {
				if(input.checked)
						return input.value;
				return null;
		}

		if(input.type && input.type == 'checkbox') {
				return input.checked;
		}

		if(!input.type && input[0] && input[0].type == 'radio') {
				var radios = input;
				for( var x = 0 ; x < radios.length ; x++ ) {
						if(radios[x].checked)
								return radios[x].value;
				}
				return null;
		}
}

// gets an input field by name
function svGetInput(fieldName, elements) {
		if(!fieldName)
			return null;
		if(elements) {
			var element = elements[fieldName];
			if(element)
				return element;
		}	
		var element = svGetMyForm().elements[fieldName];
		if(!element)
				element = document.getElementsByName(fieldName);
		if(!element)
				element = document.getElementsById(fieldName);
		return element;
}

// gets any element by id
function svGetElement(id, elements) {
	if(!id)
		return null;
	if(elements) {
		var element = elements[id];
		if(element)
			return element;
	}	
		
	var element = document.getElementById(id);
	if(!element)
		element = document.getElementsByName(id);
	return element;
}

// sets the value of an input field by name
function svSetInputValue(fieldName, value, elements) {
		svSetInputValueForInput(svGetInput(fieldName, elements), value);
}

// sets the value of an input field by name
function svSetInputValueById(fieldId, value, elements) {
		svSetInputValueForInput(svGetElement(fieldId, elements), value);
}

// sets the value of an input field 
function svSetInputValueForInput(input, value) {
	if(input.type) {
		var type = input.type;
		if(type == 'text' || type == 'hidden' || type == 'textarea') {
				input.value = value;
				if(type == 'hidden') {
					svSetDynaInputValue(input, value)
				}
				return;
		}

		if(type == 'select-one') {
				for( var x = 0 ; x < input.options.length ; x++ ) {
						if(input.options[x].value == value) {
								input.selectedIndex = x;
								return;
						}
				}
				for( var x = 0 ; x < input.options.length ; x++ ) {
						if(input.options[x].text == value) {
								input.selectedIndex = x;
								return;
						}
				}
		}
		if(type == 'radio' || type == 'checkbox') {
				input.checked = value;
				return;
		}
	}
		
	if(input.length && input[0] && input[0].type == 'radio') {
		for( var x = 0 ; x < input.length ; x++ ) {
			if(input[x].value == value) {
				input[x].checked = true;
				return;
			}
		}
	}
				
}

function svSetDynaInputValue(input, value) {
	svSetDynaDiv(input, value);
	svSetDynaTextInput(input, value);
}

var dynaDivMap;
function getDynaDivMap() {
	if(!dynaDivMap) {
		dynaDivMap = new Object();
		var divs = document.getElementsByTagName("div");
		for( var x = 0 ; x < divs.length ; x++) {
			dynaDivMap[divs[x].id] = divs[x];
		}
	}
	return dynaDivMap;
}

function svSetDynaDiv(input, value) {
	var dynaModeDiv = getDynaDivMap()[input.id+"DynaModeDiv"];
	if(dynaModeDiv) 
		dynaModeDiv.innerHTML = value;
}

function svSetDynaTextInput(input, value) {
	var dynaModeTextInput = svGetElement(input.id+"DynaModeTextInput");
	if(dynaModeTextInput)
		dynaModeTextInput.value = value;
}

// sets a checkbox checked property and synchronizes it with hidden text field
function svSetCheckbox(fieldName, action) {

	// for a checkbox, the fieldName passed in is the hidden field
	// holding the value - when calling the display checkbox field,
	// we need to add the algorithm "_chk" to it
	
	// we had to create a separate function for this, becuase the intent was
	// to give the developers an ability to set the checkbox and hidden field
	// by using the column name - however, since the checkbox name is mangled,
	// providing the column name to the svSetInputValue function would only
	// set the value of the hidden text field - since what we were passing into
	// it mapped to a text field, it never resolved to a checkbox and anything
	// subsection we put in there would never get called
	// the svSetInputValue function was not modified for this function -
	// calling it with a checkbox field name will set the checked property as
	// originally coded
				
	if (action == 'checked') {
	
		// for the checked action, we look up the correct value for this field
		// in the global checkbox hash and then check the HTML checkbox field
		svSetInputValue(fieldName, svCBValuesChecked[fieldName]);
		svSetInputValue(fieldName + "_chk", true);
		
	} else if (action == 'unchecked') {
	
		// for the checked action, we look up the correct value for this field
		// in the global checkbox hash and then check the HTML checkbox field
		svSetInputValue(fieldName, svCBValuesUnchecked[fieldName]);
		svSetInputValue(fieldName + "_chk", false);
		
	} else {
	
		// bomb if we were passed an unknown action
		alert('Unknown value[' + value + '] for checkbox[' + fieldName + ']');
	}
	return;
}

function svSetRequired(fieldName, required, className) {
	var field = svGetInput(fieldName);
	var requiredField = svGetInput(field.id+".required");
	if(field.type && field.type == 'hidden') {
		svSetRequired(field.id+"TextField", required, className);
		svSetRequired(field.id+"DcboBox", required, className);
		return;
	}
	requiredField.value = required;
	if(required) {
		if(!className)
			field.className = "required";
		else 
			field.className = className+"-required";
	} else {
		if(svIsReadOnly(field.id)) {
			if(!className)
				field.className = "readonly";
			else 
				field.className = className+"-readonly";
		} else {
			if(!className)
				field.className = "";
			else 
				field.className = className;
		}
	}
}

function svIsRequired(fieldName) {
	var field = svGetInput(fieldName);
	var requiredField = svGetInput(field.id+".required");
	if(field.type && field.type == 'hidden') {
		requiredField = svGetInput(field.id+"TextField"+".required");
	}
	return requiredField.value;
}

// this sets an input to read only
// Note:  this sets a select to disabled since there is no read only
// so any select should be enabled just before save if the value is expected
// to be in the request parameters
function svSetReadOnly(fieldName, readOnly, className) {
	var field = svGetInput(fieldName);
	if(field.type && field.type == 'hidden') {
		svSetReadOnly(field.id+"TextField", readOnly, className);
		svSetReadOnly(field.id+"DcboBox", readOnly, className);
		return;
	}
	if(readOnly) {
		field.readOnly = true;
		if(field.type == 'select-one' || field.type == 'select-multiple')
			field.disabled = true;
		else {
			if(!svIsRequired(field.id)) {
				if(!className)
					field.className = "readonly";
				else 
					field.className = className+"-readonly";
			}
		}
	} else {
		field.readOnly = false;
		if(field.type == "select-one" || field.type == 'select-multiple')
			field.disabled = false;
		else {
			if(!svIsRequired(field.id)) {
				if(!className)
					field.className = "";
				else 
					field.className = className;
			}
		}
	}
}

function svIsReadOnly(fieldName) {
	var field = svGetInput(fieldName);
	if(field.type && field.type == 'hidden') {
		field = svGetInput(field.id+"TextField");
	}
	if(field.type == 'select-one' || field.type == 'select-multiple')
		return field.disabled;
	return field.readOnly;
}

function svDisableSelects() {
	var elements = svGetMyForm().elements;
	for( var x = 0 ; x < elements.length ; x++ ) {
		if(elements[x].type == 'select-one' || elements[x].type == 'select-multiple')
			elements[x].disabled = true;
	}
}

function svEnableSelects() {
	var elements = svGetMyForm().elements;
	for( var x = 0 ; x < elements.length ; x++ ) {
		if(elements[x].type == 'select-one' || elements[x].type == 'select-multiple')
			elements[x].disabled = false;
	}
}

function svEndsWith(str, match) {
	if(str.indexOf(match) != -1 && str.indexOf(match)+match.length == str.length)
		return true;
	return false;
}

function svGetLastToken(s, delim, numberToGet) {
	var result = "";
	var i = 1;
	if(numberToGet)
		i = numberToGet;
	while(i > 0) {
		if(s.indexOf(delim) == -1)
			result = s;
		else 
			result = s.substring(s.lastIndexOf(delim)+delim.length, s.length) + result;
		i--;
	}
	return result;
}

function svGetFirstToken(s, delim, numberToGet) {
	var result = "";
	var i = 1;
	if(numberToGet)
		i = numberToGet;
	while(i > 0) {
		if(s.indexOf(delim) == -1)
			result = s;
		else 
			result = result + s.substring(0, s.indexOf(delim));
		i--;
	}
	return result;
}

function svRemoveLastToken(s, delim, numberToRemove) {
	var i = 1;
	if(numberToRemove)
		i = numberToRemove;
	while(i > 0) {
		if(s.indexOf(delim) == -1)
			s = s;
		else 
			s = s.substring(0, s.lastIndexOf(delim));
		i--;
	}
	return s;
}

function svRemoveFirstToken(s, delim, numberToRemove) {
	var i = 1;
	if(numberToRemove)
		i = numberToRemove;
	while(i > 0) {
		if(s.indexOf(delim) == -1)
			s = s;
		else 
			s = s.substring(s.indexOf(delim)+delim.length, s.length);
		i--;
	}
	return s;
}

// trims the spaces off a String
function svTrimString(str) {
  while (str.charAt(0) == ' ')
    str = str.substring(1);
  while (str.charAt(str.length - 1) == ' ')
    str = str.substring(0, str.length - 1);
  return str;
}

// trims a specific character off a String
function svTrimCharacter(s, character) {
		while(s && s.indexOf(character) == 0)
				s = s.substring(1, s.length);
		while(s && s.indexOf(character) == s.length)
				s = s.substring(s.length-1, s.length);
		return s;
}

// trims commas off a String
function svTrimCommas(s) {
		return svTrimCharacter(s, ',');
}

// replace a substring
function replace(s, substring, replacement) {
	return s.substring(0, s.indexOf(substring))+replacement
				+s.substring(s.indexOf(substring)+substring.length, s.length);
}

// focuses a field by name
function svInitializeFocus(fieldName) {
		var input = svGetInput(fieldName);
		if(input) {
				input.focus();	
				if(input.type == 'text')
						input.select();
    }
}

var svMyForm;
function svGetMyForm() {
	if(svMyForm == null)
		svMyForm = document.forms[0]; 
	return svMyForm;
}

function svSetMyForm(myForm) {
	svMyForm = myForm;
}

// returns whether any editting has occurred on any fields on this page
function svIsDirty() {
	return svIsPageDirty;
}
var svIsPageDirty = false;
function svSetDirty(isDirty) {
	svIsPageDirty = isDirty;
}

function svSkipFormValidation() {
	svValidateFormOnSubmit = false;
}
function svSkipRequiredFieldsValidation() {
	svValidateRequiredFieldsOnSubmit = false;
}
function svSkipFieldValuesValidation() {
	svValidateFieldValuesOnSubmit = false;
}
var svValidateFormOnSubmit = true;
var svValidateRequiredFieldsOnSubmit = true;
var svValidateFieldValuesOnSubmit = true;
function svValidateForm() {
	if(!svValidateFormOnSubmit)
		return true;
	var form = svGetMyForm();
	var atLeastOneValueEntered = false;
	var validationSuccessful = true;
	for( var x = 0 ; x < form.elements.length ; x++ ) {
		if(svIsValidatedInput(form.elements[x])) {
			if(!svValidateField(x, svGetInput(form.elements[x].name))) {
				svInitializeFocus(form.elements[x].name);
				validationSuccessful = false;
				break;
			}
		}
	}
	return validationSuccessful;
}
function svGetInputBySuffix(suffix) {
	for( var x = 0 ; x < svGetMyForm().elements.length ; x++ ) {
		if(svEndsWith(svGetMyForm().elements[x].name, suffix) || svEndsWith(svGetMyForm().elements[x].id, suffix))
			return svGetMyForm().elements[x];
	}
	return null;
}
function svIsValidatedInput(input) {
	//if(svEndsWith(inputName, "inputText") || svEndsWith(inputName, "inputSelect") || svEndsWith(inputName, "inputSelectHidden")
	//				|| svEndsWith(inputName, "inputDate") || svEndsWith(inputName, "inputCalendar") || svEndsWith(inputName, "inputNumeric"))
	if(input.type && (input.type == "text" || input.type == "select-one" || input.type == 'select-multiple' || input.type == "date"))
		return true;
	return false;
}
function svValidateField(index, input) {
	var inputId = input.id;
	var inputValue = svGetInputValue(inputId);
	var inputType = svGetInputValue(inputId+".type");
	if(!inputType) {
		// field isn't validated
		return true;
	}
	var inputTitle = svGetInputValue(inputId+".title");
	var inputRequired = svGetInputValue(inputId+".required");
	var inputRegex = svGetInputValue(inputId+".regex");
	var inputRegexValidationText = svGetInputValue(inputId+".regexValidationText");
	var inputValidationMethod = svGetInputValue(inputId+".validationMethod");
	if(!svValidateValue(input, inputValue, inputType, inputTitle, inputRequired, inputRegex, 
				inputRegexValidationText, inputValidationMethod)) {
		return false;
	}
	return true;
}
function svValidateValue(input, value, type, title, required, regex, regexValidationText, validationMethod) {
	if(input.disabled) 
		return true;
		
	// multiple selects don't have a "value", so we must handle them differently
	if(required == "true" && type == 'select-one' && input.id.substring(0,13) == 'dualListRight' && input.options.length == 0) {
		alert(title+" is required and has no selected values.");
		return false;
	} else {
		if(required == "true" && (value == null || svTrimString(value) == "") 
					&& svValidateRequiredFieldsOnSubmit && input.id.substring(0,8) != 'dualList') {
			alert(title+" is required.");
			return false;
		}
	}
	if(!value || !svValidateFieldValuesOnSubmit)
		return true;
	if(regex &&	value.search(regex)) {
		//value can be blank unless mandatory which is caught by above logic
		if(value == "") {
			return true;
		} else {
			if(regexValidationText)
				alert(regexValidationText);
			else 
				alert(title+" must match the assigned Regular Expression ("+regex+") (offending value: "+value+")");
			return false;
		}
	}
	if(validationMethod && !eval(validationMethod))
		return false;
	if(type == "text")
		if(!svValidateText(value, title))
			return false;
	if(type == "numeric")
		if(!svValidateNumber(value, title))
			return false;
	if(type == "date")
		if(!svValidateDate(value, title))
			return false;
	return true;
}
function svValidateDate(value, title) {
	//checks date format MM/DD/YYYY or MM/DD/YY
	if(value.search(/^\d{1,2}\/\d{1,2}\/(\d{2}|\d{4})$/)) {
		//date can be blank unless mandatory which is caught by above logic
		if(value == "") {
			return true;
		} else {
			alert(title+" must be of the format MM/DD/YY (offending value: "+value+")");
			return false;
		}
	}
	//only check date ranges if we have a date
	if(value != "") {
		//let's check most common date ranges but not all
		//1-12 for months and 1-31 for days
		var date = value.split("/");
		if(date[0] < 1 || date[0] > 12) {
			alert(title+" must contain a month between 1 and 12 (offending value: "+date[0]+")");
			return false;
		}
		if(date[1] < 1 || date[1] > 31) {
			alert(title+" must contain a day between 1 and 31 (offending value: "+date[1]+")");
			return false;
		}
	}
	return true;
}
function svValidateDateTime(value, title) {
	//checks date format MM/DD/YYYY HH:mm or MM/DD/YY HH:mm
	if(value.search("/^\d{1,2}[/]\d{1,2}[/](\d{2}|\d{4})[ ]\d{1,2}[:]\d{1,2}$/")) {
		//date can be blank unless mandatory which is caught by above logic
		if(value == "") {
			return true;
		} else {
			alert(title+" must be of the format MM/DD/YYYY HH:mm (offending value: "+value+")");
			return false;
		}
	}
	//only check date ranges if we have a date
	if(value != "") {
		//let's check most common date ranges but not all
		//1-12 for months and 1-31 for days
		var date = value.split(" ")[0].split("/");
		var time = value.split(" ")[1].split(":");
		if(date[0] < 1 || date[0] > 12) {
			alert(title+" must contain a month between 1 and 12 (offending value: "+date[0]+")");
			return false;
		}
		if(date[1] < 1 || date[1] > 31) {
			alert(title+" must contain a day between 1 and 31 (offending value: "+date[1]+")");
			return false;
		}
		if(!date[2]) {
			alert(title+" must contain a year");
			return false;
		}
		if(time[0] < 0 || time[0] > 23) {
			alert(title+" must contain a month between 0 and 23 (offending value: "+time[0]+")");
			return false;
		}
		if(time[1] < 0 || time[1] > 59) {
			alert(title+" must contain a day between 0 and 59 (offending value: "+time[1]+")");
			return false;
		}
	}
	return true;
}
function svValidateNumber(value, title) {
	if(isNaN(value)) {
		alert(title+" must be a valid number (offending value: "+value+")");
		return false;
	}
	return true;
}
function svValidateText(value, title) {
	return true;
}
function svClearInputs() {
	var form = svGetMyForm();
	for( var x = 0 ; x < form.elements.length ; x++ ) {
		if(svIsValidatedInput(form.elements[x]))
			svSetInputValue(form.elements[x].name, "");
	}
}
function svDisableButtons() {
	var elements = svGetMyForm().elements;
	for( var x = 0 ; x < elements.length ; x++ ) {
		if(elements[x].type == 'button')
			elements[x].disabled = true;
	}
} 
function svEnableButtons() {
	var elements = svGetMyForm().elements;
	for( var x = 0 ; x < elements.length ; x++ ) {
		if(elements[x].type == 'button')
			elements[x].disabled = false;
	}
}
function svGetFirstInputName() {
	var form = svGetMyForm();
	var atLeastOneValueEntered = false;
	for( var x = 0 ; x < form.elements.length ; x++ ) {
		if(svIsValidatedInput(form.elements[x]) && form.elements[x].disabled == false)
			return form.elements[x].name;
	}
}
function svFocusFirstField() {
	svInitializeFocus(svGetFirstInputName());
}
function doNothing() {
}
var log;

// generic function to determine the name of the frame a page has been loaded
function svDetermineFrameContainerName() {
	var id;
	
	for (var i=0; i < parent.frames.length; i++) {
		if (parent.frames[i].location.href == self.location.href) {
			id = parent.frames[i].name;
		}
	}
	return id;
}

// generic function to resize an iframe based upon its content's height
function svResizeIframeByContentHeight(id) {
	var myIframe = document.getElementById(id);
	if (myIframe) {
		if (myIframe.contentDocument && myIframe.contentDocument.body.offsetHeight) {
			myIframe.height = myIframe.contentDocument.body.offsetHeight + 100;
		} else if (myIframe.Document && myIframe.Document.body.scrollHeight) {
			myIframe.height = myIframe.Document.body.scrollHeight + 100;
		}
	}
}

// function to be used within a JS sort command to compare two values
function svCompareValues(a, b) { 
  var sA = parseInt( a.value, 36 );  
  var sB = parseInt( b.value, 36 );  
  return sA - sB;
}

// function to move values between two lists - a flag denotes whether the entire
// list is to be moved a one time
function svMoveDualList( srcList, destList, moveAll ) {

	// do nothing if nothing is selected
	if ((srcList.selectedIndex == -1 ) && (moveAll == false)) {
		return;
	}

	newDestList = new Array(destList.options.length);
	var len = 0;

	for(len = 0; len < destList.options.length; len++) {
		if (destList.options[ len ] != null) {
			newDestList[ len ] = new Option( destList.options[ len ].text, destList.options[ len ].value, destList.options[ len ].defaultSelected, destList.options[ len ].selected );
		}
	}

	for(var i = 0; i < srcList.options.length; i++) { 
		if (srcList.options[i] != null && (srcList.options[i].selected == true || moveAll)) {

			// incorporate into new list
			newDestList[ len ] = new Option( srcList.options[i].text, srcList.options[i].value, srcList.options[i].defaultSelected, srcList.options[i].selected );
			len++;
		}
	}

	// sort out the new destination list
	newDestList.sort(svCompareValues);
	
	// populate the destination with the items from the new array
	for (var j = 0; j < newDestList.length; j++) {
		if ( newDestList[ j ] != null ) {
			destList.options[ j ] = newDestList[ j ];
		}
	}

	// erase source list selected elements
	for (var i = srcList.options.length - 1; i >= 0; i--) { 
		if (srcList.options[i] != null && (srcList.options[i].selected == true || moveAll)) {
			srcList.options[i] = null;
		}
	}
	
	// deselect all elements in both lists
	srcList.selectedIndex = -1;
	destList.selectedIndex = -1;
}

// function to take the contents of a list and convert them to a single separated string
// stored in a field
function convertMultiSelectToField(srcList, fld, separator) {
  if (separator == null)
    separator = "";

  fld.value = "";
  for( var i = 0; i < srcList.options.length; i++ ) { 
    fld.value = fld.value + separator + srcList.options[i].value;
  }

  // if separator is not null and not a "no space", remove it from the front of the field
  if (separator != "") {
    fld.value = fld.value.substring(1, fld.value.length);
  }
}

// function to handle any nuances that are needed to perform to get the form
// ready for submission
var multiSelectIdArray = new Array();
function svPrepareFormForSubmit() {
	for( var x = 0 ; x < multiSelectIdArray.length ; x++ ) {
		var multiSelect = svGetElement(multiSelectIdArray[x]);
 
		// Auto-select all entries in any "right-side" dual-multi select boxes
		if (multiSelect.id.substring(0, 14) == 'dualListRight_' && multiSelect.type.substring(0, 6) == 'select') {
			var selObj = document.getElementById(multiSelect.id);
			for (var i=0; i < selObj.options.length; i++) {
				selObj.options[i].selected = true;
			}		
		}		
	}
}

// function to handle hightlighting the selected tab - requires an index of the current tab and
// the number of total tabs
function svHighlightCurrentTab(curr, tot, randId) {

	// turn current tab on
	document.getElementById("tab" + curr + "BG" + randId).className = "tabBG_selected";
	document.getElementById("tab" + curr + "Top" + randId).className = "tabTop_selected";
	document.getElementById("tab" + curr + "Content" + randId).className = "tabContent_selected";
	document.getElementById("tab" + curr + "Link" + randId).className = "tabLink_selected";

	// turn all other tabs off
	for (var i = 0; i < tot; i++) {
		if (i != curr) {
			document.getElementById("tab" + i + "BG" + randId).className = "tabBG";
			document.getElementById("tab" + i + "Top" + randId).className = "tabTop";
			document.getElementById("tab" + i + "Content" + randId).className = "tabContent";
			document.getElementById("tab" + i + "Link" + randId).className = "tabLink";
		}
	}
}
		
// this variable keeps track of the selected tab
var svSelectedTabId;
// this function selects a tab in a tabTable and de-selects the active one
function svSelectTab(tabId, selectedTabClass, unselectedTabClass, target, url) {
	svGetElement(svSelectedTabId+"Link").className = unselectedTabClass;
	svGetElement(svSelectedTabId+"Tab").className = unselectedTabClass;
	if(svGetElement(svSelectedTabId+"Left"))
		svGetElement(svSelectedTabId+"Left").className = unselectedTabClass;
	if(svGetElement(svSelectedTabId+"Right"))
		svGetElement(svSelectedTabId+"Right").className = unselectedTabClass;
		
	svGetElement(tabId+"Link").className = selectedTabClass;
	svGetElement(tabId+"Tab").className = selectedTabClass;
	if(svGetElement(tabId+"Left"))
		svGetElement(tabId+"Left").className = selectedTabClass;
	if(svGetElement(tabId+"Right"))
		svGetElement(tabId+"Right").className = selectedTabClass;
	if(target)
		svGetElement(target).src = url;
	
	svSelectedTabId = tabId;
}
function svEnableTab(tabId, enable) {
	svSetVisible("Disabled"+tabId+"Tab", !enable);
	svSetVisible(tabId+"Tab", enable);
}

// this function is specific to dynaModeInput fields and sets the mode
// currently only 'view' and 'edit' are supported
function svSetDynaMode(fieldId, mode) {
	if(mode == "view") {
		svShow(fieldId+"DynaModeDiv");
		svHide(fieldId+"DynaModeTextInput");
	} else if(mode == "edit") {
		svHide(fieldId+"DynaModeDiv");
		svShow(fieldId+"DynaModeTextInput");
	}
}

// this function is specific to dynaModeInput fields and sets the mode
// currently only 'view' and 'edit' are supported
function svSetDynaModeArray(inputArray, mode) {
	for( var x = 0 ; x < inputArray.length ; x++)
		if(inputArray[x] && inputArray[x].id) 
			svSetDynaMode(inputArray[x].id, mode);
}

// this function is specific to dynaModeInput fields and sets the mode
// currently only 'view' and 'edit' are supported
function svSetDynaModeMatching(stringToMatch, mode) {
	var allDivs = document.getElementsByTagName("DIV");
	for (var i = 0 ; i < allDivs.length ; i++ ) { 
		if (allDivs[i].id.match(stringToMatch)) {
			if(mode == "view") {
				svShow(allDivs[i].id);
			} else if(mode == "edit") {
				svHide(allDivs[i].id);
			}
		}
	}
	var allInputs = document.getElementsByTagName("INPUT");
	for (var i = 0 ; i < allInputs.length ; i++ ) {
		if (allInputs[i].type != "hidden" && allInputs[i].id.match(stringToMatch)) {
			if(mode == "view") {
				svHide(allInputs[i].id);
			} else if(mode == "edit") {
				svShow(allInputs[i].id);
			}
		}
	}
}

// this function gets all the tags with id that match the string passed in
function svGetMatchingElements(stringToMatch, tagName, type) {
	var array = new Array();
	var index = 0;
	var allTags = document.getElementsByTagName(tagName);
	for (var i = 0 ; i < allTags.length ; i++ ) 
		if (allTags[i].id.match(stringToMatch)) 
			if(!type || (type && allTags[i].type && allTags[i].type == type))
				array[index++] = allTags[i];
	return array;
}

// function to initialize an AJAX request
var svBrowsIsIE;
function svInitRequest(url) {
    if (window.XMLHttpRequest) {
        return new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        svBrowsIsIE = true;
        return new ActiveXObject("Microsoft.XMLHTTP");
    }
}

// create a global array to store checkbox checked/unchecked values
var svCBValuesChecked = new Object;
var svCBValuesUnchecked = new Object;

// the following code simulates the insertAdjacentHtml functionality found in IE in non-IE browsers
// any page that uses this .js file will automatically be able to use the 3 insert methods
if(typeof HTMLElement!="undefined" && !HTMLElement.prototype.insertAdjacentElement) {
	HTMLElement.prototype.insertAdjacentElement = function(where,parsedNode) {
		switch (where){
		case 'beforeBegin':
			this.parentNode.insertBefore(parsedNode,this)
			break;
		case 'afterBegin':
			this.insertBefore(parsedNode,this.firstChild);
			break;
		case 'beforeEnd':
			this.appendChild(parsedNode);
			break;
		case 'afterEnd':
			if (this.nextSibling) 
				this.parentNode.insertBefore(parsedNode,this.nextSibling);
			else 
				this.parentNode.appendChild(parsedNode);
			break;
		}
	}

	HTMLElement.prototype.insertAdjacentHTML = function(where,htmlStr) {
		var r = this.ownerDocument.createRange();
		r.setStartBefore(this);
		var parsedHTML = r.createContextualFragment(htmlStr);
		this.insertAdjacentElement(where,parsedHTML)
	}


	HTMLElement.prototype.insertAdjacentText = function(where,txtStr) {
		var parsedText = document.createTextNode(txtStr)
		this.insertAdjacentElement(where,parsedText)
	}
}

// creates a div on the fly
function svNewDiv(e, id, innerHTML, styleClass, absolute, backgroundColor) {
	var eDiv = document.createElement("DIV");
	eDiv.id = id;
	if(!(!innerHTML || innerHTML == null)) {
		eDiv.innerHTML = "<table cellspacing='0' cellpadding='1' "
					+"style='border: black 1px solid; BACKGROUND-COLOR: white;' id='"
					+id+"_table'><tr><td nowrap style='PADDING-LEFT: 10px; PADDING-RIGHT: 10px'>"+innerHTML+"</td></tr></table>";
	}
	document.body.appendChild(eDiv);
	var table = svGetElement(id+"_table");
	eDiv.style.width = xWidth(table);
	eDiv.style.height = xHeight(table);
	eDiv.style.left = xPageX(e)+(xWidth(e)/2)-(xWidth(eDiv)/2);
	eDiv.style.top = xPageY(e)-(xHeight(table)+3);
	if(!absolute || absolute == null)
		eDiv.style.position = "absolute";
	else
		eDiv.style.position = absolute;
	if(!(!backgroundColor || backgroundColor == null))
		eDiv.style.backgroundColor = backgroundColor;
	return eDiv;
}

function svShowInfo(e, info) {
	svNewDiv(e, e.id+"_svInfoDiv", info);
}

function svHideInfo(e, info) {
	document.body.removeChild(svGetElement(e.id+"_svInfoDiv"));
}