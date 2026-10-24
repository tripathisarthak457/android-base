package com.base.app.catalog.media.video

import androidx.compose.runtime.Immutable
import com.base.app.core.video.VideoClip

/** A sample clip with what the scene writes over it: who filmed it and what it shows. */
@Immutable
internal class SampleClip(val clip: VideoClip, val title: String, val author: String)

/**
 * Eleven public-domain (CC0) clips, each its uploader's own work, served as what reels apps serve: H.264
 * MP4, portrait 720 x 1280 with landscape originals cropped, at most ten seconds, a keyframe every second,
 * each with a JPEG of its first frame as its poster. They are assets of one release of the template's own
 * repository, transcoded once from originals on Wikimedia Commons and in MDN's CC0 examples, so the scene's
 * automated runs never load a third-party host. Every URL is here, so moving the set is one file.
 */
internal val SampleClips: List<SampleClip> = listOf(
    released("flower-moon", "The flower moon rising", "Roesalow"),
    released("kings-peak", "On King's Peak", "Derrellwilliams"),
    released("tilia", "Linden flowers under a microscope", "Gustamons"),
    released("oystercatcher", "A young oystercatcher", "Cesaremanfredi"),
    released("srnka", "A deer in a garden", "Pohled 111"),
    released("perseids", "The Perseids", "Bautsch"),
    released("ocean", "The sea at Sommarøy", "Netha Hussain"),
    released("bumblebee", "A bumblebee on flowers", "Roman Ryabenko"),
    released("jellyfish", "Jellyfish in Monaco's aquarium", "Benoit Prieur"),
    released("mt-hood", "Over Mount Hood", "Pastelitodepapa"),
    released("flower", "A flower opening", "MDN"),
)

/** The clips repeated, so a long session of swipes measures memory well past the first round. */
internal val ReelPages: List<SampleClip> = List(SampleClips.size * 4) { SampleClips[it % SampleClips.size] }

private fun released(key: String, title: String, author: String) = SampleClip(
    clip = VideoClip(
        id = key,
        url = "$RELEASE/$key.mp4",
        posterUrl = "$RELEASE/$key.jpg",
        width = CLIP_WIDTH,
        height = CLIP_HEIGHT,
    ),
    title = title,
    author = author,
)

private const val RELEASE = "https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1"
private const val CLIP_WIDTH = 720
private const val CLIP_HEIGHT = 1280
