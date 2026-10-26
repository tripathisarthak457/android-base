package com.example.cmp.core.perfhud.di

import com.example.cmp.core.common.environment.AppEnvironment
import com.example.cmp.core.devtools.page.DevToolsPage
import com.example.cmp.core.perfhud.R
import com.example.cmp.core.perfhud.overlay.PerformancePill
import com.example.cmp.core.perfhud.panel.PerformanceTab
import com.example.cmp.core.perfhud.recording.PerformanceRecorder
import com.example.cmp.core.perfhud.report.ReportExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object PerfHudModule {

    /**
     * The Performance tab and the pill under the DEV badge. The app's activity is injected with the
     * inspector before it is created, so providing the page here starts the recorder early enough to
     * see the launch's `perf.hud` extra.
     */
    @Provides
    @IntoSet
    fun performancePage(
        recorder: PerformanceRecorder,
        exporter: ReportExporter,
        environment: AppEnvironment,
    ): DevToolsPage = DevToolsPage(
        key = PAGE_KEY,
        title = R.string.perfhud_tab,
        content = {
            PerformanceTab(
                recorder = recorder,
                exporter = exporter,
                debugBuild = environment.isDebugBuild,
            )
        },
        badgeContent = { openPage ->
            PerformancePill(
                recorder = recorder,
                debugBuild = environment.isDebugBuild,
                onOpen = openPage,
            )
        },
    )

    private const val PAGE_KEY = "performance"
}
