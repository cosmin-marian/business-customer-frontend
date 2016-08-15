function showHideAted1Func() {
 //alert("inside showHideNukUtr");
    var selectedDiv = $('#hidden-identifiers');
//    var hiddenUtr = $('#hidden-uniqueTaxRef-true');
//    var hiddenShadeBox = $('#shade-box');
    var submitButton = $('#submit');
    var continueButton = $('#continue');
    var permission = $("#ated1-false-hidden");

//    hiddenUtr.hide();
//    hiddenShadeBox.hide();
    submitButton.hide();
    continueButton.show();
    permission.hide();


    $('input[type=radio][name=ated1]').change(function(){
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
      showHideAted1Func();
    });
