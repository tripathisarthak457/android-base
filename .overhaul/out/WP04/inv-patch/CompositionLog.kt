package PKG.perf

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import PKG.startup.LaunchHook
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Investigation only (not part of the template): logs which composables ran in each frame.
@OptIn(InternalComposeTracingApi::class)
object CompositionLog : LaunchHook {
    private val ran = ConcurrentHashMap<String, Int>()
    private var installed = false
    private var frame = 0L

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = true
        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            ran.merge(info, 1, Int::plus)
        }
        override fun traceEventEnd() = Unit
    }

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frame++
            if (ran.isNotEmpty()) {
                val snapshot = HashMap(ran); ran.clear()
                val top = snapshot.entries.filter { "(" in it.key && !it.key.contains("androidx") }
                    .sortedByDescending { it.value }.take(80)
                    .joinToString(" | ") { "${it.key.substringBefore(" (").substringAfterLast('.')} ${it.key.substringAfter(" (").substringBefore(")")} x${it.value}" }
                Log.i("perf.compose", "frame=$frame t=${SystemClock.uptimeMillis()} n=${snapshot.values.sum()} $top")
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override suspend fun beforeStart(intent: Intent) {
        if (installed) return
        installed = true
        withContext(Dispatchers.Main) {
            Composer.setTracer(tracer)
            Choreographer.getInstance().postFrameCallback(callback)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CompositionLogModule {
    @Provides
    @IntoSet
    fun hook(): LaunchHook = CompositionLog
}
