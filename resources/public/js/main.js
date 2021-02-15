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
    var $this = $(e.currentTarget);
    console.log($this.data("copy"));
});

$(".setter").click( e => {
    var $this = $(e.currentTarget);
    var path = $this.data("url");
    var data = {"key": $this.data("key"),
		"value": $this.data("value")};
    post(path, data);
});
