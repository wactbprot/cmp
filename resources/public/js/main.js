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

$(".setter").click( e => {
    var $this = $(e.currentTarget);
    var path = $this.data("url");
    var data = {"key": $this.data("key"),
		"value": $this.data("value")};
    post(path, data);
});
