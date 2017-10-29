var originCache = {};
fl.runScript(fl.configURI + 'Javascript/JSON.jsfl')
JSON.prettyPrint = true;
fl.outputPanel.clear();
var data = { library: {}, easings: {} };
data.library["__document"] = { type: "animation", timeline: processTimeline(document.timelines[0]) };

for (var i = 0; i < document.library.items.length; i++) {
	var item = document.library.items[i];
	// Library items marked as linkageExportForAS are considered assets and will be converted to a png.
	if (item.itemType == "movie clip") {
		if (item.linkageExportForAS) {
			var name = item.name;
			if (name.indexOf("resources/") == 0) name = name.substring("resources/".length);
			var unpackedIndex = name.indexOf("_unpacked")
			if (unpackedIndex == -1) {
				data.library[item.name] = { type: "image", path: name + ".png" };
			} else {
				var regionName = name.substring(name.lastIndexOf("/") + 1);
				var atlasPath = name.substring(0, unpackedIndex) + ".json";
				data.library[item.name] = { type: "atlas", regionName: regionName, atlasPath: atlasPath };
			}
		} else {
			data.library[item.name] = { type: "animation", timeline: processTimeline(item.timeline) };
		}
	}
}

function processTimeline(timeline) {
	var timelineData = { duration: round(timeline.frameCount / document.frameRate), layers: [] };
	for (var layerIndex = 0; layerIndex < timeline.layers.length; layerIndex++) {
		var layer = timeline.layers[layerIndex];
		if (layer.layerType != "normal") continue;
		
		// Search the keyframes for the movie clip instance, and ensure there is only one per layer.
		var symbolName = null;
		for (var frameIndex = 0; frameIndex < layer.frames.length; frameIndex++) {
			var frame = layer.frames[frameIndex];
			if (frame.startFrame == frameIndex && frame.elements.length > 0) {
				if (frame.elements.length > 1) throw Error("Cannot have more than one element per layer.");
				var element = frame.elements[0];
				if (element.symbolType == "movie clip") {
					var name = element.libraryItem.name;
					if (name != symbolName) {
						if (symbolName != null) throw Error("Cannot have different movie clips on one layer.");
						symbolName = name;
					}
				} else {
					throw Error("Only movie clips are currently supported.");
				}
			}
		}
		
		var layerData = { name: layer.name, symbolName: symbolName, visible: layer.visible, keyFrames: [] };
		
		var lastKeyFrameData = null;
		for (var frameIndex = 0; frameIndex < layer.frames.length; frameIndex++) {
			var frame = layer.frames[frameIndex];
			
			if (frame.startFrame == frameIndex) {
				// Keyframe.
				var keyFrameData = createFrameData(frame);
				removeUnchanged(keyFrameData, lastKeyFrameData);
				layerData.keyFrames.push(keyFrameData);
				lastKeyFrameData = keyFrameData;
			}
		}
		timelineData.layers.push(layerData);
	}
	return timelineData;
}

function createFrameData(frame) {
	//fl.trace("----------------------FRAME-----------------------------");
	var frameData = { time: round(frame.startFrame / document.frameRate), easings: {} };
	
	if (frame.tweenType == "motion") {
		var easingProps;
		if (frame.useSingleEaseCurve) {
			easingProps = ["all"];
		} else {
			easingProps = ["position", "rotation", "scale", "color"];
		}
		for each (var easingProp in easingProps) {
			var customEase = frame.getCustomEase(easingProp);
			var points = []; // x, y, ...
			// No need to save the first (0,0) and last (1,1).
			for (var i = 1; i < customEase.length - 1; i++) {
				var point = customEase[i];
				points.push(round(point.x));
				points.push(round(point.y));
			}
			frameData.easings[easingProp] = points;
		}
	}
	
	var rawProps; // The properties before associating them with easing functions.
	if (frame.elements.length > 0) {
		var element = frame.elements[0];
		rawProps = createPropsData(element);
	} else {
		rawProps = createPropsData(null);
	}
	frameData.props = {};
	var propsEasingMap;
	if (frame.tweenType == "motion") {
		if (frame.useSingleEaseCurve) {
			propsEasingMap = { visible: null, originX: null, originY: null, x: "all", y: "all", scaleX: "all", scaleY: "all", shearXZ: "all", shearYZ: "all", rotationZ: "all", colorR: "all", colorG: "all", colorB: "all", colorA: "all" };
		} else {
			propsEasingMap = { visible: null, originX: null, originY: null, x: "position", y: "position", scaleX: "scale", scaleY: "scale",  shearXZ: "rotation", shearYZ: "rotation", rotationZ: "rotation", colorR: "color", colorG: "color", colorB: "color", colorA: "color" };
		}
	} else {
		propsEasingMap = { visible: null, originX: null, originY: null, x: null, y: null, scaleX: null, scaleY: null, shearXZ: null, shearYZ: null, colorR: null, colorG: null, colorB: null, colorA: null };
	}
	for (var all in rawProps) {
		var easing = propsEasingMap[all];
		frameData.props[all] = { value: rawProps[all], easing: easing };
		if (frameData.props[all].easing == null)
			delete frameData.props[all].easing;
	}
	return frameData;
}

