var ws = new WebSocket("ws://127.0.0.1:8010/ws")

ws.onopen = function (event) {
   ws.send(JSON.stringify({"ok":true}));
};

var set_value_and_class = function(k, v){
    var $k = $("#"+k);
    $k.removeClass();
    $k.addClass("is-size-6 " + v);
    $k.text(v);
}

ws.onmessage = function (event) {
    var data =JSON.parse(event.data);
    if(data.key && data.value) {
	set_value_and_class(data.key, data.value);
    }
}
