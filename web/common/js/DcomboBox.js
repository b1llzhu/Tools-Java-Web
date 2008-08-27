// DcomboBox.js
// Copywrite 2004 2005 Martin Krolik (martin@krolik.net)
// All rights reserved.  
var arrAllComboBoxes = new Array();
var pOldDComboBoxOnloadHandler = null;
var pOldDComboBoxResizeHandler = null;
pOldDComboBoxOnloadHandler = window.onload;
pOldDComboBoxResizeHandler = window.onresize;
onload = DComboBoxOnloadHandler;
onresize = DComboBoxResizeHandler;
var debug = true;
function ComboInit(BaseName) {
	var objA, objB, d2, offset;
	offset = 0;
	yoffset = 0;
	if (navigator.appName.indexOf("Mozilla")>=0) offset = -1;
	objA = xGetElementById(BaseName + "TextField");
	objB = xGetElementById(BaseName + "DcboBox");
	d2 = xGetElementById("d2");
	if(d2) yoffset = findPosY(d2);
	// theodore.elrick 1/24/07
	// xClip doesn't work properly if the object goes off the edge of the browser so we move it 
	// to the most likely place where it will not go off the edge first
	xMoveTo(objB, 0, 0);	
	// theodore.elrick 1/24/07
	// added this part to allow for the field to adapt to the natural size of the select
	// I also added this part to allow for a minimum size so that user entered values aren't unreadable
	if(objB.clientWidth < 100)
		objB.style.width = 100;
	else
		objB.style.width = objB.clientWidth+5;
	if(objB.clientWidth)
		objA.style.width = objB.clientWidth-16;
	xClip(objB , 0, xWidth(objB), xHeight(objB), xWidth(objB) - 18);
	xShow(objB);
	xMoveTo(objB, xPageX(objA) + offset, xPageY(objA) - yoffset );	
	UnSelectAnyOptions(objB);
}
function AllComboInit() {
  for (var i = 0; i < arrAllComboBoxes.length; i++) 
		ComboInit(arrAllComboBoxes[i]);
}
function UpdateComboBoxPosition(BaseName) {
	var objA, objB, d2, offset;
	offset = 0;
	yoffset = 0;
	if (navigator.appName.indexOf("Mozilla")>=0) offset = -1;
	objA = xGetElementById(BaseName + "TextField");
	objB = xGetElementById(BaseName + "DcboBox");
	d2 = xGetElementById("d2");
	if(d2) yoffset = findPosY(d2);
	//xMoveTo(objB, 0, 0);	
	xMoveTo(objB, xPageX(objA) + offset, xPageY(objA) - yoffset );	
}
function UpdateAllComboBoxPositions() {
  for (var i = 0; i < arrAllComboBoxes.length; i++) 
		UpdateComboBoxPosition(arrAllComboBoxes[i]);
}
function DComboBoxOnloadHandler() {
  if (pOldDComboBoxOnloadHandler) pOldDComboBoxOnloadHandler();
  AllComboInit();
}
function DComboBoxResizeHandler() {
  if (pOldDComboBoxResizeHandler) pOldDComboBoxResizeHandler();
  UpdateAllComboBoxPositions();
}

// added 6/5/2006 dmoorhem (Savvis) 
// returns index of select option.text = value 
function getIndex(obj, value) {
	var opts = obj.options;
	for(var i=0; i < opts.length; i++) {
		if (opts[i].text == value) {
			return i;
		}
	}
	return -1;
}

// added 6/5/2006 dmoorhem (Savvis) 
// onkeydown checks for up or down arrow
// cycles through select box options
function CycleSelect(evt) {
	var objA, objB, strName;
	var objEvent = new xEvent(evt);
	var code = evt.keyCode;	
	var dir = 1;
	if(code == 40) { dir = 1; }else if (code == 38) { dir = -1; }else{ return; }
	objA = objEvent.target;
	strName = objA.id;	
	strName = strName.split("TextField")[0];
	objB = xGetElementById(strName + "DcboBox");
	var ind = getIndex(objB, objA.value);
	if(ind + dir >= objB.options.length) return;
	if(ind + dir < 0) return;
	objA.value = objB.options[ind + dir].text;
	UnSelectAnyOptions(objB);
	//if ((! document.all) && objA && (objA.onchange)) objA.onchange();
	if (objA && (objA.onchange)) objA.onchange();
}

