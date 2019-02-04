/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.serialization

import org.junit.Test

class XmlTest {

	@Test fun bbb() {

		// language=HTML
		val xml1 = """<!DOCTYPE html>
<html>
<head>
	<title>Matrix Precise</title>
	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">

	<style>
		html, body {
			width: 100%;
			height: 100%;
			margin: 0;
			padding: 0;
			background-color: black;
			overflow: hidden;
		}
		.loadingMsg {
			color: #bdbdbd;
			font-size: 36px;
			font-family: sans-serif;
			margin-top: 100px;
			width: 100%;
			text-align: center;
		}
	</style>
</head>
<body>

<div id="mpSiteRoot" style="width: 100%; height: 100%;">
	<div class="loadingMsg">Loading...</div>
</div>
<script src="lib/files.js?version=1508793104592"></script>
<script data-main="lib/mpSite" src="lib/require.js?version=1508793104562"></script>

</body>
</html>"""

		XmlSerializer.read(xml1).properties()

	}
}