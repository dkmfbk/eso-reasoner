
$(document).ready(function(){
	
	$("#navigation").treeview({
		animated: "fast",
		collapsed: true,
		control: "#treecontrol"
	});

	$("#fileForm").submit(function() {
		$("#submitButton").button('loading');
	});
});