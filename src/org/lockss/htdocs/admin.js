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
