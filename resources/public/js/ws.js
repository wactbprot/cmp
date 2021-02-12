var ws = new WebSocket("ws://127.0.0.1:8010/ws")

ws.onopen = function (event) {
   ws.send(JSON.stringify({"ok":true})); 
};

var set_value = function(k, v){
    $("#"+k).text(v);
}

ws.onmessage = function (event) {
    var data =JSON.parse(event.data);
    if(data.key && data.value) {
	set_value(data.key, data.value);
    }
}
