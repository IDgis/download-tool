require([
	"dojo/dom",
	"dojo/dom-attr",
	"dojo/request/xhr",
	
	"dojo/domReady!"
	], function(dom, domAttr, xhr) {
		verifyStatus();
		
		function verifyStatus() {
			const nodeId = dom.byId("js-request-id");
			const id = domAttr.get(nodeId, "data-request-id");
			const nodeFeedback = dom.byId("js-feedback");
			
			xhr(jsRoutes.controllers.DownloadForm.status(id).url, {
				handleAs: "json"
			}).then(function(data) {
				let html;
				if(!data["exists"]) {
					html = "De opgevraagde data is onbekend";
				} else if(data["expired"]) {
					html = "De data is verlopen";
				} else if(data["ready"]) {
					if(data["success"]) {
						html = 'De data staat klaar: <a href="' + data["url"] + '">' + data["url"] + '</a>';
					} else {
						html = "Het ophalen van de data is mislukt";
					}
				} else {
					html = "De data staat nog niet klaar";
					
					setTimeout(verifyStatus, 5000);
				}
				
				domAttr.set(nodeFeedback, "innerHTML", html);
			}, function(err){
				const html = "Er ging iets mis bij het downloaden van het bestand";
				domAttr.set(nodeFeedback, "innerHTML", html);
			});
		}
});
