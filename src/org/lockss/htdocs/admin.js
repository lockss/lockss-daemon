function lockssSetElem(form, name, value) {
 for (var ix = 0; ix < form.elements.length; ix++) {
  if (form.elements[ix].name == name) {
   form.elements[ix].value = value;
   return;
  }
 }
}

function lockssButton(element, action, name, val) {
 var form = element.form;
 if (name !== undefined && val !== undefined) {
  lockssSetElem(form, name, val);
 }
 form.lockssAction.value = action;
 form.submit();
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
function selectAll(form, invert) {
 for (var i=0;i < form.length;i++) {
  elem = form.elements[i];
  if (elem.type == 'checkbox') { 
   if (invert)
    elem.checked = !elem.checked;
   else elem.checked = true; 
  }
 }
}
