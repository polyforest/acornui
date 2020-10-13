/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("unused")

package com.acornui.google

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
import com.acornui.component.WithNode
import com.acornui.component.span
import com.acornui.component.style.CommonStyleTags
import com.acornui.component.style.CssClass
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.*
import com.acornui.google.IconButtonStyle.iconButton
import com.acornui.google.MaterialIconsCss.materialIconsStyleTag
import com.acornui.properties.afterChange
import com.acornui.skins.CssProps
import org.w3c.dom.HTMLElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A list of icons provided by Google's material design icon set.
 * https://material.io/resources/icons
 */
object Icons {
	const val ROTATION_3D = 0xe84d
	const val AC_UNIT = 0xeb3b
	const val ACCESS_ALARM = 0xe190
	const val ACCESS_ALARMS = 0xe191
	const val ACCESS_TIME = 0xe192
	const val ACCESSIBILITY = 0xe84e
	const val ACCESSIBLE = 0xe914
	const val ACCOUNT_BALANCE = 0xe84f
	const val ACCOUNT_BALANCE_WALLET = 0xe850
	const val ACCOUNT_BOX = 0xe851
	const val ACCOUNT_CIRCLE = 0xe853
	const val ADB = 0xe60e
	const val ADD = 0xe145
	const val ADD_A_PHOTO = 0xe439
	const val ADD_ALARM = 0xe193
	const val ADD_ALERT = 0xe003
	const val ADD_BOX = 0xe146
	const val ADD_CIRCLE = 0xe147
	const val ADD_CIRCLE_OUTLINE = 0xe148
	const val ADD_LOCATION = 0xe567
	const val ADD_SHOPPING_CART = 0xe854
	const val ADD_TO_PHOTOS = 0xe39d
	const val ADD_TO_QUEUE = 0xe05c
	const val ADJUST = 0xe39e
	const val AIRLINE_SEAT_FLAT = 0xe630
	const val AIRLINE_SEAT_FLAT_ANGLED = 0xe631
	const val AIRLINE_SEAT_INDIVIDUAL_SUITE = 0xe632
	const val AIRLINE_SEAT_LEGROOM_EXTRA = 0xe633
	const val AIRLINE_SEAT_LEGROOM_NORMAL = 0xe634
	const val AIRLINE_SEAT_LEGROOM_REDUCED = 0xe635
	const val AIRLINE_SEAT_RECLINE_EXTRA = 0xe636
	const val AIRLINE_SEAT_RECLINE_NORMAL = 0xe637
	const val AIRPLANEMODE_ACTIVE = 0xe195
	const val AIRPLANEMODE_INACTIVE = 0xe194
	const val AIRPLAY = 0xe055
	const val AIRPORT_SHUTTLE = 0xeb3c
	const val ALARM = 0xe855
	const val ALARM_ADD = 0xe856
	const val ALARM_OFF = 0xe857
	const val ALARM_ON = 0xe858
	const val ALBUM = 0xe019
	const val ALL_INCLUSIVE = 0xeb3d
	const val ALL_OUT = 0xe90b
	const val ANDROID = 0xe859
	const val ANNOUNCEMENT = 0xe85a
	const val APPS = 0xe5c3
	const val ARCHIVE = 0xe149
	const val ARROW_BACK = 0xe5c4
	const val ARROW_DOWNWARD = 0xe5db
	const val ARROW_DROP_DOWN = 0xe5c5
	const val ARROW_DROP_DOWN_CIRCLE = 0xe5c6
	const val ARROW_DROP_UP = 0xe5c7
	const val ARROW_FORWARD = 0xe5c8
	const val ARROW_UPWARD = 0xe5d8
	const val ART_TRACK = 0xe060
	const val ASPECT_RATIO = 0xe85b
	const val ASSESSMENT = 0xe85c
	const val ASSIGNMENT = 0xe85d
	const val ASSIGNMENT_IND = 0xe85e
	const val ASSIGNMENT_LATE = 0xe85f
	const val ASSIGNMENT_RETURN = 0xe860
	const val ASSIGNMENT_RETURNED = 0xe861
	const val ASSIGNMENT_TURNED_IN = 0xe862
	const val ASSISTANT = 0xe39f
	const val ASSISTANT_PHOTO = 0xe3a0
	const val ATTACH_FILE = 0xe226
	const val ATTACH_MONEY = 0xe227
	const val ATTACHMENT = 0xe2bc
	const val AUDIOTRACK = 0xe3a1
	const val AUTORENEW = 0xe863
	const val AV_TIMER = 0xe01b
	const val BACKSPACE = 0xe14a
	const val BACKUP = 0xe864
	const val BATTERY_ALERT = 0xe19c
	const val BATTERY_CHARGING_FULL = 0xe1a3
	const val BATTERY_FULL = 0xe1a4
	const val BATTERY_STD = 0xe1a5
	const val BATTERY_UNKNOWN = 0xe1a6
	const val BEACH_ACCESS = 0xeb3e
	const val BEENHERE = 0xe52d
	const val BLOCK = 0xe14b
	const val BLUETOOTH = 0xe1a7
	const val BLUETOOTH_AUDIO = 0xe60f
	const val BLUETOOTH_CONNECTED = 0xe1a8
	const val BLUETOOTH_DISABLED = 0xe1a9
	const val BLUETOOTH_SEARCHING = 0xe1aa
	const val BLUR_CIRCULAR = 0xe3a2
	const val BLUR_LINEAR = 0xe3a3
	const val BLUR_OFF = 0xe3a4
	const val BLUR_ON = 0xe3a5
	const val BOOK = 0xe865
	const val BOOKMARK = 0xe866
	const val BOOKMARK_BORDER = 0xe867
	const val BORDER_ALL = 0xe228
	const val BORDER_BOTTOM = 0xe229
	const val BORDER_CLEAR = 0xe22a
	const val BORDER_COLOR = 0xe22b
	const val BORDER_HORIZONTAL = 0xe22c
	const val BORDER_INNER = 0xe22d
	const val BORDER_LEFT = 0xe22e
	const val BORDER_OUTER = 0xe22f
	const val BORDER_RIGHT = 0xe230
	const val BORDER_STYLE = 0xe231
	const val BORDER_TOP = 0xe232
	const val BORDER_VERTICAL = 0xe233
	const val BRANDING_WATERMARK = 0xe06b
	const val BRIGHTNESS_1 = 0xe3a6
	const val BRIGHTNESS_2 = 0xe3a7
	const val BRIGHTNESS_3 = 0xe3a8
	const val BRIGHTNESS_4 = 0xe3a9
	const val BRIGHTNESS_5 = 0xe3aa
	const val BRIGHTNESS_6 = 0xe3ab
	const val BRIGHTNESS_7 = 0xe3ac
	const val BRIGHTNESS_AUTO = 0xe1ab
	const val BRIGHTNESS_HIGH = 0xe1ac
	const val BRIGHTNESS_LOW = 0xe1ad
	const val BRIGHTNESS_MEDIUM = 0xe1ae
	const val BROKEN_IMAGE = 0xe3ad
	const val BRUSH = 0xe3ae
	const val BUBBLE_CHART = 0xe6dd
	const val BUG_REPORT = 0xe868
	const val BUILD = 0xe869
	const val BURST_MODE = 0xe43c
	const val BUSINESS = 0xe0af
	const val BUSINESS_CENTER = 0xeb3f
	const val CACHED = 0xe86a
	const val CAKE = 0xe7e9
	const val CALL = 0xe0b0
	const val CALL_END = 0xe0b1
	const val CALL_MADE = 0xe0b2
	const val CALL_MERGE = 0xe0b3
	const val CALL_MISSED = 0xe0b4
	const val CALL_MISSED_OUTGOING = 0xe0e4
	const val CALL_RECEIVED = 0xe0b5
	const val CALL_SPLIT = 0xe0b6
	const val CALL_TO_ACTION = 0xe06c
	const val CAMERA = 0xe3af
	const val CAMERA_ALT = 0xe3b0
	const val CAMERA_ENHANCE = 0xe8fc
	const val CAMERA_FRONT = 0xe3b1
	const val CAMERA_REAR = 0xe3b2
	const val CAMERA_ROLL = 0xe3b3
	const val CANCEL = 0xe5c9
	const val CARD_GIFTCARD = 0xe8f6
	const val CARD_MEMBERSHIP = 0xe8f7
	const val CARD_TRAVEL = 0xe8f8
	const val CASINO = 0xeb40
	const val CAST = 0xe307
	const val CAST_CONNECTED = 0xe308
	const val CENTER_FOCUS_STRONG = 0xe3b4
	const val CENTER_FOCUS_WEAK = 0xe3b5
	const val CHANGE_HISTORY = 0xe86b
	const val CHAT = 0xe0b7
	const val CHAT_BUBBLE = 0xe0ca
	const val CHAT_BUBBLE_OUTLINE = 0xe0cb
	const val CHECK = 0xe5ca
	const val CHECK_BOX = 0xe834
	const val CHECK_BOX_OUTLINE_BLANK = 0xe835
	const val CHECK_CIRCLE = 0xe86c
	const val CHEVRON_LEFT = 0xe5cb
	const val CHEVRON_RIGHT = 0xe5cc
	const val CHILD_CARE = 0xeb41
	const val CHILD_FRIENDLY = 0xeb42
	const val CHROME_READER_MODE = 0xe86d
	const val CLASS_ICON = 0xe86e
	const val CLEAR = 0xe14c
	const val CLEAR_ALL = 0xe0b8
	const val CLOSE = 0xe5cd
	const val CLOSED_CAPTION = 0xe01c
	const val CLOUD = 0xe2bd
	const val CLOUD_CIRCLE = 0xe2be
	const val CLOUD_DONE = 0xe2bf
	const val CLOUD_DOWNLOAD = 0xe2c0
	const val CLOUD_OFF = 0xe2c1
	const val CLOUD_QUEUE = 0xe2c2
	const val CLOUD_UPLOAD = 0xe2c3
	const val CODE = 0xe86f
	const val COLLECTIONS = 0xe3b6
	const val COLLECTIONS_BOOKMARK = 0xe431
	const val COLOR_LENS = 0xe3b7
	const val COLORIZE = 0xe3b8
	const val COMMENT = 0xe0b9
	const val COMPARE = 0xe3b9
	const val COMPARE_ARROWS = 0xe915
	const val COMPUTER = 0xe30a
	const val CONFIRMATION_NUMBER = 0xe638
	const val CONTACT_MAIL = 0xe0d0
	const val CONTACT_PHONE = 0xe0cf
	const val CONTACTS = 0xe0ba
	const val CONTENT_COPY = 0xe14d
	const val CONTENT_CUT = 0xe14e
	const val CONTENT_PASTE = 0xe14f
	const val CONTROL_POINT = 0xe3ba
	const val CONTROL_POINT_DUPLICATE = 0xe3bb
	const val COPYRIGHT = 0xe90c
	const val CREATE = 0xe150
	const val CREATE_NEW_FOLDER = 0xe2cc
	const val CREDIT_CARD = 0xe870
	const val CROP = 0xe3be
	const val CROP_16_9 = 0xe3bc
	const val CROP_3_2 = 0xe3bd
	const val CROP_5_4 = 0xe3bf
	const val CROP_7_5 = 0xe3c0
	const val CROP_DIN = 0xe3c1
	const val CROP_FREE = 0xe3c2
	const val CROP_LANDSCAPE = 0xe3c3
	const val CROP_ORIGINAL = 0xe3c4
	const val CROP_PORTRAIT = 0xe3c5
	const val CROP_ROTATE = 0xe437
	const val CROP_SQUARE = 0xe3c6
	const val DASHBOARD = 0xe871
	const val DATA_USAGE = 0xe1af
	const val DATE_RANGE = 0xe916
	const val DEHAZE = 0xe3c7
	const val DELETE = 0xe872
	const val DELETE_FOREVER = 0xe92b
	const val DELETE_SWEEP = 0xe16c
	const val DESCRIPTION = 0xe873
	const val DESKTOP_MAC = 0xe30b
	const val DESKTOP_WINDOWS = 0xe30c
	const val DETAILS = 0xe3c8
	const val DEVELOPER_BOARD = 0xe30d
	const val DEVELOPER_MODE = 0xe1b0
	const val DEVICE_HUB = 0xe335
	const val DEVICES = 0xe1b1
	const val DEVICES_OTHER = 0xe337
	const val DIALER_SIP = 0xe0bb
	const val DIALPAD = 0xe0bc
	const val DIRECTIONS = 0xe52e
	const val DIRECTIONS_BIKE = 0xe52f
	const val DIRECTIONS_BOAT = 0xe532
	const val DIRECTIONS_BUS = 0xe530
	const val DIRECTIONS_CAR = 0xe531
	const val DIRECTIONS_RAILWAY = 0xe534
	const val DIRECTIONS_RUN = 0xe566
	const val DIRECTIONS_SUBWAY = 0xe533
	const val DIRECTIONS_TRANSIT = 0xe535
	const val DIRECTIONS_WALK = 0xe536
	const val DISC_FULL = 0xe610
	const val DNS = 0xe875
	const val DO_NOT_DISTURB = 0xe612
	const val DO_NOT_DISTURB_ALT = 0xe611
	const val DO_NOT_DISTURB_OFF = 0xe643
	const val DO_NOT_DISTURB_ON = 0xe644
	const val DOCK = 0xe30e
	const val DOMAIN = 0xe7ee
	const val DONE = 0xe876
	const val DONE_ALL = 0xe877
	const val DONUT_LARGE = 0xe917
	const val DONUT_SMALL = 0xe918
	const val DRAFTS = 0xe151
	const val DRAG_HANDLE = 0xe25d
	const val DRIVE_ETA = 0xe613
	const val DVR = 0xe1b2
	const val EDIT = 0xe3c9
	const val EDIT_LOCATION = 0xe568
	const val EJECT = 0xe8fb
	const val EMAIL = 0xe0be
	const val ENHANCED_ENCRYPTION = 0xe63f
	const val EQUALIZER = 0xe01d
	const val ERROR = 0xe000
	const val ERROR_OUTLINE = 0xe001
	const val EURO_SYMBOL = 0xe926
	const val EV_STATION = 0xe56d
	const val EVENT = 0xe878
	const val EVENT_AVAILABLE = 0xe614
	const val EVENT_BUSY = 0xe615
	const val EVENT_NOTE = 0xe616
	const val EVENT_SEAT = 0xe903
	const val EXIT_TO_APP = 0xe879
	const val EXPAND_LESS = 0xe5ce
	const val EXPAND_MORE = 0xe5cf
	const val EXPLICIT = 0xe01e
	const val EXPLORE = 0xe87a
	const val EXPOSURE = 0xe3ca
	const val EXPOSURE_NEG_1 = 0xe3cb
	const val EXPOSURE_NEG_2 = 0xe3cc
	const val EXPOSURE_PLUS_1 = 0xe3cd
	const val EXPOSURE_PLUS_2 = 0xe3ce
	const val EXPOSURE_ZERO = 0xe3cf
	const val EXTENSION = 0xe87b
	const val FACE = 0xe87c
	const val FAST_FORWARD = 0xe01f
	const val FAST_REWIND = 0xe020
	const val FAVORITE = 0xe87d
	const val FAVORITE_BORDER = 0xe87e
	const val FEATURED_PLAY_LIST = 0xe06d
	const val FEATURED_VIDEO = 0xe06e
	const val FEEDBACK = 0xe87f
	const val FIBER_DVR = 0xe05d
	const val FIBER_MANUAL_RECORD = 0xe061
	const val FIBER_NEW = 0xe05e
	const val FIBER_PIN = 0xe06a
	const val FIBER_SMART_RECORD = 0xe062
	const val FILE_DOWNLOAD = 0xe2c4
	const val FILE_UPLOAD = 0xe2c6
	const val FILTER = 0xe3d3
	const val FILTER_1 = 0xe3d0
	const val FILTER_2 = 0xe3d1
	const val FILTER_3 = 0xe3d2
	const val FILTER_4 = 0xe3d4
	const val FILTER_5 = 0xe3d5
	const val FILTER_6 = 0xe3d6
	const val FILTER_7 = 0xe3d7
	const val FILTER_8 = 0xe3d8
	const val FILTER_9 = 0xe3d9
	const val FILTER_9_PLUS = 0xe3da
	const val FILTER_B_AND_W = 0xe3db
	const val FILTER_CENTER_FOCUS = 0xe3dc
	const val FILTER_DRAMA = 0xe3dd
	const val FILTER_FRAMES = 0xe3de
	const val FILTER_HDR = 0xe3df
	const val FILTER_LIST = 0xe152
	const val FILTER_NONE = 0xe3e0
	const val FILTER_TILT_SHIFT = 0xe3e2
	const val FILTER_VINTAGE = 0xe3e3
	const val FIND_IN_PAGE = 0xe880
	const val FIND_REPLACE = 0xe881
	const val FINGERPRINT = 0xe90d
	const val FIRST_PAGE = 0xe5dc
	const val FITNESS_CENTER = 0xeb43
	const val FLAG = 0xe153
	const val FLARE = 0xe3e4
	const val FLASH_AUTO = 0xe3e5
	const val FLASH_OFF = 0xe3e6
	const val FLASH_ON = 0xe3e7
	const val FLIGHT = 0xe539
	const val FLIGHT_LAND = 0xe904
	const val FLIGHT_TAKEOFF = 0xe905
	const val FLIP = 0xe3e8
	const val FLIP_TO_BACK = 0xe882
	const val FLIP_TO_FRONT = 0xe883
	const val FOLDER = 0xe2c7
	const val FOLDER_OPEN = 0xe2c8
	const val FOLDER_SHARED = 0xe2c9
	const val FOLDER_SPECIAL = 0xe617
	const val FONT_DOWNLOAD = 0xe167
	const val FORMAT_ALIGN_CENTER = 0xe234
	const val FORMAT_ALIGN_JUSTIFY = 0xe235
	const val FORMAT_ALIGN_LEFT = 0xe236
	const val FORMAT_ALIGN_RIGHT = 0xe237
	const val FORMAT_BOLD = 0xe238
	const val FORMAT_CLEAR = 0xe239
	const val FORMAT_COLOR_FILL = 0xe23a
	const val FORMAT_COLOR_RESET = 0xe23b
	const val FORMAT_COLOR_TEXT = 0xe23c
	const val FORMAT_INDENT_DECREASE = 0xe23d
	const val FORMAT_INDENT_INCREASE = 0xe23e
	const val FORMAT_ITALIC = 0xe23f
	const val FORMAT_LINE_SPACING = 0xe240
	const val FORMAT_LIST_BULLETED = 0xe241
	const val FORMAT_LIST_NUMBERED = 0xe242
	const val FORMAT_PAINT = 0xe243
	const val FORMAT_QUOTE = 0xe244
	const val FORMAT_SHAPES = 0xe25e
	const val FORMAT_SIZE = 0xe245
	const val FORMAT_STRIKETHROUGH = 0xe246
	const val FORMAT_TEXTDIRECTION_L_TO_R = 0xe247
	const val FORMAT_TEXTDIRECTION_R_TO_L = 0xe248
	const val FORMAT_UNDERLINED = 0xe249
	const val FORUM = 0xe0bf
	const val FORWARD = 0xe154
	const val FORWARD_10 = 0xe056
	const val FORWARD_30 = 0xe057
	const val FORWARD_5 = 0xe058
	const val FREE_BREAKFAST = 0xeb44
	const val FULLSCREEN = 0xe5d0
	const val FULLSCREEN_EXIT = 0xe5d1
	const val FUNCTIONS = 0xe24a
	const val G_TRANSLATE = 0xe927
	const val GAMEPAD = 0xe30f
	const val GAMES = 0xe021
	const val GAVEL = 0xe90e
	const val GESTURE = 0xe155
	const val GET_APP = 0xe884
	const val GIF = 0xe908
	const val GOLF_COURSE = 0xeb45
	const val GPS_FIXED = 0xe1b3
	const val GPS_NOT_FIXED = 0xe1b4
	const val GPS_OFF = 0xe1b5
	const val GRADE = 0xe885
	const val GRADIENT = 0xe3e9
	const val GRAIN = 0xe3ea
	const val GRAPHIC_EQ = 0xe1b8
	const val GRID_OFF = 0xe3eb
	const val GRID_ON = 0xe3ec
	const val GROUP = 0xe7ef
	const val GROUP_ADD = 0xe7f0
	const val GROUP_WORK = 0xe886
	const val HD = 0xe052
	const val HDR_OFF = 0xe3ed
	const val HDR_ON = 0xe3ee
	const val HDR_STRONG = 0xe3f1
	const val HDR_WEAK = 0xe3f2
	const val HEADSET = 0xe310
	const val HEADSET_MIC = 0xe311
	const val HEALING = 0xe3f3
	const val HEARING = 0xe023
	const val HELP = 0xe887
	const val HELP_OUTLINE = 0xe8fd
	const val HIGH_QUALITY = 0xe024
	const val HIGHLIGHT = 0xe25f
	const val HIGHLIGHT_OFF = 0xe888
	const val HISTORY = 0xe889
	const val HOME = 0xe88a
	const val HOT_TUB = 0xeb46
	const val HOTEL = 0xe53a
	const val HOURGLASS_EMPTY = 0xe88b
	const val HOURGLASS_FULL = 0xe88c
	const val HTTP = 0xe902
	const val HTTPS = 0xe88d
	const val IMAGE = 0xe3f4
	const val IMAGE_ASPECT_RATIO = 0xe3f5
	const val IMPORT_CONTACTS = 0xe0e0
	const val IMPORT_EXPORT = 0xe0c3
	const val IMPORTANT_DEVICES = 0xe912
	const val INBOX = 0xe156
	const val INDETERMINATE_CHECK_BOX = 0xe909
	const val INFO = 0xe88e
	const val INFO_OUTLINE = 0xe88f
	const val INPUT = 0xe890
	const val INSERT_CHART = 0xe24b
	const val INSERT_COMMENT = 0xe24c
	const val INSERT_DRIVE_FILE = 0xe24d
	const val INSERT_EMOTICON = 0xe24e
	const val INSERT_INVITATION = 0xe24f
	const val INSERT_LINK = 0xe250
	const val INSERT_PHOTO = 0xe251
	const val INVERT_COLORS = 0xe891
	const val INVERT_COLORS_OFF = 0xe0c4
	const val ISO = 0xe3f6
	const val KEYBOARD = 0xe312
	const val KEYBOARD_ARROW_DOWN = 0xe313
	const val KEYBOARD_ARROW_LEFT = 0xe314
	const val KEYBOARD_ARROW_RIGHT = 0xe315
	const val KEYBOARD_ARROW_UP = 0xe316
	const val KEYBOARD_BACKSPACE = 0xe317
	const val KEYBOARD_CAPSLOCK = 0xe318
	const val KEYBOARD_HIDE = 0xe31a
	const val KEYBOARD_RETURN = 0xe31b
	const val KEYBOARD_TAB = 0xe31c
	const val KEYBOARD_VOICE = 0xe31d
	const val KITCHEN = 0xeb47
	const val LABEL = 0xe892
	const val LABEL_OUTLINE = 0xe893
	const val LANDSCAPE = 0xe3f7
	const val LANGUAGE = 0xe894
	const val LAPTOP = 0xe31e
	const val LAPTOP_CHROMEBOOK = 0xe31f
	const val LAPTOP_MAC = 0xe320
	const val LAPTOP_WINDOWS = 0xe321
	const val LAST_PAGE = 0xe5dd
	const val LAUNCH = 0xe895
	const val LAYERS = 0xe53b
	const val LAYERS_CLEAR = 0xe53c
	const val LEAK_ADD = 0xe3f8
	const val LEAK_REMOVE = 0xe3f9
	const val LENS = 0xe3fa
	const val LIBRARY_ADD = 0xe02e
	const val LIBRARY_BOOKS = 0xe02f
	const val LIBRARY_MUSIC = 0xe030
	const val LIGHTBULB_OUTLINE = 0xe90f
	const val LINE_STYLE = 0xe919
	const val LINE_WEIGHT = 0xe91a
	const val LINEAR_SCALE = 0xe260
	const val LINK = 0xe157
	const val LINKED_CAMERA = 0xe438
	const val LIST = 0xe896
	const val LIVE_HELP = 0xe0c6
	const val LIVE_TV = 0xe639
	const val LOCAL_ACTIVITY = 0xe53f
	const val LOCAL_AIRPORT = 0xe53d
	const val LOCAL_ATM = 0xe53e
	const val LOCAL_BAR = 0xe540
	const val LOCAL_CAFE = 0xe541
	const val LOCAL_CAR_WASH = 0xe542
	const val LOCAL_CONVENIENCE_STORE = 0xe543
	const val LOCAL_DINING = 0xe556
	const val LOCAL_DRINK = 0xe544
	const val LOCAL_FLORIST = 0xe545
	const val LOCAL_GAS_STATION = 0xe546
	const val LOCAL_GROCERY_STORE = 0xe547
	const val LOCAL_HOSPITAL = 0xe548
	const val LOCAL_HOTEL = 0xe549
	const val LOCAL_LAUNDRY_SERVICE = 0xe54a
	const val LOCAL_LIBRARY = 0xe54b
	const val LOCAL_MALL = 0xe54c
	const val LOCAL_MOVIES = 0xe54d
	const val LOCAL_OFFER = 0xe54e
	const val LOCAL_PARKING = 0xe54f
	const val LOCAL_PHARMACY = 0xe550
	const val LOCAL_PHONE = 0xe551
	const val LOCAL_PIZZA = 0xe552
	const val LOCAL_PLAY = 0xe553
	const val LOCAL_POST_OFFICE = 0xe554
	const val LOCAL_PRINTSHOP = 0xe555
	const val LOCAL_SEE = 0xe557
	const val LOCAL_SHIPPING = 0xe558
	const val LOCAL_TAXI = 0xe559
	const val LOCATION_CITY = 0xe7f1
	const val LOCATION_DISABLED = 0xe1b6
	const val LOCATION_OFF = 0xe0c7
	const val LOCATION_ON = 0xe0c8
	const val LOCATION_SEARCHING = 0xe1b7
	const val LOCK = 0xe897
	const val LOCK_OPEN = 0xe898
	const val LOCK_OUTLINE = 0xe899
	const val LOOKS = 0xe3fc
	const val LOOKS_3 = 0xe3fb
	const val LOOKS_4 = 0xe3fd
	const val LOOKS_5 = 0xe3fe
	const val LOOKS_6 = 0xe3ff
	const val LOOKS_ONE = 0xe400
	const val LOOKS_TWO = 0xe401
	const val LOOP = 0xe028
	const val LOUPE = 0xe402
	const val LOW_PRIORITY = 0xe16d
	const val LOYALTY = 0xe89a
	const val MAIL = 0xe158
	const val MAIL_OUTLINE = 0xe0e1
	const val MAP = 0xe55b
	const val MARKUNREAD = 0xe159
	const val MARKUNREAD_MAILBOX = 0xe89b
	const val MEMORY = 0xe322
	const val MENU = 0xe5d2
	const val MERGE_TYPE = 0xe252
	const val MESSAGE = 0xe0c9
	const val MIC = 0xe029
	const val MIC_NONE = 0xe02a
	const val MIC_OFF = 0xe02b
	const val MMS = 0xe618
	const val MODE_COMMENT = 0xe253
	const val MODE_EDIT = 0xe254
	const val MONETIZATION_ON = 0xe263
	const val MONEY_OFF = 0xe25c
	const val MONOCHROME_PHOTOS = 0xe403
	const val MOOD = 0xe7f2
	const val MOOD_BAD = 0xe7f3
	const val MORE = 0xe619
	const val MORE_HORIZ = 0xe5d3
	const val MORE_VERT = 0xe5d4
	const val MOTORCYCLE = 0xe91b
	const val MOUSE = 0xe323
	const val MOVE_TO_INBOX = 0xe168
	const val MOVIE = 0xe02c
	const val MOVIE_CREATION = 0xe404
	const val MOVIE_FILTER = 0xe43a
	const val MULTILINE_CHART = 0xe6df
	const val MUSIC_NOTE = 0xe405
	const val MUSIC_VIDEO = 0xe063
	const val MY_LOCATION = 0xe55c
	const val NATURE = 0xe406
	const val NATURE_PEOPLE = 0xe407
	const val NAVIGATE_BEFORE = 0xe408
	const val NAVIGATE_NEXT = 0xe409
	const val NAVIGATION = 0xe55d
	const val NEAR_ME = 0xe569
	const val NETWORK_CELL = 0xe1b9
	const val NETWORK_CHECK = 0xe640
	const val NETWORK_LOCKED = 0xe61a
	const val NETWORK_WIFI = 0xe1ba
	const val NEW_RELEASES = 0xe031
	const val NEXT_WEEK = 0xe16a
	const val NFC = 0xe1bb
	const val NO_ENCRYPTION = 0xe641
	const val NO_SIM = 0xe0cc
	const val NOT_INTERESTED = 0xe033
	const val NOTE = 0xe06f
	const val NOTE_ADD = 0xe89c
	const val NOTIFICATIONS = 0xe7f4
	const val NOTIFICATIONS_ACTIVE = 0xe7f7
	const val NOTIFICATIONS_NONE = 0xe7f5
	const val NOTIFICATIONS_OFF = 0xe7f6
	const val NOTIFICATIONS_PAUSED = 0xe7f8
	const val OFFLINE_PIN = 0xe90a
	const val ONDEMAND_VIDEO = 0xe63a
	const val OPACITY = 0xe91c
	const val OPEN_IN_BROWSER = 0xe89d
	const val OPEN_IN_NEW = 0xe89e
	const val OPEN_WITH = 0xe89f
	const val PAGES = 0xe7f9
	const val PAGEVIEW = 0xe8a0
	const val PALETTE = 0xe40a
	const val PAN_TOOL = 0xe925
	const val PANORAMA = 0xe40b
	const val PANORAMA_FISH_EYE = 0xe40c
	const val PANORAMA_HORIZONTAL = 0xe40d
	const val PANORAMA_VERTICAL = 0xe40e
	const val PANORAMA_WIDE_ANGLE = 0xe40f
	const val PARTY_MODE = 0xe7fa
	const val PAUSE = 0xe034
	const val PAUSE_CIRCLE_FILLED = 0xe035
	const val PAUSE_CIRCLE_OUTLINE = 0xe036
	const val PAYMENT = 0xe8a1
	const val PEOPLE = 0xe7fb
	const val PEOPLE_OUTLINE = 0xe7fc
	const val PERM_CAMERA_MIC = 0xe8a2
	const val PERM_CONTACT_CALENDAR = 0xe8a3
	const val PERM_DATA_SETTING = 0xe8a4
	const val PERM_DEVICE_INFORMATION = 0xe8a5
	const val PERM_IDENTITY = 0xe8a6
	const val PERM_MEDIA = 0xe8a7
	const val PERM_PHONE_MSG = 0xe8a8
	const val PERM_SCAN_WIFI = 0xe8a9
	const val PERSON = 0xe7fd
	const val PERSON_ADD = 0xe7fe
	const val PERSON_OUTLINE = 0xe7ff
	const val PERSON_PIN = 0xe55a
	const val PERSON_PIN_CIRCLE = 0xe56a
	const val PERSONAL_VIDEO = 0xe63b
	const val PETS = 0xe91d
	const val PHONE = 0xe0cd
	const val PHONE_ANDROID = 0xe324
	const val PHONE_BLUETOOTH_SPEAKER = 0xe61b
	const val PHONE_FORWARDED = 0xe61c
	const val PHONE_IN_TALK = 0xe61d
	const val PHONE_IPHONE = 0xe325
	const val PHONE_LOCKED = 0xe61e
	const val PHONE_MISSED = 0xe61f
	const val PHONE_PAUSED = 0xe620
	const val PHONELINK = 0xe326
	const val PHONELINK_ERASE = 0xe0db
	const val PHONELINK_LOCK = 0xe0dc
	const val PHONELINK_OFF = 0xe327
	const val PHONELINK_RING = 0xe0dd
	const val PHONELINK_SETUP = 0xe0de
	const val PHOTO = 0xe410
	const val PHOTO_ALBUM = 0xe411
	const val PHOTO_CAMERA = 0xe412
	const val PHOTO_FILTER = 0xe43b
	const val PHOTO_LIBRARY = 0xe413
	const val PHOTO_SIZE_SELECT_ACTUAL = 0xe432
	const val PHOTO_SIZE_SELECT_LARGE = 0xe433
	const val PHOTO_SIZE_SELECT_SMALL = 0xe434
	const val PICTURE_AS_PDF = 0xe415
	const val PICTURE_IN_PICTURE = 0xe8aa
	const val PICTURE_IN_PICTURE_ALT = 0xe911
	const val PIE_CHART = 0xe6c4
	const val PIE_CHART_OUTLINED = 0xe6c5
	const val PIN_DROP = 0xe55e
	const val PLACE = 0xe55f
	const val PLAY_ARROW = 0xe037
	const val PLAY_CIRCLE_FILLED = 0xe038
	const val PLAY_CIRCLE_OUTLINE = 0xe039
	const val PLAY_FOR_WORK = 0xe906
	const val PLAYLIST_ADD = 0xe03b
	const val PLAYLIST_ADD_CHECK = 0xe065
	const val PLAYLIST_PLAY = 0xe05f
	const val PLUS_ONE = 0xe800
	const val POLL = 0xe801
	const val POLYMER = 0xe8ab
	const val POOL = 0xeb48
	const val PORTABLE_WIFI_OFF = 0xe0ce
	const val PORTRAIT = 0xe416
	const val POWER = 0xe63c
	const val POWER_INPUT = 0xe336
	const val POWER_SETTINGS_NEW = 0xe8ac
	const val PREGNANT_WOMAN = 0xe91e
	const val PRESENT_TO_ALL = 0xe0df
	const val PRINT = 0xe8ad
	const val PRIORITY_HIGH = 0xe645
	const val PUBLIC = 0xe80b
	const val PUBLISH = 0xe255
	const val QUERY_BUILDER = 0xe8ae
	const val QUESTION_ANSWER = 0xe8af
	const val QUEUE = 0xe03c
	const val QUEUE_MUSIC = 0xe03d
	const val QUEUE_PLAY_NEXT = 0xe066
	const val RADIO = 0xe03e
	const val RADIO_BUTTON_CHECKED = 0xe837
	const val RADIO_BUTTON_UNCHECKED = 0xe836
	const val RATE_REVIEW = 0xe560
	const val RECEIPT = 0xe8b0
	const val RECENT_ACTORS = 0xe03f
	const val RECORD_VOICE_OVER = 0xe91f
	const val REDEEM = 0xe8b1
	const val REDO = 0xe15a
	const val REFRESH = 0xe5d5
	const val REMOVE = 0xe15b
	const val REMOVE_CIRCLE = 0xe15c
	const val REMOVE_CIRCLE_OUTLINE = 0xe15d
	const val REMOVE_FROM_QUEUE = 0xe067
	const val REMOVE_RED_EYE = 0xe417
	const val REMOVE_SHOPPING_CART = 0xe928
	const val REORDER = 0xe8fe
	const val REPEAT = 0xe040
	const val REPEAT_ONE = 0xe041
	const val REPLAY = 0xe042
	const val REPLAY_10 = 0xe059
	const val REPLAY_30 = 0xe05a
	const val REPLAY_5 = 0xe05b
	const val REPLY = 0xe15e
	const val REPLY_ALL = 0xe15f
	const val REPORT = 0xe160
	const val REPORT_PROBLEM = 0xe8b2
	const val RESTAURANT = 0xe56c
	const val RESTAURANT_MENU = 0xe561
	const val RESTORE = 0xe8b3
	const val RESTORE_PAGE = 0xe929
	const val RING_VOLUME = 0xe0d1
	const val ROOM = 0xe8b4
	const val ROOM_SERVICE = 0xeb49
	const val ROTATE_90_DEGREES_CCW = 0xe418
	const val ROTATE_LEFT = 0xe419
	const val ROTATE_RIGHT = 0xe41a
	const val ROUNDED_CORNER = 0xe920
	const val ROUTER = 0xe328
	const val ROWING = 0xe921
	const val RSS_FEED = 0xe0e5
	const val RV_HOOKUP = 0xe642
	const val SATELLITE = 0xe562
	const val SAVE = 0xe161
	const val SCANNER = 0xe329
	const val SCHEDULE = 0xe8b5
	const val SCHOOL = 0xe80c
	const val SCREEN_LOCK_LANDSCAPE = 0xe1be
	const val SCREEN_LOCK_PORTRAIT = 0xe1bf
	const val SCREEN_LOCK_ROTATION = 0xe1c0
	const val SCREEN_ROTATION = 0xe1c1
	const val SCREEN_SHARE = 0xe0e2
	const val SD_CARD = 0xe623
	const val SD_STORAGE = 0xe1c2
	const val SEARCH = 0xe8b6
	const val SECURITY = 0xe32a
	const val SELECT_ALL = 0xe162
	const val SEND = 0xe163
	const val SENTIMENT_DISSATISFIED = 0xe811
	const val SENTIMENT_NEUTRAL = 0xe812
	const val SENTIMENT_SATISFIED = 0xe813
	const val SENTIMENT_VERY_DISSATISFIED = 0xe814
	const val SENTIMENT_VERY_SATISFIED = 0xe815
	const val SETTINGS = 0xe8b8
	const val SETTINGS_APPLICATIONS = 0xe8b9
	const val SETTINGS_BACKUP_RESTORE = 0xe8ba
	const val SETTINGS_BLUETOOTH = 0xe8bb
	const val SETTINGS_BRIGHTNESS = 0xe8bd
	const val SETTINGS_CELL = 0xe8bc
	const val SETTINGS_ETHERNET = 0xe8be
	const val SETTINGS_INPUT_ANTENNA = 0xe8bf
	const val SETTINGS_INPUT_COMPONENT = 0xe8c0
	const val SETTINGS_INPUT_COMPOSITE = 0xe8c1
	const val SETTINGS_INPUT_HDMI = 0xe8c2
	const val SETTINGS_INPUT_SVIDEO = 0xe8c3
	const val SETTINGS_OVERSCAN = 0xe8c4
	const val SETTINGS_PHONE = 0xe8c5
	const val SETTINGS_POWER = 0xe8c6
	const val SETTINGS_REMOTE = 0xe8c7
	const val SETTINGS_SYSTEM_DAYDREAM = 0xe1c3
	const val SETTINGS_VOICE = 0xe8c8
	const val SHARE = 0xe80d
	const val SHOP = 0xe8c9
	const val SHOP_TWO = 0xe8ca
	const val SHOPPING_BASKET = 0xe8cb
	const val SHOPPING_CART = 0xe8cc
	const val SHORT_TEXT = 0xe261
	const val SHOW_CHART = 0xe6e1
	const val SHUFFLE = 0xe043
	const val SIGNAL_CELLULAR_4_BAR = 0xe1c8
	const val SIGNAL_CELLULAR_CONNECTED_NO_INTERNET_4_BAR = 0xe1cd
	const val SIGNAL_CELLULAR_NO_SIM = 0xe1ce
	const val SIGNAL_CELLULAR_NULL = 0xe1cf
	const val SIGNAL_CELLULAR_OFF = 0xe1d0
	const val SIGNAL_WIFI_4_BAR = 0xe1d8
	const val SIGNAL_WIFI_4_BAR_LOCK = 0xe1d9
	const val SIGNAL_WIFI_OFF = 0xe1da
	const val SIM_CARD = 0xe32b
	const val SIM_CARD_ALERT = 0xe624
	const val SKIP_NEXT = 0xe044
	const val SKIP_PREVIOUS = 0xe045
	const val SLIDESHOW = 0xe41b
	const val SLOW_MOTION_VIDEO = 0xe068
	const val SMARTPHONE = 0xe32c
	const val SMOKE_FREE = 0xeb4a
	const val SMOKING_ROOMS = 0xeb4b
	const val SMS = 0xe625
	const val SMS_FAILED = 0xe626
	const val SNOOZE = 0xe046
	const val SORT = 0xe164
	const val SORT_BY_ALPHA = 0xe053
	const val SPA = 0xeb4c
	const val SPACE_BAR = 0xe256
	const val SPEAKER = 0xe32d
	const val SPEAKER_GROUP = 0xe32e
	const val SPEAKER_NOTES = 0xe8cd
	const val SPEAKER_NOTES_OFF = 0xe92a
	const val SPEAKER_PHONE = 0xe0d2
	const val SPELLCHECK = 0xe8ce
	const val STAR = 0xe838
	const val STAR_BORDER = 0xe83a
	const val STAR_HALF = 0xe839
	const val STARS = 0xe8d0
	const val STAY_CURRENT_LANDSCAPE = 0xe0d3
	const val STAY_CURRENT_PORTRAIT = 0xe0d4
	const val STAY_PRIMARY_LANDSCAPE = 0xe0d5
	const val STAY_PRIMARY_PORTRAIT = 0xe0d6
	const val STOP = 0xe047
	const val STOP_SCREEN_SHARE = 0xe0e3
	const val STORAGE = 0xe1db
	const val STORE = 0xe8d1
	const val STORE_MALL_DIRECTORY = 0xe563
	const val STRAIGHTEN = 0xe41c
	const val STREETVIEW = 0xe56e
	const val STRIKETHROUGH_S = 0xe257
	const val STYLE = 0xe41d
	const val SUBDIRECTORY_ARROW_LEFT = 0xe5d9
	const val SUBDIRECTORY_ARROW_RIGHT = 0xe5da
	const val SUBJECT = 0xe8d2
	const val SUBSCRIPTIONS = 0xe064
	const val SUBTITLES = 0xe048
	const val SUBWAY = 0xe56f
	const val SUPERVISOR_ACCOUNT = 0xe8d3
	const val SURROUND_SOUND = 0xe049
	const val SWAP_CALLS = 0xe0d7
	const val SWAP_HORIZ = 0xe8d4
	const val SWAP_VERT = 0xe8d5
	const val SWAP_VERTICAL_CIRCLE = 0xe8d6
	const val SWITCH_CAMERA = 0xe41e
	const val SWITCH_VIDEO = 0xe41f
	const val SYNC = 0xe627
	const val SYNC_DISABLED = 0xe628
	const val SYNC_PROBLEM = 0xe629
	const val SYSTEM_UPDATE = 0xe62a
	const val SYSTEM_UPDATE_ALT = 0xe8d7
	const val TAB = 0xe8d8
	const val TAB_UNSELECTED = 0xe8d9
	const val TABLET = 0xe32f
	const val TABLET_ANDROID = 0xe330
	const val TABLET_MAC = 0xe331
	const val TAG_FACES = 0xe420
	const val TAP_AND_PLAY = 0xe62b
	const val TERRAIN = 0xe564
	const val TEXT_FIELDS = 0xe262
	const val TEXT_FORMAT = 0xe165
	const val TEXTSMS = 0xe0d8
	const val TEXTURE = 0xe421
	const val THEATERS = 0xe8da
	const val THUMB_DOWN = 0xe8db
	const val THUMB_UP = 0xe8dc
	const val THUMBS_UP_DOWN = 0xe8dd
	const val TIME_TO_LEAVE = 0xe62c
	const val TIMELAPSE = 0xe422
	const val TIMELINE = 0xe922
	const val TIMER = 0xe425
	const val TIMER_10 = 0xe423
	const val TIMER_3 = 0xe424
	const val TIMER_OFF = 0xe426
	const val TITLE = 0xe264
	const val TOC = 0xe8de
	const val TODAY = 0xe8df
	const val TOLL = 0xe8e0
	const val TONALITY = 0xe427
	const val TOUCH_APP = 0xe913
	const val TOYS = 0xe332
	const val TRACK_CHANGES = 0xe8e1
	const val TRAFFIC = 0xe565
	const val TRAIN = 0xe570
	const val TRAM = 0xe571
	const val TRANSFER_WITHIN_A_STATION = 0xe572
	const val TRANSFORM = 0xe428
	const val TRANSLATE = 0xe8e2
	const val TRENDING_DOWN = 0xe8e3
	const val TRENDING_FLAT = 0xe8e4
	const val TRENDING_UP = 0xe8e5
	const val TUNE = 0xe429
	const val TURNED_IN = 0xe8e6
	const val TURNED_IN_NOT = 0xe8e7
	const val TV = 0xe333
	const val UNARCHIVE = 0xe169
	const val UNDO = 0xe166
	const val UNFOLD_LESS = 0xe5d6
	const val UNFOLD_MORE = 0xe5d7
	const val UPDATE = 0xe923
	const val USB = 0xe1e0
	const val VERIFIED_USER = 0xe8e8
	const val VERTICAL_ALIGN_BOTTOM = 0xe258
	const val VERTICAL_ALIGN_CENTER = 0xe259
	const val VERTICAL_ALIGN_TOP = 0xe25a
	const val VIBRATION = 0xe62d
	const val VIDEO_CALL = 0xe070
	const val VIDEO_LABEL = 0xe071
	const val VIDEO_LIBRARY = 0xe04a
	const val VIDEOCAM = 0xe04b
	const val VIDEOCAM_OFF = 0xe04c
	const val VIDEOGAME_ASSET = 0xe338
	const val VIEW_AGENDA = 0xe8e9
	const val VIEW_ARRAY = 0xe8ea
	const val VIEW_CAROUSEL = 0xe8eb
	const val VIEW_COLUMN = 0xe8ec
	const val VIEW_COMFY = 0xe42a
	const val VIEW_COMPACT = 0xe42b
	const val VIEW_DAY = 0xe8ed
	const val VIEW_HEADLINE = 0xe8ee
	const val VIEW_LIST = 0xe8ef
	const val VIEW_MODULE = 0xe8f0
	const val VIEW_QUILT = 0xe8f1
	const val VIEW_STREAM = 0xe8f2
	const val VIEW_WEEK = 0xe8f3
	const val VIGNETTE = 0xe435
	const val VISIBILITY = 0xe8f4
	const val VISIBILITY_OFF = 0xe8f5
	const val VOICE_CHAT = 0xe62e
	const val VOICEMAIL = 0xe0d9
	const val VOLUME_DOWN = 0xe04d
	const val VOLUME_MUTE = 0xe04e
	const val VOLUME_OFF = 0xe04f
	const val VOLUME_UP = 0xe050
	const val VPN_KEY = 0xe0da
	const val VPN_LOCK = 0xe62f
	const val WALLPAPER = 0xe1bc
	const val WARNING = 0xe002
	const val WATCH = 0xe334
	const val WATCH_LATER = 0xe924
	const val WB_AUTO = 0xe42c
	const val WB_CLOUDY = 0xe42d
	const val WB_INCANDESCENT = 0xe42e
	const val WB_IRIDESCENT = 0xe436
	const val WB_SUNNY = 0xe430
	const val WC = 0xe63d
	const val WEB = 0xe051
	const val WEB_ASSET = 0xe069
	const val WEEKEND = 0xe16b
	const val WHATSHOT = 0xe80e
	const val WIDGETS = 0xe1bd
	const val WIFI = 0xe63e
	const val WIFI_LOCK = 0xe1e1
	const val WIFI_TETHERING = 0xe1e2
	const val WORK = 0xe8f9
	const val WRAP_TEXT = 0xe25b
	const val YOUTUBE_SEARCHED_FOR = 0xe8fa
	const val ZOOM_IN = 0xe8ff
	const val ZOOM_OUT = 0xe900
	const val ZOOM_OUT_MAP = 0xe56b
	
}

