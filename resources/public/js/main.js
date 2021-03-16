var post = (path, data, callback) => { 
    if(! callback){callback = () => {console.log(data)}}
    
    $.ajax( {
	type: "POST",
	url: "/" + path,
	data: JSON.stringify(data),
	success: callback,
	contentType: "application/json; charset=utf-8",
	dataType: "json"
    });
}

$(".copy").click( e => {

    // https://stackoverflow.com/questions/33855641/copy-output-of-a-javascript-variable-to-the-clipboard
    var $this = $(e.currentTarget);
    var k = $this.data("copy");
    var d = document.createElement("textarea");
    document.body.appendChild(d);
    d.value = k;
    d.select();
    document.execCommand("copy");
    document.body.removeChild(d);  
});


var get_url = (e) => {
    var $this = $(e.currentTarget);
    return $this.data("url");
}

var get_data = (e) => {
    var $this = $(e.currentTarget);
    return {"key": $this.data("key"),
	    "value": $this.data("value")}
}

$(".setter").click( e => {
    post(get_url(e), get_data(e));
});

$(".restart").click( e => {
    post(get_url(e), get_data(e));
    var $this = $(e.currentTarget);
    
    $this.after( "<progress class='progress is-danger' max='100'>30%</progress>")
    setTimeout(()=> { location.reload(); }, 2000); 
});


$(".rebuild").click( e => {
    post(get_url(e), get_data(e));
    var $this = $(e.currentTarget);
    
    $this.after( "<progress class='progress is-warning' max='100'>30%</progress>")
    setTimeout(()=> { location.reload(); }, 2000); 
});
