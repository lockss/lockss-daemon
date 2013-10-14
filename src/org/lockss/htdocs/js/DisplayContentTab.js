var lastChecked = null;
$(document).ready(function () {
    var $chkboxes = $(".chkbox");
    $chkboxes.click(function(e) {
        if(!lastChecked) {
            lastChecked = this;
            return;
        }
        if(e.shiftKey) {
            var start = $chkboxes.index(this);
            var end = $chkboxes.index(lastChecked);
            var chkboxList = $chkboxes.slice(Math.min(start,end), Math.max(start,end)+ 1);
            for (var i = 0; i < chkboxList.length; i++) {
                chkboxList[i].checked = lastChecked.checked;
            }
        }
        lastChecked = this;
    });
//    var form = document.getElementById('submitForm');
//    form.addEventListener('submit', function(e) {
//        if (button == 'add') {
//            alert("Add");
//        }
//        if (button == 'delete') {
//            confirm("Are you sure you want to delete the selected AUs?");
//        }
//    });
});

function hideMessages() {
    $(".messageDiv").slideUp();
    $(".messageDivClose").hide();
}
