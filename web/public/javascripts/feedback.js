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
				if(data["ready"]) {
					if(data["success"]) {
						const html = 'De data staat klaar: <a href="' + data["url"] + '">' + data["url"] + '</a>';
						domAttr.set(nodeFeedback, "innerHTML", html);
					} else {
						const html = 'Het ophalen van de data is mislukt';
						domAttr.set(nodeFeedback, "innerHTML", html);
					}
				} else {
					const html = 'De data staat nog niet klaar';
					domAttr.set(nodeFeedback, "innerHTML", html);
					
					setTimeout(verifyStatus, 5000);
				}
			});
		}
});
