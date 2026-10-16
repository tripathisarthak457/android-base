package PKG.perf

import android.content.Intent
import android.os.Trace
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import PKG.startup.LaunchHook
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Investigation only (not part of the template): every composable that runs becomes an atrace section
// named after it, so a Perfetto capture shows what each one cost inside a frame.
@OptIn(InternalComposeTracingApi::class)
object CompositionTrace : LaunchHook {
    private var installed = false

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = true

        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            Trace.beginSection(info.take(MAX_SECTION))
        }

        override fun traceEventEnd() = Trace.endSection()
    }

    override suspend fun beforeStart(intent: Intent) {
        if (installed) return
        installed = true
        withContext(Dispatchers.Main) { Composer.setTracer(tracer) }
    }

    private const val MAX_SECTION = 127
}

@Module
@InstallIn(SingletonComponent::class)
object CompositionTraceModule {
    @Provides
    @IntoSet
    fun hook(): LaunchHook = CompositionTrace
}
