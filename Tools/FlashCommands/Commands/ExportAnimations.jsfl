var originCache = {};
fl.runScript(fl.configURI + 'Javascript/JSON.jsfl')
JSON.prettyPrint = true;
fl.outputPanel.clear();
var data = { library: {}, easings: {}, animations: [] };
data.animations.push({ name: "document", timeline: processTimeline(document.timelines[0]) });

for (var i = 0; i < document.library.items.length; i++) {
	var item = document.library.items[i];
	// Library items marked as linkageExportForAS are considered assets and will be converted to a png.
	if (item.itemType == "movie clip") {
		if (item.linkageExportForAS) {
			var name = item.name;
			var unpackedIndex = name.indexOf("_unpacked")
			if (unpackedIndex == -1) {
				data.library[item.name] = { type: "image", path: item.name + ".png" };
			} else {
				var regionName = name.substring(name.lastIndexOf("/") + 1);
				var atlasPath = name.substring(0, unpackedIndex) + ".json";
				data.library[item.name] = { type: "atlas", regionName: regionName, atlasPath: atlasPath };
			}
		} else {
			data.animations.push({ name: item.name, timeline: processTimeline(item.timeline) });
			data.library[item.name] = { type: "animation" };
		}
	}
	
}

function processTimeline(timeline) {
	var timelineData = { layers: [] };
	for (var layerIndex = 0; layerIndex < timeline.layers.length; layerIndex++) {
		var layer = timeline.layers[layerIndex];
		
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
	var frameData = { time: round(frame.startFrame / document.frameRate, 1000), easings: {} };
	
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
			for each (var point in customEase) {
				points.push(round(point.x, 1000));
				points.push(round(point.y, 1000));
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
			propsEasingMap = { visible: null, originX: null, originY: null, x: "all", y: "all", scaleX: "all", scaleY: "all", skewXZ: "all", skewYZ: "all", colorR: "all", colorG: "all", colorB: "all", colorA: "all" };
		} else {
			propsEasingMap = { visible: null, originX: null, originY: null, x: "position", y: "position", scaleX: "scale", scaleY: "scale",  skewXZ: "rotation", skewYZ: "rotation", colorR: "color", colorG: "color", colorB: "color", colorA: "color" };
		}
	} else {
		propsEasingMap = { visible: null, originX: null, originY: null, x: null, y: null, scaleX: null, scaleY: null, skewXZ: null, skewYZ: null, colorR: null, colorG: null, colorB: null, colorA: null };
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
	props.originX = round(p2.x + origin.x, 1000);
	props.originY = round(p2.y + origin.y, 1000);
	
	// Translation in acorn is relative to the origin.
	props.x = round(element.transformX, 1000);
	props.y = round(element.transformY, 1000);
	
	//props.rotationZ = round(element.rotation, 1000);
	props.scaleX = round(element.scaleX, 1000);
	props.scaleY = round(element.scaleY, 1000);
	
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

function round(n, m) {
	return Math.round(n * m) / m;
}

/**
 * Multiplies an x,y coordinate by the given matrix.
 */
function prj(x, y, matrix) {
	var x2 = matrix.a * x + matrix.b * y + matrix.tx;
	var y2 = matrix.c * x + matrix.d * y + matrix.ty;
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





