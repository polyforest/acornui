fl.outputPanel.clear();
var doc = fl.getDocumentDOM();
var library = document.library;
var log = [];

var firstScript = doc.timelines[0].layers[0].frames[0].actionScript;
fl.trace(firstScript);
var dpiKey = "//dpi=";
var sizes = [1];
var nameWith1x = false; // If true, 1x sizes will have _1x added to their names.
if (firstScript.indexOf(dpiKey) === 0) {
	var sizeStrs = firstScript.substring(dpiKey.length, firstScript.length).split(",");
	sizes.length = sizeStrs.length;
	for (var sizeStrI = 0; sizeStrI < sizeStrs.length; sizeStrI++) {
		sizes[sizeStrI] = parseInt(sizeStrs[sizeStrI]);
	}
	nameWith1x = true;
}

if (!doc) {
	alert("Select a document.");
} else {
	var folder = doc.pathURI.substring(0, doc.pathURI.lastIndexOf(doc.name));
	var selectedItems = doc.library.getSelectedItems();
	if (selectedItems.length === 0) selectedItems = doc.library.items;

	// Create a temporary doc.
	fl.createDocument();
	var exportDoc = fl.getDocumentDOM();

	trace(selectedItems.length + " selected items");
	for (var sizeIndex = 0; sizeIndex < sizes.length; sizeIndex++) {
		var size = sizes[sizeIndex];
		trace("Publishing pngs to: " + folder + " size: " + size + "x");
		for each (var item in library.items) {
			if (item.linkageExportForAS && selectedItems.indexOf(item) !== -1) {
				/** @type string */
				var itemName;
				var unpackedIndex = item.name.indexOf("_unpacked/");
				if (!nameWith1x && size === 1) {
					itemName = item.name;
				} else {
					if (unpackedIndex === -1) {
						// Not part of an atlas, rename the png to match the hdpi size:
						itemName = item.name + "_" + size + "x";
					} else {
						// Part of an atlas, rename the atlas to match the hdpi size:
						itemName = item.name.substring(0, unpackedIndex) + "_" + size + "x" + item.name.substring(unpackedIndex, item.name.length);
					}
				}
				trace("Item: " + itemName);
				makeDirs(itemName);
				var totalFrames = item.timeline.layers[0].frames.length;
				var timeline = exportDoc.timelines[0];
				timeline.removeFrames(0, timeline.frameCount);
				timeline.insertFrames(totalFrames);
				exportDoc.addItem({x: 0, y: 0}, item);

				// noinspection JSValidateTypes
				/** @type SymbolInstance */
				var inserted = exportDoc.timelines[0].layers[0].frames[0].elements[0];
				inserted.symbolType = "graphic";

				inserted.scaleX = size;
				inserted.scaleY = size;

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
						pngName = folder + itemName + "_" + frame.name + numberTag(sameLabelCount) + ".png";
					} else {
						pngName = folder + itemName + numberTag(i) + ".png";
					}
					exportDoc.exportPNG(pngName, true, true);
				}
				exportDoc.timelines[0].setSelectedFrames(0, 0);
			}
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
