function lockssSetElem(form, name, value) {
 elem = findNamedElem(form, name);
 if (elem != null) elem.value = value;
}

function lockssButton(element, action, name, val) {
 var form = element.form;
 if (name !== undefined && val !== undefined) {
  lockssSetElem(form, name, val);
 }
 form.lockssAction.value = action;
 form.submit();
}

function findNamedElem(form, name) {
 for (var ix = 0; ix < form.elements.length; ix++) {
  if (form.elements[ix].name == name) {
   return form.elements[ix];
  }
 }
 return null;
}

function findNamedElems(form, name) {
 var res = new Array();
 for (var ix = 0; ix < form.elements.length; ix++) {
  if (form.elements[ix].name == name) {
   res[res.length] = form.elements[ix];
  }
 }
 return res;
}

function radioButtonValue(name) {
 var buttons = buttonMap[name];
 for (var ix = 0; ix < buttons.length; ix++) {
  var b = buttons[ix];
  if (b.name == name && b.checked) return b.value;
 }
 return null;
}

function cascadeSelectEnable(elem, nextId) {
 var elemVal = elem.options[elem.selectedIndex].value;
 var enable = (elemVal == "" && !elem.disabled);
 var nextEl = document.getElementById(nextId);
 if (nextEl !== undefined) {
  nextEl.disabled = !enable;
  if (nextEl.onchange !== undefined && nextEl.onchange != null) {
   nextEl.onchange();
  }
 }
}

function selectAll(form, clear) {
 for (var i=0;i < form.length;i++) {
  elem = form.elements[i];
  if (elem.type == 'checkbox') { 
   if (clear) {
    elem.checked = false;
   } else if (!elem.checked) {
//    elem.checked = true; 
    elem.click();
   }
  }
 }
}

function selectRepo(checkbox, form) {
 initRepoMap(form);
 repobuttons = buttonMap[checkbox.value];
 if (repobuttons == null || repobuttons.length < 2) return;
 var defval = radioButtonValue("DefaultRepository");
 var defrb = null;
 for (var ix = 0; ix < repobuttons.length; ix++) {
  var rb = repobuttons[ix];
  if (rb.checked) return;
  if (rb.value == defval) defrb = rb;
 }
 if (defrb != null) defrb.checked = true;
}

buttonMap = null;

function addToMapVal(key, val) {
 var arr = buttonMap[key];
 if (arr == null) {
  arr = new Array();
  buttonMap[key] = arr;
 }
 arr[arr.length] = val;
} 

function initRepoMap(form) {
 if (buttonMap != null) return;
 buttonMap = new Array();
 var pattern = /^Repository_(.+)$/;
 for (var ix = 0; ix < form.elements.length; ix++) {
  var elem = form.elements[ix];
  if (elem.name == "DefaultRepository") {
   addToMapVal(elem.name, elem);
  } else {
   var result = elem.name.match(pattern);
   if (result != null) {
    addToMapVal(result[1], elem);
   }
  }
 }
}

