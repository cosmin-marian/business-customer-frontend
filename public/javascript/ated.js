function countryCodeAutoComplete() {
    var input = $("<input></input>")
    .attr("id", "businessAddress.country")
    .attr("name", "businessAddress.country")
    .attr("class", "form-control")
    .attr("type", "text");
    var isoListMap = [];
    var select = $('select');
    var selected = "";
    $(select).each(function(){
        $(this).find('option').each(function(){
            var item = $(this);
            var label = (item.text());
            var value = "";
            if(item.attr('value')) {
                value = item.attr('value')
                if(item.attr('selected') == "selected") {
                    selected = item.attr('value')
                }
            }
            isoListMap.push({
                'label': label,
                'value': value
            });
        });
    });
//    $("select").remove();
    input.attr('value')
    var isErrorThere = $("#businessAddress-country_div").has("div.form-field--error").length
    console.log(isErrorThere)
    if(isErrorThere != 0) {
        $("#businessAddress-country_div").find("div.form-field--error").append(input)
    }else{
        $("#businessAddress-country_div").append(input);
    }
    $("#businessAddress\\.country").autocomplete({
        source: isoListMap,
        minLength: 2
    });
}
