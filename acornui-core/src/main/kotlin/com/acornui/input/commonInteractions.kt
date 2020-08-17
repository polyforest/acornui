/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.input

import com.acornui.component.WithNode
import com.acornui.signal.*
import org.w3c.dom.Node
import org.w3c.dom.TouchEvent
import org.w3c.dom.Window
import org.w3c.dom.events.*
import org.w3c.dom.events.Event

/**
 * Dispatched when the mouse or touch is pressed down on this element.
 */
val WithEventTarget.mousePressed
	get() = event<MouseEvent>("mousedown")

/**
 * Dispatched when the mouse or touch is released from this element.
 */
val WithEventTarget.mouseReleased
	get() = event<MouseEvent>("mouseup")

/**
 * Dispatched when the mouse or touch has moved within the bounds of this element.
 */
val WithEventTarget.mouseMoved
	get() = event<MouseEvent>("mousemove")

/**
 * The mouseenter event is fired at an Element when a pointing device (usually a mouse) is initially moved so that its
 * hotspot is within the element at which the event was fired.
 *
 * Though similar to mouseover, mouseenter differs in that it doesn't bubble and it isn't sent to any descendants when
 * the pointer is moved from one of its descendants' physical space to its own physical space.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseenter_event]
 */
val WithEventTarget.mouseEntered
	get() = event<MouseEvent>("mouseenter")

/**
 * The mouseleave event is fired at an Element when the cursor of a pointing device (usually a mouse) is moved out of
 * it.
 *
 * mouseLeave and [mouseExitedChild] are similar but differ in that mouseleave does not bubble and mouseout does. This means
 * that mouseleave is fired when the pointer has exited the element and all of its descendants, whereas mouseout is
 * fired when the pointer leaves the element or leaves one of the element's descendants (even if the pointer is still
 * within the element).
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseleave_event]
 */
val WithEventTarget.mouseExited
	get() = event<MouseEvent>("mouseleave")

/**
 * The mouseover event is fired at an Element when a pointing device (such as a mouse or trackpad) is used to move the
 * cursor onto the element or one of its child elements.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseover_event]
 */
val WithEventTarget.mouseEnteredChild
	get() = event<MouseEvent>("mouseover")

/**
 * The mouseout event is fired  at an Element when a pointing device (usually a mouse) is used to move the cursor so
 * that it is no longer contained within the element or one of its children. mouseout is also delivered to an element
 * if the cursor enters a child element, because the child element obscures the visible area of the element.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseout_event]
 */
val WithEventTarget.mouseExitedChild
	get() = event<MouseEvent>("mouseout")

/**
 * Dispatched when both pressed and released while the pointer is located inside the element.
 *
 * If the button is pressed on one element and the pointer is moved outside the element before the button is released,
 * the event is fired on the most specific ancestor element that contained both elements.
 *
 * click fires after both the mousedown and mouseup events have fired, in that order.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event]
 */
val WithEventTarget.clicked
	get() = event<MouseEvent>("click")

/**
 * Dispatched when a key has been pressed while this interactive element has focus.
 */
val WithEventTarget.keyPressed
	get() = event<KeyboardEvent>("keydown")

/**
 * Dispatched when a key has been released while this interactive element has focus.
 */
val WithEventTarget.keyReleased
	get() = event<KeyboardEvent>("keyup")

/**
 * The wheel event fires when the user rotates a wheel button on a pointing device (typically a mouse).
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/wheel_event]
 */
val WithEventTarget.wheeled
	get() = event<WheelEvent>("wheel")

/**
 * The touchstart event is fired when one or more touch points are placed on the touch surface.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/touchstart_event]]
 */
val WithEventTarget.touchStarted
	get() = event<TouchEvent>("touchstart")

/**
 * The touchmove event is fired when one or more touch points are moved along the touch surface.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/touchmove_event]
 */
val WithEventTarget.touchMoved
	get() = event<TouchEvent>("touchmove")

/**
 * The touchend event fires when one or more touch points are removed from the touch surface.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/touchend_event]
 */
val WithEventTarget.touchEnded
	get() = event<TouchEvent>("touchend")

/**
 * The touchcancel event is fired when one or more touch points have been disrupted in an implementation-specific manner
 * (for example, too many touch points are created).
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/Element/touchcancel_event]
 */
val WithEventTarget.touchCancelled
	get() = event<TouchEvent>("touchcancel")

/**
 * Fires when the document view or an element has been scrolled.
 *
 * @see com.acornui.component.scroll.ScrollArea
 */
val WithEventTarget.scrolled
	get() = event<Event>("scroll")

/**
 * The change event is fired for `<input>`, `<select>`, and `<textarea>` elements when an alteration to the element's
 * value is committed by the user. Unlike the input event, the change event is not necessarily fired for each alteration
 * to an element's value.
 */
val WithEventTarget.changed
	get() = event<Event>("change")

/**
 * The input event fires when the value of an `<input>`, `<select>`, or `<textarea>` element has been changed.
 */
val WithEventTarget.input
	get() = event<InputEvent>("input")

/**
 * The focus event fires when an element has received focus. The main difference between this event and [focusedIn] is that
 * [focusedIn] bubbles while [focused] does not.
 */
val WithEventTarget.focused
	get() = event<FocusEvent>("focus")

/**
 * The focusin event fires when an element is about to receive focus. The main difference between this event and [focused]
 * is that [focusedIn] bubbles while [focused] does not.
 */
val WithEventTarget.focusedIn
	get() = event<FocusEvent>("focusin")

/**
 * The blur event fires when an element has lost focus. The main difference between this event and [focusedOut] is that
 * [focusedOut] bubbles while [blurred] does not.
 */
val WithEventTarget.blurred
	get() = event<FocusEvent>("blur")

/**
 * The focusout event fires when an element is about to lose focus. The main difference between this event and [blurred] is
 * that focusout bubbles while [blurred] does not.
 */
val WithEventTarget.focusedOut
	get() = event<FocusEvent>("focusout")

/**
 * The [focusedOut] event filtered to only dispatch when the new focus is not a descendent of this node.
 */
val WithNode.focusedOutContainer
	get() = event<FocusEvent>("focusout").filtered {
		(it.relatedTarget == null || !dom.contains(it.relatedTarget.unsafeCast<Node>()))
	}

/**
 * The [focusedIn] event filtered to only dispatch when the previous focus is not a descendent of this node.
 */
val WithNode.focusedInContainer
	get() = event<FocusEvent>("focusin").filtered {
		(it.relatedTarget == null || !dom.contains(it.relatedTarget.unsafeCast<Node>()))
	}

/**
 * The contextmenu event fires when the user attempts to open a context menu. This event is typically triggered by
 * clicking the right mouse button, or by pressing the context menu key. In the latter case, the context menu is
 * displayed at the bottom left of the focused element, unless the element is a tree, in which case the context menu is
 * displayed at the bottom left of the current row.
 */
val WithEventTarget.contextMenuOpened
	get() = event<MouseEvent>("contextmenu")

val WithEventTarget.load
	get() = event<Event>("load")

val Window.beforeUnloaded: Signal<Event>
	get() = asWithEventTarget().event("beforeunload")

// TODO: Clipboard