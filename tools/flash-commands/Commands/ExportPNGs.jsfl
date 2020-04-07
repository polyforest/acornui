fl.outputPanel.clear();
var doc = fl.getDocumentDOM();
var library = document.library;
var log = [];

if (!doc) {
	alert("Select a document.");
} else {	
	var docName = doc.name.substring(0, doc.name.length - 4);
	var folder = doc.pathURI.substring(0, doc.pathURI.lastIndexOf(doc.name));
	
	// Create a temporary doc.
	fl.createDocument();
	var exportDoc = fl.getDocumentDOM();
	
	trace("Publishing pngs to: " + folder);
	for each (var item in library.items) {
		if (item.linkageExportForAS) {
			makeDirs(item.name);
			trace("Item: " + item.name);
			var totalFrames = item.timeline.layers[0].frames.length;
			var timeline = exportDoc.timelines[0];
			timeline.removeFrames(0, timeline.frameCount);
			timeline.insertFrames(totalFrames);
			exportDoc.addItem({ x: 0, y: 0 }, item);
			
			exportDoc.timelines[0].layers[0].frames[0].elements[0].symbolType = "graphic";
			resizeDocument(exportDoc);
			var lastLabel = null;
			var sameLabelCount = 0;
			for (var i = 0; i < totalFrames; i++) {
				exportDoc.timelines[0].setSelectedFrames(i, i);
				var pngName;
				var frame = item.timeline.layers[0].frames[i];
				if (frame.labelType === "name") {
					if (lastLabel !== frame.name) {
						lastLabel = frame.name;
					} else {
						sameLabelCount++;
					}
					pngName = folder + item.name + "_" + frame.name + numberTag(sameLabelCount) + ".png";
				} else {
					pngName = folder + item.name + numberTag(i) + ".png";
				}
				exportDoc.exportPNG(pngName, true, true);
			}
			exportDoc.timelines[0].setSelectedFrames(0, 0);
		}
	}
	
	exportDoc.close(false);
	fl.outputPanel.clear();
	fl.trace(log.join("\n"));

}

/**
 * Creates the number tag for the given frame index.
 * @param i
 * @returns {string}
 */
function numberTag(i) {
	if (i === 0) return "";
	else return "_" + padNumber(i, 4);
}

/**
 * Appends to the log array and invokes `fl.trace`
 */
function trace(message) {
	log.push(message);
	if (log.length > 1000) log.unshift();
	fl.trace(message);
}

function makeDirs(path) {
	var split = path.split("/");
	var parent = folder;
	for (var i = 0; i < split.length - 1; i++) {
		parent += split[i] + "/";
		if (!FLfile.exists(parent)) {
			FLfile.createFolder(parent);
		}
	}
}

/**
 * Returns a String representing a number prefixed with zeros up to a certain number of digits. 
 * E.g.
 * padNumber(1, 3) // "001"
 * padNumber(523, 2) // "523"
 * padNumber(45, 3) // "045"
 * @param num {Number} An integer to prefix.
 * @param digits {Number} The minimum number of display digits.
 * @returns {string}
 */
function padNumber(num, digits) {
	var str = num + "";
	var intDiff = digits - str.length;
	while (intDiff > 0) {
		str = "0" + str;
		intDiff--;
	}
	return str;
}

/**
 * Resizes the given document to fit its single element.
 * @param doc {Document}
 * @returns {boolean} Returns false if no element was found.
 */
function resizeDocument(doc) {
	doc.selectAll();
	var element = doc.timelines[0].layers[0].frames[0].elements[0];
	if (element == null) return false;
	element.x += -element.left;
	element.y += -element.top;
	doc.width = Math.ceil(element.width);
	doc.height = Math.ceil(element.height);	
	return true;
}
