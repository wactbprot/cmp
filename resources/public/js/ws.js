var ws = new WebSocket("ws://127.0.0.1:8010/ws")

ws.onopen = function (event) {
   ws.send(JSON.stringify({"ok":true}));
};

var set_value_and_class = (k, v) => {
    var $k = $("#"+k);
    $k.removeClass();
    $k.addClass("is-size-6 " + v);
    $k.text(v);
}

var get_func = (k) => {return k.split("_")[3];}

var show_message = (k, v) => {
    // ok is already the answer of
    // successful message task
    // so leave everything as
    // it is if ok comes in
    if(v != "ok") {	
	var $message = $("#"+k);
	$message.children(".modal-content").children(".notification").text(v);
	$message.addClass("is-active");
    }
}

ws.onmessage = function (event) {
    var data =JSON.parse(event.data);
    if(data.key && data.value) {
	var k = data.key;
	var v = data.value;
	if(get_func(k) == "message"){
	    show_message(k, v);
	}else{
	    set_value_and_class(k, v);
	}
    }
}
