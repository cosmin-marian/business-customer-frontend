function countryCodeAutoComplete() {
    var combo = $("<input></input>").attr("id", "abc-id").attr("name", "abc").attr("type", "text");
    var isoListMap = [];
    var select = $('select');
    $(select).each(function(){
        $(this).find('option').each(function(){
            var item = $(this);
            var label = (item.text());
            var value = "";
            if(item.attr('value')) {
                value = item.attr('value')
            }
            isoListMap.push({
                'label': label,
                'value': value
            });
        });
    });

    $("#SELECTOR").append(combo);
    $("#abc-id").autocomplete({ source: isoListMap, minLength: 1 });
}
