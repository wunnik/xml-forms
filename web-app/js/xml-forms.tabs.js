$(function() {
// Setup tabs
  	$(document).on ("click" , 'a.tab',function(e) {
        var test=$(document).find("#tab-2");
        e.preventDefault();
		$(this).tab('show');
	});
});