inline fun Context.icon(codePoint: Int, init: ComponentInit<UiComponentImpl<HTMLElement>> = {}): UiComponentImpl<HTMLElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("i") {
		addClass(materialIconsStyleTag)
		dom.innerText = codePoint.toChar().toString()
		init()
	}
}

object MaterialIconsCss {
	init {
		head.add(linkElement("https://fonts.googleapis.com/icon?family=Material+Icons", rel = "stylesheet"))
	}

	val materialIconsStyleTag = CssClass("material-icons")
}

class IconButton(owner: Context) : A(owner) {

	val iconComponent = addChild(icon(0))
	val labelComponent = addChild(span {
		addClass(IconButtonStyle.label)
	})

	var icon: Int = 0
		set(value) {
			field = value
			iconComponent.label = value.toChar().toString()
		}

	override var label: String
		get() = labelComponent.label
		set(value) {
			labelComponent.label = value
			labelComponent.style.display = if (value.isEmpty()) "none" else "inline-block"
		}

	var disabled: Boolean by afterChange(false) {
		toggleClass(CommonStyleTags.disabled)
	}

	init {
		addClass(iconButton)
		tabIndex = 0
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		labelComponent.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		labelComponent.removeElement(element)
	}
}

object IconButtonStyle {

	val iconButton by cssClass()
	val label by cssClass()

	init {
		addStyleToHead("""
	
$iconButton {
	display: flex;
	align-items: center;
	-webkit-user-select: none;        
	-moz-user-select: none;
	user-select: none;
}

$label {
	margin-left: ${CssProps.gap.v};
	display: none;
}

$iconButton${CommonStyleTags.disabled} {
	color: ${CssProps.toggledInnerDisabled.v};
	pointer-events: none;
}
			""")
	}
}

inline fun Context.iconButton(codePoint: Int, label: String = "", init: ComponentInit<IconButton> = {}): IconButton {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return IconButton(this).apply {
		icon = codePoint
		this.label = label
		init()
	}
}

