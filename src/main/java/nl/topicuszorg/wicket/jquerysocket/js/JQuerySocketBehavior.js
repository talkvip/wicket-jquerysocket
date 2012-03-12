var socket = $.socket("${url}", {
    transports: [${transports}],
    id: function() { return"${clientid}" }
});

socket.on("message", function(data){
	try {
    	streamLog(data, "debug");
    	eval(data);
    } catch(ex) {
    	streamLog(data, "debug");
    }
});

function streamLog(s, t) {
	try {
		switch(t) {
			case "debug":
				console.debug(s);
				break;
			case "info":
				console.info(s);
				break;
			case "warn":
				console.warn(s);
				break;
			case "error":
				console.warn("ERROR: " + s);
				break;
			case "log":
				console.log(s);
				break;
			case "alert":
				alert(s);
				break;
			default:
				console.log(s);
				break;
		}
	} catch(ex) {
		if(t == "error") alert("ERROR: " + s);
		return true;
	}
}