function UpdateDCBoxGeneric(evt) {
	var objA, objB, strName;
	var objEvent = new xEvent(evt);
	objB = objEvent.target;
	strName = objB.id;	
	strName = strName.split("DcboBox")[0];
	objA = xGetElementById(strName + "TextField");
	objA.focus();
	objA.value = objB.options[objB.selectedIndex].text;
	var value = objB.options[objB.selectedIndex].value;
	//if(!value)
	//	value = objB.options[objB.selectedIndex].text;
	UpdateHiddenInput(strName, value);
	UnSelectAnyOptions(objB);
	//if ((! document.all) && objA && (objA.onchange)) objA.onchange();
	if (objA && (objA.onchange)) objA.onchange();
}

function SetDCBoxByValue(value, select, textField) {
	for( var x = 0 ; x < select.options.length ; x++ ) {
		if(select.options[x].value == value) {
			select.options[x].selected = true;
			textField.value = select.options[x].text;
		}
	}
}
function UnSelectAnyOptions(selectElement) {
	// this unselects any records
	for (var n = 0; n < selectElement.options.length; n++)
	{
		selectElement.options[n].selected = false;
	}
	if (selectElement.options.length > 0)  // this is for NS4 on X-Windows
	{
		if (document.layers) 
		{  
			// this is for NS4 on X-Windows
			selectElement.options[0].selected = true;
		}
		selectElement.options[0].selected = false;
	}
	selectElement.selectedIndex = -1;
	selectElement.value = null;
}
function UpdateInputSelectHiddenField(textInput) {
	svSetInputValue(textInput.id.split("TextField")[0], svGetInputValue(textInput.id));
}
function UpdateHiddenInput(hiddenInputName, value) {
	if(hiddenInputName.indexOf('DcboBox') != -1) {
		svSetInputValue(xGetElementById(hiddenInputName).id.split("DcboBox")[0], value);
	} else {
		svSetInputValue(xGetElementById(hiddenInputName).id, value);
	}
}
function BuildDCBox(BaseName, StartValue, Options, PixelWidth, Other, SelectOther, AdditionalStyle, OptionValues, id) {
	if(!id)
		id = BaseName;
	var DCOptions;
	var DCOptionValues;
//	if (!(PixelWidth)) PixelWidth = 200;
	if (!(StartValue)) StartValue = "";
	if ((typeof Options) == "string")  { DCOptions = Options.split("|") } else { DCOptions = Options }
	if (OptionValues) {
		if ((typeof OptionValues) == "string")  { DCOptionValues = OptionValues.split("|") } else { DCOptionValues = OptionValues }
	}
	var strBuild = "";
	if (document.layers)
		strBuild += "<table border=0><tr><td align=center>";
	strBuild += "<input type='hidden' name='"+BaseName+"' value='"+StartValue+"' id='" + id + "'>";
	strBuild += "<input type='text' value='"+StartValue;
	strBuild += "' name='" + BaseName + "TextField" + "' id='" + id + "TextField" + "' "
	strBuild += " onkeydown='CycleSelect(event);' "
	if (!(document.layers)) {
		strBuild += "style='";
		if (PixelWidth)
			strBuild += "width: " + (PixelWidth - 19) + "px; ";
		strBuild += "position: relative; " + AdditionalStyle + "' ";
	}
	strBuild += Other + " />"
	if (document.layers)
		strBuild += "</td></tr><tr><td align=center>- Or Select -</td></tr><tr><td align=center>";
	else
		strBuild += "<img border='0' height='1' width='15' />";
	strBuild += "<select tabindex=-1 name='" + BaseName + "DcboBox' id='" + id ;
	strBuild += "DcboBox' "
	if (!(document.layers)) {
		strBuild += "style='";
		if (PixelWidth)
			strBuild += "width: " + PixelWidth + "px; ";
		strBuild += "position: absolute; visibility: hidden; " + AdditionalStyle + "' ";
	}
	strBuild += "readonly='true' " ;
	strBuild += SelectOther + " >"
	if ((DCOptions) && (DCOptions.length)) {
		for (var cnt = 0; cnt < DCOptions.length; cnt++ ) {
			if(DCOptionValues) {
				strBuild += "<option value='"+DCOptionValues[cnt]+"'>" + DCOptions[cnt] + "</option>";
			} else {
				strBuild += "<option>" + DCOptions[cnt] + "</option>";
			}
		}
	}
	strBuild += "</select>";
	if (document.layers)
		strBuild += "</td></tr></table>";

	return (strBuild);

}
function WriteDCBox(BaseName, StartValue, Options, PixelWidth, Other, SelectOther, AdditionalStyle, OptionValues, id) {
	if (!(BaseName)) BaseName = "DefaultDComboBoxName";
	if(!id) id = BaseName;
	document.write(BuildDCBox(BaseName,StartValue,Options,PixelWidth,Other,SelectOther,AdditionalStyle,OptionValues, id));
	setTimeout("ComboInit('" + id + "');", 350);
	arrAllComboBoxes.push(id);
}

