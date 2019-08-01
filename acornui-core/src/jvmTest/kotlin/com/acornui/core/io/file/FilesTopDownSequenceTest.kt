/*
 * Copyright 2018 Poly Forest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.core.io.file

import com.acornui.collection.isSorted
import com.acornui.io.file.FilesManifest
import com.acornui.serialization.parseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesTopDownSequenceTest {

	@Test
	fun walk() {
		val jsonStr = """
			{
	"files": [
		{
			"path": "assets/Overview2263.csv",
			"modified": 1525269404081,
			"size": 231269,
			"mimeType": "application/vnd.ms-excel"
		},
		{
			"path": "assets/Overview2263.json",
			"modified": 1493668608599,
			"size": 10089,
			"mimeType": null
		},
		{
			"path": "assets/Overview22630.png",
			"modified": 1493668608601,
			"size": 43004,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview22631.png",
			"modified": 1493668608604,
			"size": 104554,
			"mimeType": "image/png"
		},
		{
			"path": "assets/build.txt",
			"modified": 1525269404222,
			"size": 3,
			"mimeType": "text/plain"
		},
		{
			"path": "assets/countries.tsv",
			"modified": 1493157224402,
			"size": 13319,
			"mimeType": null
		},
		{
			"path": "assets/files.json",
			"modified": 1525269256031,
			"size": 11996,
			"mimeType": null
		},
		{
			"path": "assets/flags.json",
			"modified": 1493157224412,
			"size": 43684,
			"mimeType": null
		},
		{
			"path": "assets/flags0.png",
			"modified": 1493157224412,
			"size": 44553,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/1ghost.png.png",
			"modified": 1525269404084,
			"size": 1422,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/2 ghost.png.png",
			"modified": 1525269404087,
			"size": 3252,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/5 ghost.png.png",
			"modified": 1525269404090,
			"size": 3231,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/ButtonPlaceHolder.png",
			"modified": 1525269404097,
			"size": 284,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/MachineButtonPlaceHolder.png",
			"modified": 1525269404207,
			"size": 298,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/Tunnel 2 ghost small.png.png",
			"modified": 1525269404213,
			"size": 675,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/WaEx 1 ghost.png.png",
			"modified": 1525269404216,
			"size": 1849,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/_packSettings.json",
			"modified": 1525269404219,
			"size": 535,
			"mimeType": null
		},
		{
			"path": "assets/Overview2263_unpacked/deviceCloser.bmp.png",
			"modified": 1525269404099,
			"size": 63904,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machinessss.png",
			"modified": 1525269404210,
			"size": 224,
			"mimeType": "image/png"
		},
		{
			"path": "assets/images/2017-01-29.jpg",
			"modified": 1493157224412,
			"size": 114071,
			"mimeType": "image/jpeg"
		},
		{
			"path": "assets/images/image128.jpg",
			"modified": 1493157224412,
			"size": 34850,
			"mimeType": "image/jpeg"
		},
		{
			"path": "assets/images/image2.jpg",
			"modified": 1493157224412,
			"size": 34074,
			"mimeType": "image/jpeg"
		},
		{
			"path": "assets/images/pigbar.jpg",
			"modified": 1493157224412,
			"size": 131041,
			"mimeType": "image/jpeg"
		},
		{
			"path": "assets/res/datagrid.properties",
			"modified": 1493157224412,
			"size": 805,
			"mimeType": null
		},
		{
			"path": "assets/res/datagrid_de_DE.properties",
			"modified": 1493157224412,
			"size": 834,
			"mimeType": null
		},
		{
			"path": "assets/res/datagrid_en_US.properties",
			"modified": 1493157224422,
			"size": 810,
			"mimeType": null
		},
		{
			"path": "assets/res/datagrid_fr_FR.properties",
			"modified": 1493157224422,
			"size": 817,
			"mimeType": null
		},
		{
			"path": "assets/res/ui.properties",
			"modified": 1493668608605,
			"size": 0,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/uiskin.json",
			"modified": 1493668608638,
			"size": 5628,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/uiskin0.png",
			"modified": 1493668608640,
			"size": 46581,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/verdana_14.bmfc",
			"modified": 1493668608643,
			"size": 747,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/verdana_14.fnt",
			"modified": 1493668608646,
			"size": 26502,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/verdana_14_bold.fnt",
			"modified": 1493668608648,
			"size": 22077,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/verdana_14_bold_italic.fnt",
			"modified": 1493668608651,
			"size": 22084,
			"mimeType": null
		},
		{
			"path": "assets/uiskin/verdana_14_italic.fnt",
			"modified": 1493668608653,
			"size": 22079,
			"mimeType": null
		},
		{
			"path": "assets/Overview2263_unpacked/Bag Assets/BagThing.png",
			"modified": 1525269404094,
			"size": 417,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/MachineAssets/MachineThing.png",
			"modified": 1525269404204,
			"size": 406,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/Indexer.png",
			"modified": 1525269404106,
			"size": 1399,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/LiveRailSingle.png",
			"modified": 1525269404109,
			"size": 804,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/OpenerStyleA.png",
			"modified": 1525269404111,
			"size": 2008,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/Sensor.png",
			"modified": 1525269404114,
			"size": 198,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/StopDefaultDown.png",
			"modified": 1525269404119,
			"size": 934,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/StopDefaultUp.png",
			"modified": 1525269404122,
			"size": 891,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/conveyor_arrow.png",
			"modified": 1525269404103,
			"size": 188,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/stabilizer.png",
			"modified": 1525269404116,
			"size": 143,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutDevices/threeway_object.png",
			"modified": 1525269404125,
			"size": 106,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/Curve45_short.png",
			"modified": 1525269404128,
			"size": 340,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/Curve90LowerLeft.png",
			"modified": 1525269404131,
			"size": 724,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/Curve90LowerRight.png",
			"modified": 1525269404135,
			"size": 690,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/Curve90UpperLeft.png",
			"modified": 1525269404138,
			"size": 677,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/Curve90UpperRight.png",
			"modified": 1525269404141,
			"size": 727,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/RailHorizontal.png",
			"modified": 1525269404144,
			"size": 125,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutRail/RailVertical.png",
			"modified": 1525269404146,
			"size": 117,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayCurve_shortFat.png",
			"modified": 1525269404153,
			"size": 2516,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayCurve_tallFat.png",
			"modified": 1525269404156,
			"size": 2563,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayCurve_tallSkinny.png",
			"modified": 1525269404158,
			"size": 2613,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayStraight45B.png",
			"modified": 1525269404161,
			"size": 2093,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayStraight_shortFat.png",
			"modified": 1525269404164,
			"size": 2179,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayStraight_shortSkinny.png",
			"modified": 1525269404166,
			"size": 2132,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayStraight_tallFat.png",
			"modified": 1525269404169,
			"size": 2233,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch2wayStraight_tallSkinny.png",
			"modified": 1525269404172,
			"size": 2078,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch3WayLeft.png",
			"modified": 1525269404174,
			"size": 6329,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch3WayLeft2.png",
			"modified": 1525269404177,
			"size": 5458,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch3WayLeftB.png",
			"modified": 1525269404180,
			"size": 6631,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/Switch3WayRightB.png",
			"modified": 1525269404183,
			"size": 6358,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/layoutSwitches/cthreeway.png",
			"modified": 1525269404150,
			"size": 739,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machine objects/Conveyor Horizontal.png.png",
			"modified": 1525269404189,
			"size": 221,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machine objects/Conveyor Vertical.png.png",
			"modified": 1525269404193,
			"size": 756,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machine objects/Milnor Dryer.png.png",
			"modified": 1525269404196,
			"size": 2165,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machine objects/Table Dumper.png.png",
			"modified": 1525269404200,
			"size": 2511,
			"mimeType": "image/png"
		},
		{
			"path": "assets/Overview2263_unpacked/machine objects/clean12.png.png",
			"modified": 1525269404186,
			"size": 3455,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Alias.png",
			"modified": 1493668608609,
			"size": 2983,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/AllScroll.png",
			"modified": 1493668608612,
			"size": 2918,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Cell.png",
			"modified": 1493668608614,
			"size": 2886,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Copy.png",
			"modified": 1493668608616,
			"size": 2983,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Crosshair.png",
			"modified": 1493668608618,
			"size": 2856,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Help.png",
			"modified": 1493668608620,
			"size": 2940,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/IBeam.png",
			"modified": 1493668608623,
			"size": 2862,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Move.png",
			"modified": 1493668608625,
			"size": 2919,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/NotAllowed.png",
			"modified": 1493668608628,
			"size": 2961,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/PointerWait.png",
			"modified": 1493668608630,
			"size": 3029,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/ResizeNE.png",
			"modified": 1493668608632,
			"size": 2884,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/ResizeSE.png",
			"modified": 1493668608634,
			"size": 2891,
			"mimeType": "image/png"
		},
		{
			"path": "assets/uiskin/cursors/Wait.png",
			"modified": 1493668608636,
			"size": 2958,
			"mimeType": "image/png"
		}
	]
}
		"""

		val list = ArrayList<FileEntry>()
		val manifest = parseJson(jsonStr, FilesManifest.serializer())
		val files = FilesImpl(manifest)
		files.getDir("")!!.walkFilesTopDown().forEach {
			fileEntry ->
			list.add(fileEntry)
		}
		assertEquals(files.getDir("")!!.totalFiles, list.size)

		assertTrue(list.isSorted())
	}


}

