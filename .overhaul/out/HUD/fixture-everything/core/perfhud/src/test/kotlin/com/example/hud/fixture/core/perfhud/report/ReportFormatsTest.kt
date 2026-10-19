package com.example.hud.fixture.core.perfhud.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hud.fixture.core.perfhud.checks.BuildFacts
import com.example.hud.fixture.core.perfhud.checks.ProfileStatus
import com.example.hud.fixture.core.perfhud.checks.StrictModeExample
import com.example.hud.fixture.core.perfhud.checks.StrictModeSummary
import com.example.hud.fixture.core.perfhud.frame.FrameStats
import com.example.hud.fixture.core.perfhud.frame.FrameSummary
import com.example.hud.fixture.core.perfhud.frame.ScreenFrames
import com.example.hud.fixture.core.perfhud.network.NetworkTimings
import com.example.hud.fixture.core.perfhud.recording.AppFacts
import com.example.hud.fixture.core.perfhud.recording.DeviceFacts
import com.example.hud.fixture.core.perfhud.recording.PerformanceSnapshot
import com.example.hud.fixture.core.perfhud.score.OptimisationScore
import com.example.hud.fixture.core.perfhud.score.ScoreInputs
import com.example.hud.fixture.core.perfhud.startup.StartupSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ReportFormatsTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `the JSON carries every screen and each lost point's fix`() {
        val report = PerformanceReport.of(snapshot(), GENERATED_AT, resources)

        val json = Json.parseToJsonElement(report.toJson()).jsonObject

        assertEquals(PerformanceReport.SCHEMA, json.getValue("schema").jsonPrimitive.int)
        val screens = json.getValue("frames").jsonObject.getValue("screens").jsonArray
        assertEquals(listOf("Home", "Detail"), screens.map { it.jsonObject.getValue("name").jsonPrimitive.content })
        val janky = json.getValue("score").jsonObject.getValue("metrics").jsonArray
            .map { it.jsonObject }
            .single { it.getValue("metric").jsonPrimitive.content == "JankyFrames" }
        assertEquals("C", janky.getValue("grade").jsonPrimitive.content)
        assertTrue(janky.getValue("fix").jsonPrimitive.content.startsWith("Open Janky frames by screen"))
        assertEquals("docs/PERFORMANCE-OVERLAY.md#janky-frames", janky.getValue("doc").jsonPrimitive.content)
    }

    @Test
    fun `a debug build's report says its numbers are pessimistic, in both formats`() {
        val snapshot = snapshot(debugBuild = true)

        val json = PerformanceReport.of(snapshot, GENERATED_AT, resources)
        val markdown = MarkdownReport(resources).write(snapshot, GENERATED_AT)

        assertTrue(json.notes.single().contains("devRelease"))
        assertTrue(markdown, markdown.contains("> **Debug build: these numbers are pessimistic**"))
    }

    @Test
    fun `the Markdown leads with the score and lists the worst screen first`() {
        val markdown = MarkdownReport(resources).write(snapshot(), GENERATED_AT)

        val score = markdown.indexOf("## Optimisation score")
        val frames = markdown.indexOf("## Frames")
        assertTrue(markdown, score in 0 until frames)
        assertTrue(markdown, markdown.indexOf("| Detail |") in frames until markdown.indexOf("| Home |"))
        assertTrue(markdown, markdown.contains("- **Janky frames** −12.5 of 25. Open Janky frames by screen"))
    }

    @Test
    fun `before the frames are graded neither format gives a score`() {
        val snapshot = snapshot(frameCount = 120)

        val json = Json.parseToJsonElement(PerformanceReport.of(snapshot, GENERATED_AT, resources).toJson()).jsonObject
        val markdown = MarkdownReport(resources).write(snapshot, GENERATED_AT)

        val score = json.getValue("score").jsonObject
        assertEquals(JsonNull, score.getValue("total"))
        assertEquals(JsonNull, score.getValue("grade"))
        assertTrue(markdown, markdown.contains("Not enough recorded to score yet: the score needs 300 frames"))
    }

    @Test
    fun `StrictMode examples name the kind and where, from the module's copy`() {
        val examples = listOf(
            StrictModeExample("DiskRead", "com.example.app.Store.load(Store.kt:12)"),
            StrictModeExample("Network", frame = null),
        )

        val markdown = MarkdownReport(resources).write(snapshot(examples = examples), GENERATED_AT)

        assertTrue(markdown, markdown.contains("- `DiskRead at com.example.app.Store.load(Store.kt:12)`"))
        assertTrue(markdown, markdown.contains("- `Network at an unknown frame`"))
    }

    private fun snapshot(
        debugBuild: Boolean = false,
        frameCount: Int = 1000,
        examples: List<StrictModeExample> = emptyList(),
    ): PerformanceSnapshot {
        val frames = FrameSummary(
            overall = stats(frames = frameCount, janky = frameCount * 15 / 1000),
            uiP50Millis = 2f,
            uiP90Millis = 3f,
            uiP99Millis = 5f,
            uiP90BudgetShare = 0.36f,
            uiP99BudgetShare = 0.6f,
            framesPerSecond = 118f,
            refreshHz = 120f,
            screens = listOf(
                ScreenFrames("Home", isOther = false, stats = stats(frames = 900, janky = 5)),
                ScreenFrames("Detail", isOther = false, stats = stats(frames = 100, janky = 10)),
            ),
        )
        val build = BuildFacts(debuggable = debugBuild, minified = !debugBuild, profile = ProfileStatus.Compiled)
        val strictMode = StrictModeSummary(
            supported = true,
            violations = examples.size,
            byKind = examples.groupingBy { it.kind }.eachCount(),
            examples = examples,
        )
        return PerformanceSnapshot(
            frames = frames,
            memory = null,
            startup = StartupSummary.Unsupported,
            build = build,
            strictMode = strictMode,
            network = NetworkTimings.None,
            score = OptimisationScore.of(ScoreInputs(frames, null, null, build, strictMode)),
            recordedMillis = 95_000L,
            app = AppFacts("com.example.app.dev", "1.0.0-devRelease", "dev", debugBuild),
            device = DeviceFacts("Xiaomi", "M2101K6I", "17", 37, listOf(120f, 60f)),
        )
    }

    private fun stats(frames: Int, janky: Int) = FrameStats(
        frames = frames,
        janky = janky,
        slow = 0,
        frozen = 0,
        longWork = 0,
        p50Millis = 3f,
        p90Millis = 4f,
        p99Millis = 7f,
        worstMillis = 12f,
    )

    private companion object {
        const val GENERATED_AT = "2026-10-18T15:00:00+05:30"
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
