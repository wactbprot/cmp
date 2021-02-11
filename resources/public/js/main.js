$(".copy").click( e => {
    var $this = $(e.currentTarget);
    console.log($this.data("copy"));
});
