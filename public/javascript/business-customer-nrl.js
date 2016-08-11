function showHideNrlFunc() {
 //alert("inside showHideNukUtr");
    var selectedDiv = $('#hidden-identifiers');
    var hiddenUtr = $('#hidden-uniqueTaxRef-true');
    var hiddenShadeBox = $('#shade-box');
    var submitButton = $('#submit');
    var continueButton = $('#continue');

    hiddenUtr.hide();
    hiddenShadeBox.hide();
    submitButton.hide();


    $('input[type=radio][name=paysSA]').change(function(){
        if(this.value == 'true') {

            hiddenUtr.show();
            hiddenShadeBox.show();
            submitButton.show();
            continueButton.hide();
        } else {
        //alert("Inside false radio");
            hiddenUtr.hide();
            hiddenShadeBox.hide();
            submitButton.hide();
            continueButton.show();

        }
    });

}

$(document).ready(function() {
      showHideNrlFunc();
    });
