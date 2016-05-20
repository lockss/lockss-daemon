/**
 * Manage Bulk Actions button on subscription page
 * 
 * Picks up the action in the select box and 
 * applies it to every triboxes in the selected rows.
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
            if(count > 0){
                $("#bulk-actions-msg-box").text(count+" selections have been set for "+action+". Press the 'Add' button to apply the change.");
            } else {
                $("#bulk-actions-msg-box").removeClass("success")
                $("#bulk-actions-msg-box").addClass("warning")
                $("#bulk-actions-msg-box").text("You haven't selected any publication.")
            }
        }
    });
    

    /* 
     * Rewrite the code for the tribox as it needs to works slightly differently.
     * 
     * This function set every elements in the same row than the tribox 
     * which has change status after the bulk action as been applied.
     */
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
        // changing to "Unsubscribe All".
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