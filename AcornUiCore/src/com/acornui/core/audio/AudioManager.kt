package com.acornui.core.audio

import com.acornui.collection.ActiveList
import com.acornui.collection.poll
import com.acornui.collection.pop
import com.acornui.collection.sortedInsertionIndex
import com.acornui.core.Disposable
import com.acornui.core.UpdatableChild
import com.acornui.core.UpdatableChildBase
import com.acornui.core.di.DKey


interface AudioManagerRo {

	/**
	 * The global sound gain. 0-1
	 */
	var soundVolume: Float

	/**
	 * The global music gain. 0-1
	 */
	var musicVolume: Float

	/**
	 * The current active sounds being played.
	 */
	val activeSounds: List<Sound>

	/**
	 * The current active musics being played.
	 */
	val activeMusics: List<Music>

	companion object : DKey<AudioManagerRo>

}

interface AudioManager : AudioManagerRo, UpdatableChild {

	val simultaneousSounds: Int

	/**
	 * A quick check to ensure this sound can be played given the priority of the current active sounds.
	 */
	fun canPlaySound(priority: Float): Boolean {
		if (activeSounds.size < simultaneousSounds) return true
		return priority >= activeSounds.last().priority
	}

	fun registerSoundSource(soundSource: SoundFactory)
	fun unregisterSoundSource(soundSource: SoundFactory)

	fun registerSound(sound: Sound)
	fun unregisterSound(sound: Sound)

	fun registerMusic(music: Music)
	fun unregisterMusic(music: Music)

	companion object : DKey<AudioManager> {

		override val extends: DKey<AudioManagerRo>? = AudioManagerRo
	}
}

open class AudioManagerImpl(override final val simultaneousSounds: Int = 8) : UpdatableChildBase(), AudioManager, Disposable {

	override val activeSounds = ActiveList<Sound>(simultaneousSounds)
	override val activeMusics = ActiveList<Music>()
	protected val soundSources = ArrayList<SoundFactory>()

	private val soundPriorityComparator = {
		o1: Sound?, o2: Sound? ->
		if (o1 == null && o2 == null) 0
		else if (o1 == null) 1
		else if (o2 == null) -1
		else {
			// A lower priority value should be first in the list (and therefore first to be removed).
			o1.priority.compareTo(o2.priority)
		}
	}

	override fun registerSoundSource(soundSource: SoundFactory) {
		soundSources.add(soundSource)
	}

	override fun unregisterSoundSource(soundSource: SoundFactory) {
		soundSources.remove(soundSource)
	}

	override fun registerSound(sound: Sound) {
		val index = activeSounds.sortedInsertionIndex(sound, soundPriorityComparator)
		activeSounds.add(index, sound)
		if (activeSounds.size > simultaneousSounds) {
			// Exceeded simultaneous sounds.
			activeSounds.poll().stop()
		}
	}

	override fun unregisterSound(sound: Sound) {
		activeSounds.remove(sound)
	}

	override fun registerMusic(music: Music) {
		activeMusics.add(music)
	}

	override fun unregisterMusic(music: Music) {
		activeMusics.remove(music)
	}

	private var _soundVolume: Float = 1f

	override var soundVolume: Float
		get() = _soundVolume
		set(value) {
			if (_soundVolume == value) return
			_soundVolume = value
			for (i in 0..activeSounds.lastIndex) {
				activeSounds[i].volume = activeSounds[i].volume
			}
		}

	private var _musicVolume: Float = 1f

	override var musicVolume: Float
		get() = _musicVolume
		set(value) {
			if (_musicVolume == value) return
			_musicVolume = value
			for (i in 0..activeMusics.lastIndex) {
				activeMusics[i].volume = activeMusics[i].volume
			}
		}

	override fun update(stepTime: Float) {
		activeMusics.iterate {
			it.update()
			true
		}
		activeSounds.iterate {
			it.update()
			true
		}

	}

	override fun dispose() {
		remove()
		while (activeSounds.isNotEmpty()) {
			activeSounds.poll().dispose()
		}
		while (activeMusics.isNotEmpty()) {
			activeMusics.poll().dispose()
		}
		while (soundSources.isNotEmpty()) {
			soundSources.pop().dispose()
		}
	}
}