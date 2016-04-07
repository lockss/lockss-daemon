/**
 * Manage Bulk Actions button
 */

$(document).ready(function () {
    $("#bulk-actions-menu").shiftSelectable();
    
    $("#bulk-actions-btn").click(function(){
        $("#bulk-actions-msg-box").text("");
        
        var action = $("#bulk-actions-menu option:selected").val();
        if(action == ""){
            $("#bulk-actions-msg-box").removeClass("success")
            $("#bulk-actions-msg-box").addClass("warning")
            $("#bulk-actions-msg-box").text("Please select an action first.");
        } else {
            var count = 0;
            $(".bulk-actions-ckbox:checked").each( function(){
                count++;
                $(this).prop('checked', false);
                
                // Get id from check box
                var publicationNumber = $(this).attr('id').replace("bulk-action-ckbox_","");
                
                // Get tribox
                var triboxSelector = "#publicationSubscription"+publicationNumber;
                
                // Update tribox
                switch (action) {
                    case "subscribeAll":
                        $(triboxSelector).tristate('state', true);
                        break;
                    case "unsubscribeAll":
                        $(triboxSelector).tristate('state', false);
                        break;
                    case "unsetAll":
                        $(triboxSelector).tristate('state', null);
                        break;
                }
                refreshPublicationSubscription(
                        "publicationSubscription" + publicationNumber + "Hidden",
                        "subscribedRanges" + publicationNumber,
                        "unsubscribedRanges" + publicationNumber)
            });
            $("#bulk-actions-msg-box").removeClass("warning")
            $("#bulk-actions-msg-box").addClass("success")
            $("#bulk-actions-msg-box").text(count+" selections have been "+action+".");
        }
    });
    

    // Toggles the ability of two subscription slave
    // elements through the selection
    // of a master publication subscription tribox element.
    function refreshPublicationSubscription(
            pubSubId,
            subscribedRangesId, 
            unsubscribedRangesId) {
        // The publication subscription tribox.
        var pubSub = document.getElementById(pubSubId);

        // The subscribed ranges input box.
        var subscribedRanges = document
                .getElementById(subscribedRangesId);

        // The unsubscribed ranges input box.
        var unsubscribedRanges = document
                .getElementById(unsubscribedRangesId);

        // Check whether the publication subscription is
        // changing to "Not Set".
        if (pubSub.value == "unset") {
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
        if (subscribedRanges !== "unset") {
            subscribedRanges.disabled = true;
        }

        // Check whether the publication subscription is
        // changing to "Unsubscribe
        // All".
        if (pubSub.value == "false") {
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
});