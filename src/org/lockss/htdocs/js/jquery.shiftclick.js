/**
 * Taken from https://gist.github.com/DelvarWorld/3784055
 * 
 * Usage: $form.find('input[type="checkbox"]').shiftSelectable();
 * replace input[type="checkbox"] with the selector to match your list of checkboxes
 */

$(document).ready(function () {
    $form = $("form");
    $form.find(".shift-click-box").shiftSelectable();
});

$.fn.shiftSelectable = function() {
    var lastChecked,
        $boxes = this;

    $boxes.click(function(evt) {
        if(!lastChecked) {
            lastChecked = this;
            return;
        }

        if(evt.shiftKey) {
            var start = $boxes.index(this),
                end = $boxes.index(lastChecked);
            // select every box between start and end and give them the same status
            $boxes.slice(Math.min(start, end), Math.max(start, end) + 1)
                .prop('checked', lastChecked.checked);
        }

        lastChecked = this;
    });
};