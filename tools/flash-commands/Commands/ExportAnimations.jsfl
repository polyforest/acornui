var originCache = {};
var folder = document.pathURI.substring(0, document.pathURI.lastIndexOf(document.name));
fl.runScript(fl.configURI + 'Javascript/JSON.jsfl');
//JSON.prettyPrint = true;
fl.outputPanel.clear();
var data = {library: {}, easings: {}};
data.library["__document"] = {type: "animation", timeline: processTimeline(document.timelines[0], "__document")};

for (var i = 0; i < document.library.items.length; i++) {
	var item = document.library.items[i];
	// Library items marked as linkageExportForAS are considered assets and will be converted to a png.
	if (item.itemType === "movie clip") {
		if (item.linkageExportForAS) {
			var name = item.name;
			if (name.indexOf("resources/") === 0) name = name.substring("resources/".length);
			var unpackedIndex = name.indexOf("_unpacked");
			if (unpackedIndex === -1) {
				data.library[item.name] = {type: "image", path: name + ".png"};
			} else {
				var regionName = name.substring(name.lastIndexOf("/") + 1);
				var atlasPath = name.substring(0, unpackedIndex) + ".json";
				data.library[item.name] = {type: "atlas", regionName: regionName, atlasPath: atlasPath};
			}
		} else {
			data.library[item.name] = {type: "animation", timeline: processTimeline(item.timeline, item.name)};
		}
	}
}

/**
 * Creates a data object representing a timeline.
 * @param timeline The Timeline object.
 * @param movieName The name of the movie (for debugging purposes)
 */
function processTimeline(timeline, movieName) {
	var timelineData = {duration: round(timeline.frameCount / document.frameRate), labels: {}, layers: []};
	for (var layerIndex = 0; layerIndex < timeline.layers.length; layerIndex++) {
		var layer = timeline.layers[layerIndex];
		if (layer.layerType !== "normal") continue;

		// Search the keyframes for the movie clip instance, and ensure there is only one per layer.
		var symbolName = null;
		for (var frameIndex = 0; frameIndex < layer.frames.length; frameIndex++) {
			var frame = layer.frames[frameIndex];
			if (frame.startFrame === frameIndex && frame.elements.length > 0) {
				if (frame.elements.length > 1) throw Error("Cannot have more than one element per layer. movie: " + movieName + " layer: " + layer.name + " frame: " + frameIndex);
				var element = frame.elements[0];
				if (element.libraryItem == null || element.libraryItem.itemType === "bitmap") continue;
				//if (element.libraryItem == null) throw Error("Error on frame " + frameIndex + " Only movie clips are currently supported. symbolType: " + element.symbolType + ", movie: " + movieName + " layer: " + layer.name + " frame: " + frameIndex);
				var name = element.libraryItem.name;
				if (name !== symbolName) {
					if (symbolName != null) throw Error("Cannot have different movie clips on one layer. movie: " + movieName + " layer: " + layer.name + " frame: " + frameIndex);
					symbolName = name;
				}
			}

			// Frame labels
			if (frame.startFrame === frameIndex && frame.labelType === "name") {
				timelineData.labels[frame.name] = frame.startFrame / document.frameRate;
			}
		}
		if (symbolName == null) continue; // No symbol on that layer.
		var layerData = {name: layer.name, symbolName: symbolName, visible: layer.visible, keyFrames: []};

		var lastKeyFrameData = null;
		for (var frameIndex = 0; frameIndex < layer.frames.length; frameIndex++) {
			var frame = layer.frames[frameIndex];

			if (frame.startFrame === frameIndex) {
				// Keyframe.
				var keyFrameData = createFrameData(frame);
				//removeUnchanged(keyFrameData, lastKeyFrameData);
				layerData.keyFrames.push(keyFrameData);
				lastKeyFrameData = keyFrameData;
			}
		}
		if (timeline.frameCount > layer.frames.length) {
			// Add a virtual blank keyframe to the end if the layer is shorter than the timeline
			var blankFrameData = {
				time: round(layer.frames.length / document.frameRate),
				easings: {},
				props: {VISIBLE: {value: 0, easing: null}}
			};
			layerData.keyFrames.push(blankFrameData);
		}

		timelineData.layers.push(layerData);
	}
	return timelineData;
}

