// Communication between the visible tristate widgets and their corresponding
// hidden input widgets.
//
// The hidden input widget is needed to communicate with the server the state of
// the tristate widget, because the tristate widget is seen by the server as a
// plain two-state check box.
//
// This code is applied to all the objects of class "tribox"
var triBox = $('.tribox').tristate({
	// To be run on tristate widget initialization (page load).
    init: function(state, value) {
      // Get the hidden input widget.
      var hidden = document.getElementById(this.attr('id') + "Hidden");
      // Get the text widget.
      var textSpan = document.getElementById(this.attr('id') + "Text");
      // Initialize the tristate widget and its text representation according to
      // the initial value of the hidden input widget.
      if (hidden.value === "unset") {
        this.tristate('state', null);
        textSpan.innerHTML = 'Not Set';
      } else {
        this.tristate('state', hidden.value === "true");
        if (hidden.value === "true") {
          textSpan.innerHTML = 'Subscribe All';
        } else {
          textSpan.innerHTML = 'Unsubscribe All';
        }
      } 
    }
    ,
	// To be run when the tristate widget changes (user click).
    change: function(state, value) {
      // Get the hidden input widget.
      var hidden = document.getElementById(this.attr('id') + "Hidden");
      // Get the text widget.
      var textSpan = document.getElementById(this.attr('id') + "Text");
      // Populate the hidden input widget and its text representation according
      // to the changed value of the tristate widget.
      if (state === null) {
        hidden.value = "unset";
        textSpan.innerHTML = 'Not Set';
      } else {
        hidden.value = state;
        if (state === true) {
          textSpan.innerHTML = 'Subscribe All';
        } else {
          textSpan.innerHTML = 'Unsubscribe All';
        }
      } 
    }
  });
