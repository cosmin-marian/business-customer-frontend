function countryCodeAutoComplete() {
//    var input = $("<input></input>")
//    .attr("id", "businessAddress.country")
//    .attr("name", "businessAddress.country")
//    .attr("class", "form-control")
//    .attr("type", "text");
//    var isoListMap = [];
//    var select = $('select');
//    var selected = "";
//    $(select).each(function(){
//        $(this).find('option').each(function(){
//            var item = $(this);
//            var label = (item.text());
//            var value = "";
//            if(item.attr('value')) {
//                value = item.attr('value')
//                if(item.attr('selected') == "selected") {
//                    selected = item.attr('value')
//                }
//            }
//            isoListMap.push({
//                'label': label,
//                'value': value
//            });
//        });
//    });
//    $("select").remove();
//    input.attr('value')
//    var isErrorThere = $("#businessAddress-country_div").has("div.form-field--error").length
//    console.log(isErrorThere)
//    if(isErrorThere != 0) {
//        $("#businessAddress-country_div").find("div.form-field--error").append(input)
//    }else{
//        $("#businessAddress-country_div").append(input);
//    }
//    $("#businessAddress\\.country").autocomplete({
//        source: isoListMap,
//        minLength: 2
//    });
////////////////////////////////////////////
(function( $ ) {
    $.widget( "custom.combobox", {
      _create: function() {
        this.wrapper = $( "<span>" )
//          .addClass( "custom-combobox" )
          .insertAfter( this.element );

        this.element.hide();
        this._createAutocomplete();
        this.element.attr("id", this.element.attr("id")+"_");
      },

      _createAutocomplete: function() {
        var selected = this.element.children( ":selected" ),
          value = selected.val() ? selected.text() : "";

        this.input = $( "<input>" )
          .appendTo( this.wrapper )
          .val( value )
          .attr( "title", "" )
          .attr( "id", this.element.attr("id") )
          .addClass( "custom-combobox-input ui-widget ui-widget-content ui-state-default ui-corner-left form-control" )
          .autocomplete({
            delay: 0,
            minLength: 2,
            source: $.proxy( this, "_source" )
          });

        this._on( this.input, {
          autocompleteselect: function( event, ui ) {
            ui.item.option.selected = true;
            this._trigger( "select", event, {
              item: ui.item.option
            });
          },

          autocompletechange: "_removeIfInvalid"
        });
      },

      _source: function( request, response ) {
        var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
        response( this.element.children( "option" ).map(function() {
          var text = $( this ).text();
          if ( this.value && ( !request.term || matcher.test(text) ) )
            return {
              label: text,
              value: text,
              option: this
            };
        }) );
      },

      _removeIfInvalid: function( event, ui ) {

        // Selected an item, nothing to do
        if ( ui.item ) {
          return;
        }

        // Search for a match (case-insensitive)
        var value = this.input.val(),
          valueLowerCase = value.toLowerCase(),
          valid = false;
        this.element.children( "option" ).each(function() {
          if ( $( this ).text().toLowerCase() === valueLowerCase ) {
            this.selected = valid = true;
            return false;
          }
        });

        // Found a match, nothing to do
        if ( valid ) {
          return;
        }

        // Remove invalid value
        this.input
          .val( "" )
          .attr( "title", value + " didn't match any item" );
        this.element.val( "" );
        this.input.autocomplete( "instance" ).term = "";
      },

      _destroy: function() {
        this.wrapper.remove();
        this.element.show();
      }
    });
  })( jQuery );
///////////////////////////////////////////
$(function() {
    $("#businessAddress\\.country").combobox();
});

}
