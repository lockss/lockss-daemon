/*
 * $Id$
 */

/*

Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

function submitForm(name) {
 var form = document.getElementById(name);
 if(form && form.onsubmit && !form.onsubmit()) return;
 form.submit();
}

function lockssSetElem(form, name, value) {
 var elem = findNamedElem(form, name);
 if (elem != null) elem.value = value;
}

function disableAllButtons() {
  var inputs = document.getElementsByTagName("INPUT");
  for (var i = 0; i < inputs.length; i++) {
    if (inputs[i].type === 'submit' || inputs[i].type === 'button') {
      inputs[i].disabled = true;
    }
  }
}

function lockssButton(element, action, name, val) {
 var form = element.form;
 if (name !== undefined && val !== undefined) {
  lockssSetElem(form, name, val);
 }
  disableAllButtons()
  element.value='Working...';
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

function removeElementId(id)	{
 var node = document.getElementById(id);
 var parent=node.parentNode;
 parent.removeChild(node);
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

function selectEnable(elem, id1, id2) {
 var enable = elem.checked;
 var el1 = document.getElementById(id1);
 if (el1 !== undefined) {
  el1.disabled = !enable;
 }
 var el2 = document.getElementById(id2);
 if (el2 !== undefined) {
  el2.disabled = !enable;
 }
}

lastAuClicked = null;

function selectAll(form, op) {
 selectAll(form, op, null, null);
}

function selectAll(form, op, first, last) {
 initRepoMap(form);
 for (var i=0;i < form.length;i++) {
  var elem = form.elements[i];
  if (elem.className == 'doall') {
   if (first != null && elem.tabIndex < first.tabIndex) {
    continue;
   }
   if (last != null && elem.type == 'checkbox' &&
       elem.tabIndex > last.tabIndex) {
    break;
   }
   if (op == 'clear' && (elem.type == 'checkbox' ||
			 elem.type == 'radio' && !elem.defaultChecked)) { 
    elem.checked = false;
   } else if (op == 'all' && elem.type == 'checkbox') { 
    if (!elem.checked) {
     elem.click();	       // click() runs onclick fn, which we rely on
    }
   } else if (op == 'inRepo' && elem.type == 'checkbox') {
    var repobuttons = buttonMap[elem.value];
    if (repobuttons != null && repobuttons.length == 1) {
     var button = repobuttons[0];
     if (button.defaultChecked && !elem.checked) {
      elem.click();
     }
    }
   }
  }
 }
}

function clickAu(clickEvent, checkbox, form) {
 if (clickEvent.shiftKey && lastAuClicked != null) {
  if (checkbox.tabIndex < lastAuClicked.tabIndex) {
   selectAll(form, "all", checkbox, lastAuClicked);
  } else {
   selectAll(form, "all", lastAuClicked, checkbox);
  }
 }
 // must do this even if called selectAll; it won't have selected this AU's
 // repo because the checkbox is already checked when it gets there.
 if (checkbox.checked) {
  selectRepo(checkbox, form);
 }
 window.lastAuClicked = checkbox;
}

function selectRepo(checkbox, form) {
 initRepoMap(form);
 var repobuttons = buttonMap[checkbox.value];
 if (repobuttons == null || repobuttons.length < 2) return;
 for (var ix = 0; ix < repobuttons.length; ix++) {
  var rb = repobuttons[ix];
  if (rb.checked) return;
 }
 var nextrepo = nextDefault();
 for (var ix = 0; ix < repobuttons.length; ix++) {
  var rb = repobuttons[ix];
  if (rb.value == nextrepo) {
   rb.checked = true;
   return;
  }
 }
}

function nextDefault() {
 if (repoSelections == null) {
  return "1";
 }
 if (nextDefaultIx >= repoSelections.length) {
  nextDefaultIx = 0;
 }
 var next = repoSelections[nextDefaultIx++];
 return next;
}

buttonMap = null;
repoSelections = null;

function addToMapVal(key, val) {
 var arr = buttonMap[key];
 if (arr == null) {
  arr = new Array();
  buttonMap[key] = arr;
 }
 arr[arr.length] = val;
} 

function resetRepoSelections(){
 buttonMap = null;
}

function initRepoMap(form) {
 if (buttonMap != null) return;
 nextDefaultIx = 0;
 buttonMap = new Array();
 var pattern = /^Repository_(.+)$/;
 for (var ix = 0; ix < form.elements.length; ix++) {
  var elem = form.elements[ix];
  if (elem.name == "DefaultRepository") {
   if (elem.checked) {
    addToMapVal("DefaultRepository", elem.value);
   }
  } else {
   var result = elem.name.match(pattern);
   if (result != null) {
    addToMapVal(result[1], elem);
   }
  }
 }
 repoSelections = buttonMap["DefaultRepository"];
}

// Toggle the visibility of two elements, so that one is visible while the other
// is hidden.
function toggleElements(id1, id2) {
  var el1 = document.getElementById(id1);
  var el2 = document.getElementById(id2);
  if (el1.style.display == "none") {
    el1.style.display = "block";
	el2.style.display = "none";
  } else {
	el1.style.display = "none";
    el2.style.display = "block";
  }
}

// Toggles the ability of two subscription slave elements through the selection
// of a master publication subscription tribox element.
function publicationSubscriptionChanged(pubSubId, subscribedRangesId,
		unsubscribedRangesId) {
	// The publication subscription tribox.
	var pubSub = document.getElementById(pubSubId);

	// The subscribed ranges input box.
	var subscribedRanges = document.getElementById(subscribedRangesId);

	// The unsubscribed ranges input box.
	var unsubscribedRanges = document.getElementById(unsubscribedRangesId);

	// Check whether the publication subscription is changing to "Not Set".
	if (pubSub.value == "false") {
		// Yes: Enable both ranges.
		if (subscribedRanges !== undefined) {
			subscribedRanges.disabled = false;
		}
		if (unsubscribedRanges !== undefined) {
			unsubscribedRanges.disabled = false;
		}
		return;
	}

	// No: Disable the subscribed ranges.
	if (subscribedRanges !== undefined) {
		subscribedRanges.disabled = true;
	}

	// Check whether the publication subscription is changing to "Unsubscribe
	// All".
	if (pubSub.value == "true") {
		// Yes: Disable the unsubscribed ranges.
		if (unsubscribedRanges !== undefined) {
			unsubscribedRanges.disabled = true;
		}
	} else {
		// No: Enable the unsubscribed ranges.
		if (unsubscribedRanges !== undefined) {
			unsubscribedRanges.disabled = false;
		}
	}
}

// Sets the parameters passed from the MetadataMonitor forms to the
// MetadataControl servlet.
function setMonCtrlParams(id1, value1, id2, value2, id3, value3) {
	if (id1 != null) {
		var param = document.getElementById(id1);

		if (param != null) {
			param.value = value1;
		}
	}

	if (id2 != null) {
		var param = document.getElementById(id2);

		if (param != null) {
			param.value = value2;
		}
	}

	if (id3 != null) {
		var param = document.getElementById(id3);

		if (param != null) {
			param.value = value3;
		}
	}
}