function createFrameData(frame) {
	//fl.trace("----------------------FRAME-----------------------------");
	var frameData = {time: round(frame.startFrame / document.frameRate), easings: {}};

	if (frame.tweenType === "motion") {
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

	var element = null;
	// Find first element with a libraryItem
	for each (i in frame.elements) {
		if (i.libraryItem != null) {
			element = i;
			break;
		}
	}
	var rawProps = createPropsData(element);// The properties before associating them with easing functions.

	frameData.props = {};
	var propsEasingMap;
	if (frame.tweenType === "motion") {
		if (frame.useSingleEaseCurve) {
			propsEasingMap = {
				VISIBLE: null,
				ORIGIN_X: null,
				ORIGIN_Y: null,
				X: "all",
				Y: "all",
				SCALE_X: "all",
				SCALE_Y: "all",
				SHEAR_XZ: "all",
				SHEAR_YZ: "all",
				ROTATION_Z: "all",
				COLOR_R: "all",
				COLOR_G: "all",
				COLOR_B: "all",
				COLOR_A: "all"
			};
		} else {
			propsEasingMap = {
				VISIBLE: null,
				ORIGIN_X: null,
				ORIGIN_Y: null,
				X: "position",
				Y: "position",
				SCALE_X: "scale",
				SCALE_Y: "scale",
				SHEAR_XZ: "rotation",
				SHEAR_YZ: "rotation",
				ROTATION_Z: "rotation",
				COLOR_R: "color",
				COLOR_G: "color",
				COLOR_B: "color",
				COLOR_A: "color"
			};
		}
	} else {
		propsEasingMap = {
			VISIBLE: null,
			ORIGIN_X: null,
			ORIGIN_Y: null,
			X: null,
			Y: null,
			SCALE_X: null,
			SCALE_Y: null,
			SHEAR_XZ: null,
			SHEAR_YZ: null,
			COLOR_R: null,
			COLOR_G: null,
			COLOR_B: null,
			COLOR_A: null
		};
	}
	for (var all in rawProps) {
		var easing = propsEasingMap[all];
		frameData.props[all] = {value: rawProps[all], easing: easing};
		if (frameData.props[all].easing == null)
			delete frameData.props[all].easing;
	}
	return frameData;
}

function createPropsData(element) {
	if (element == null) {
		return {VISIBLE: 0};
	}
	var props = {VISIBLE: 1};

	var origin = getOrigin(element);

	// Registration point ("origin") in acorn is local to the component.
	var inv = fl.Math.invertMatrix(element.matrix)
	var p2 = prj(element.transformX, element.transformY, inv);

	props.ORIGIN_X = round(p2.x + origin.x);
	props.ORIGIN_Y = round(p2.y + origin.y);

	// Translation in acorn is relative to the origin.
	props.X = round(element.transformX);
	props.Y = round(element.transformY);

	var degRad = Math.PI / 180;

	if (isNaN(element.rotation)) {
		var r = 0;
		if (element.skewY > 0 && element.skewX > 0) {
			r = Math.min(element.skewX, element.skewY);
		} else if (element.skewY < 0 && element.skewX < 0) {
			r = Math.max(element.skewX, element.skewY);
		}
		props.SCALE_X = round(element.scaleX * Math.cos((element.skewY - r) * degRad));
		props.SCALE_Y = round(element.scaleY * Math.cos((element.skewX - r) * degRad));

		props.SHEAR_XZ = -round((element.skewX - r) * degRad);
		props.SHEAR_YZ = round((element.skewY - r) * degRad);
		props.ROTATION_Z = round(r * degRad);

	} else {
		props.SHEAR_XZ = 0;
		props.SHEAR_YZ = 0;
		props.SCALE_X = round(element.scaleX);
		props.SCALE_Y = round(element.scaleY);
		props.ROTATION_Z = round(element.rotation * degRad);
	}
	props.COLOR_A = element.colorAlphaPercent / 100;
	props.COLOR_R = element.colorRedPercent / 100;
	props.COLOR_G = element.colorGreenPercent / 100;
	props.COLOR_B = element.colorBluePercent / 100;
	return props;
}

function removeUnchanged(keyFrameData, lastKeyFrameData) {
	if (lastKeyFrameData == null) return;
	for (var all in keyFrameData.props) {
		if (lastKeyFrameData.props[all] != null &&
			keyFrameData.props[all].value === lastKeyFrameData.props[all].value) {
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
	return {x: x2, y: y2};
}

function rot(x, y, matrix) {
	var x2 = matrix.a * x + matrix.c * y;
	var y2 = matrix.b * x + matrix.d * y;
	return {x: x2, y: y2};
}

function getOrigin(element) {
	if (originCache[element.libraryItem.name] == null) {
		originCache[element.libraryItem.name] = _getOrigin(element.libraryItem);
	}
	return originCache[element.libraryItem.name];
}

function _getOrigin(libraryItem) {
	var itemType = libraryItem.itemType;
	if (itemType === "movie clip" || itemType === "component") {
		var left = 99999999;
		var top = 99999999;
		var hasElement = false;

		for each (var layer in libraryItem.timeline.layers) {
			if (layer.layerType !== "normal") continue;
			for each (var element in layer.frames[0].elements) {
				hasElement = true;
				if (element.left < left) {
					left = element.left;
				}
				if (element.top < top) {
					top = element.top;
				}
			}
		}
		if (hasElement) {
			return {x: -left, y: -top};
		} else {
			return {x: 0, y: 0};
		}
	} else {
		return {x: 0, y: 0};
	}
}

var json = JSON.stringify(data);
makeDirs("resources/assets/anim");
var animName = document.name.substring(0, document.name.length - 4) + ".json";
FLfile.write(folder + "resources/assets/anim/" + animName, json);

fl.trace("Written animation to: " + folder + "resources/assets/anim/" + animName);

function makeDirs(path) {
	var split = path.split("/");
	var parent = folder;
	for (var i = 0; i < split.length; i++) {
		parent += split[i] + "/";
		if (!FLfile.exists(parent)) {
			FLfile.createFolder(parent);
		}
	}
}