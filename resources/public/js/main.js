
var post = ($this, path, data, callback) => { 
    if(! callback){callback = () => {console.log("ok")}}
    
    $.ajax( {
	type: "POST",
	url: "/" + path,
	data: JSON.stringify(data),
	success: callback,
	contentType: "application/json; charset=utf-8",
	dataType: "json"
    });
}
//----------------------------------
$("#year").change( e => {
    var $this = $(e.currentTarget);
    post($this, "year", {"value": $this.val()}, () => {location.reload();});
});
$("#standard").change( e => {
    var $this = $(e.currentTarget);
    post($this, "standard",{"value": $this.val()}, () => {location.reload();});
});
$("#n").change( e => {
    var $this = $(e.currentTarget);
    post($this, "n", {"value": $this.val()}, () => {location.reload();});
});

$("#gas").change( e => {
    var $this = $(e.currentTarget);
    post($this, "gas", {"value": $this.val()});
});

$("#mode").change( e => {
    var $this = $(e.currentTarget);
    post($this, "mode", {"value": $this.val()});
});

$("#maintainer").change( e => {
    var $this = $(e.currentTarget);
    post($this, "maintainer", {"value": $this.val()});
});

//----------------------------------
$(".run").click( e => {
    var $this = $(e.currentTarget),
	row = $this.data("row"),
	data = {"value": $("#task_" + row).val(), "row": row};
    $("#device-stdout_" + row).val("...will post to server");
    post($this, "run", data);
    
});

//----------------------------------
$(".device").change(e => {
    var $this = $(e.currentTarget),
	row =  $this.data("row"),
	data = {"value": $this.val(), "row": row};
    post($this, "device/"+ row, data, () => {location.reload(); });
});

//----------------------------------
$(".reset").click( e => {
    var $this = $(e.currentTarget),
	row = $this.data("row"),
	data = {"value": true, "row": row};
    post($this, "reset", data, () => {location.reload()});
});

$(".id").change( e => {
    var $this = $(e.currentTarget),
	data = {"value": $this.val(), "row": $this.data("row")};
    post($this, "id", data);
});

$(".branch").change( e => {
    var $this = $(e.currentTarget),
	data = {"value": $this.val(),"row": $this.data("row")};
    post($this, "branch", data);
});

$(".fullscale").change( e => {
    var $this = $(e.currentTarget),
	data = {"value": $this.val(),"row": $this.data("row")};
    post($this, "fullscale", data);
});

$(".defaults").change( e => {
    var $this = $(e.currentTarget),
	row = $this.data("row"),
        key = $this.data("key"),
	data = {"value": $this.val(), "row": row, "key": key};
    post($this, "default/" + row, data);
});