function createPropsData(element) {
	if (element == null) {
		return { visible: 0 };
	}
	var props = { visible: 1 };
	
	var origin = getOrigin(element);
	
	// Registration point ("origin") in acorn is local to the component.
	var inv = fl.Math.invertMatrix( element.matrix )
	var p2 = prj(element.transformX, element.transformY, inv);
	
	props.originX = round(p2.x + origin.x);
	props.originY = round(p2.y + origin.y);
	
	// Translation in acorn is relative to the origin.
	props.x = round(element.transformX);
	props.y = round(element.transformY);
	
	var degRad = Math.PI / 180;
	
	if (isNaN(element.rotation)) {
		var r = 0;
		if (element.skewY > 0 && element.skewX > 0) {
			r = Math.min(element.skewX, element.skewY);
		} else if (element.skewY < 0 && element.skewX < 0) {
			r = Math.max(element.skewX, element.skewY);
		}
		props.scaleX = round(element.scaleX * Math.cos((element.skewY - r) * degRad));
		props.scaleY = round(element.scaleY * Math.cos((element.skewX - r) * degRad));

		props.shearXZ = round(Math.tan((element.skewX - r)));
		props.shearYZ = round(Math.tan((element.skewY - r)));
		props.rotationZ = round(r * degRad);

	} else {
		props.shearXZ = 0;
		props.shearYZ = 0;
		props.scaleX = round(element.scaleX);
		props.scaleY = round(element.scaleY);
		props.rotationZ = round(element.rotation * degRad);
	}
	props.colorA = element.colorAlphaPercent / 100;
	props.colorR = element.colorRedPercent / 100;
	props.colorG = element.colorGreenPercent / 100;
	props.colorB = element.colorBluePercent / 100;
	return props;
}

function removeUnchanged(keyFrameData, lastKeyFrameData) {
	return;
	if (lastKeyFrameData == null) return;
	for (var all in keyFrameData.props) {
		if (lastKeyFrameData.props[all] != null &&
			keyFrameData.props[all].value == lastKeyFrameData.props[all].value) {
			delete keyFrameData.props[all];
		}
	}
}

function round(n) {
	var m = 1000;
	return Math.round(n * m) / m;
}

/**
 * Multiplies an x,y coordinate by the given matrix.
 */
function prj(x, y, matrix) {
	var x2 = matrix.a * x + matrix.c * y + matrix.tx;
	var y2 = matrix.b * x + matrix.d * y + matrix.ty;
	return { x: x2, y: y2 };
}

function rot(x, y, matrix) {
	var x2 = matrix.a * x + matrix.c * y;
	var y2 = matrix.b * x + matrix.d * y;
	return { x: x2, y: y2 };
}

function getOrigin(element) {
	if (originCache[element.libraryItem.name] == null) {
		originCache[element.libraryItem.name] = _getOrigin(element.libraryItem);
	}
	return originCache[element.libraryItem.name];
}

function _getOrigin(libraryItem) {
	var itemType = libraryItem.itemType;
	if (itemType == "movie clip" || itemType == "component") {
		libraryItem.itemType == "movie clip"
		var left = 99999999;
		var top = 99999999;
		
		for each (var layer in libraryItem.timeline.layers) {
			if (layer.layerType != "normal") continue;
			for each (var element in layer.frames[0].elements) {
				if (element.left < left) {
					left = element.left;
				}
				if (element.top < top) {
					top = element.top;
				}
			}
		}
		return {x: -left, y: -top};
	} else {
		return {x: 0, y: 0};
	}
}

fl.trace(JSON.stringify(data));





