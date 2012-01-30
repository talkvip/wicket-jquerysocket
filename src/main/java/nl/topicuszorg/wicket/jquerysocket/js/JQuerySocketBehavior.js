$.socket("${url}", {
        dataType: "json",
        type:"http",
        message: function(event) {
                try {
                	streamLog(event.data.javascript, "debug");
                	
                	eval(event.data.javascript);
                } catch(ex) {
                	streamLog(event.data.javascript, "debug");
                }
        },
        openData: {
            type: "json",
            clientid: "${clientid}"
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