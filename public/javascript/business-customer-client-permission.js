function showHideClientPermissionFunc() {
 //alert("inside showHideNukUtr");
    var selectedDiv = $('#hidden-identifiers');
//    var hiddenUtr = $('#hidden-uniqueTaxRef-true');
//    var hiddenShadeBox = $('#shade-box');
    var submitButton = $('#submit');
    var continueButton = $('#continue');
    var permission = $("#client-permission-false-hidden");

//    hiddenUtr.hide();
//    hiddenShadeBox.hide();
    submitButton.hide();
    continueButton.show();
    permission.hide();


    $('input[type=radio][name=permission]').change(function(){
        if(this.value == 'true') {

//            hiddenUtr.show();
//            hiddenShadeBox.show();
            submitButton.hide();
            continueButton.show();
            permission.hide();
        } else {
        //alert("Inside false radio");
//            hiddenUtr.hide();
//            hiddenShadeBox.hide();
            submitButton.show();
            continueButton.hide();
            permission.show();

        }
    });

}

$(document).ready(function() {
      showHideClientPermissionFunc();
    });
