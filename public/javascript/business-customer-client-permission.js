function showHideClientPermissionFunc() {
 //alert("inside showHideNukUtr");
    var selectedDiv = $('#hidden-identifiers');
//    var hiddenUtr = $('#hidden-uniqueTaxRef-true');
//    var hiddenShadeBox = $('#shade-box');
    var submitButton = $('#submit');
    var continueButton = $('#continue');
    var permissionFalse = $("#client-permission-false-hidden");
    var permissionTrue = $("#client-permission-true-hidden");

//    hiddenUtr.hide();
//    hiddenShadeBox.hide();
    submitButton.hide();
    continueButton.show();
    permissionFalse.hide();
    permissionTrue.hide();


    $('input[type=radio][name=permission]').change(function(){
        if(this.value == 'true') {

//            hiddenUtr.show();
//            hiddenShadeBox.show();
            submitButton.hide();
            continueButton.show();
            permissionFalse.hide();
            permissionTrue.show();
        } else {
        //alert("Inside false radio");
//            hiddenUtr.hide();
//            hiddenShadeBox.hide();
            submitButton.show();
            continueButton.hide();
            permissionFalse.show();
            permissionTrue.hide();

        }
    });

}

$(document).ready(function() {
      showHideClientPermissionFunc();
    });
