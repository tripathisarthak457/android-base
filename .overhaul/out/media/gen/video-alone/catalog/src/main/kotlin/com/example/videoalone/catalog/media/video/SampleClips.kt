package com.example.videoalone.catalog.media.video

import androidx.compose.runtime.Immutable
import com.example.videoalone.core.video.VideoClip

/** A sample clip with what the scene writes over it: who filmed it and what it shows. */
@Immutable
internal class SampleClip(val clip: VideoClip, val title: String, val author: String)

/**
 * Twelve public-domain (CC0) clips, each its uploader's own work: ten from Wikimedia Commons (WebM, VP9
 * or VP8, with Commons' still of each as its poster) and two from MDN's CC0 examples (H.264 MP4, with no
 * poster anywhere, so they show the skeleton colour until their first frame). Landscape clips fill the
 * portrait page cropped, as reels apps show them. The catalog's client names itself in its User-Agent,
 * which Commons asks of every client. Every URL is here, so moving the set to other hosting is one file.
 */
internal val SampleClips: List<SampleClip> = listOf(
    commons("flower-moon", "a/a8", "Flower_Moon_May_2nd_2024.webm", 1080, 1920, "The flower moon rising", "Roesalow"),
    commons("kings-peak", "3/3b", "King%27s_Peak_summit.webm", 1080, 1920, "On King's Peak", "Derrellwilliams"),
    commons(
        "tilia", "8/80", "Tilia_cordata_flowers_under_a_microscope_zoom-in.webm", 1080, 1920,
        "Linden flowers under a microscope", "Gustamons",
    ),
    commons("oystercatcher", "3/3d", "Oystercatcher_juvenile_15-06-24_1.webm", 1080, 1920, "A young oystercatcher", "Cesaremanfredi"),
    commons("srnka", "d/dc", "Srnka_v_zahrad%C4%9B.webm", 1920, 1080, "A deer in a garden", "Pohled 111"),
    commons("perseids", "1/1d", "Perseiden.2020-08-12.webm", 1920, 1080, "The Perseids", "Bautsch"),
    commons("ocean", "5/5d", "Ocean_in_Sommaroy.webm", 1920, 1080, "The sea at Sommarøy", "Netha Hussain"),
    commons("bumblebee", "0/04", "Bumblebee_on_flowers.webm", 1920, 1080, "A bumblebee on flowers", "Roman Ryabenko"),
    commons(
        "jellyfish", "a/aa", "M%C3%A9duse_%C3%A0_l%27aquarium_de_Monaco_en_novembre_2021.webm", 1920, 1080,
        "Jellyfish in Monaco's aquarium", "Benoit Prieur",
    ),
    commons("mt-hood", "9/99", "Mt_Hood_Aerial_View.webm", 1184, 720, "Over Mount Hood", "Pastelitodepapa"),
    mdn("flower", "flower.mp4", 960, 540, "A flower opening"),
    mdn("friday", "friday.mp4", 640, 480, "Friday"),
)

/** The twelve clips repeated, so a long session of swipes measures memory well past the first round. */
internal val ReelPages: List<SampleClip> = List(SampleClips.size * 4) { SampleClips[it % SampleClips.size] }

private fun commons(key: String, hash: String, file: String, width: Int, height: Int, title: String, author: String) =
    SampleClip(
        clip = VideoClip(
            id = key,
            url = "$COMMONS/$hash/$file",
            posterUrl = "$COMMONS/thumb/$hash/$file/${POSTER_WIDTH}px--$file.jpg",
            width = width,
            height = height,
        ),
        title = title,
        author = author,
    )

private fun mdn(key: String, file: String, width: Int, height: Int, title: String) = SampleClip(
    clip = VideoClip(id = key, url = "$MDN/$file", posterUrl = null, width = width, height = height),
    title = title,
    author = "MDN",
)

private const val COMMONS = "https://upload.wikimedia.org/wikipedia/commons"
private const val MDN = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos"

/** One of the widths Commons renders stills at; it refuses others. */
private const val POSTER_WIDTH = 960
