package com.base.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.base.app.MainActivity
import com.base.app.R

/**
 * A home-screen widget: the app's name and one line the app keeps current.
 *
 * Glance, not the design system: a widget is drawn by the launcher from RemoteViews, so the
 * app's composables cannot run there. It uses the launcher's own colours through [GlanceTheme],
 * which is what makes it sit comfortably beside every other widget on the page.
 *
 * Tapping it opens the app. Change the line with [AppWidgets.setHeadline] — an unread count, the
 * next appointment, today's streak — from wherever that value changes.
 */
class AppWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val headline = currentState<Preferences>()[HEADLINE]
                    ?: LocalContext.current.getString(R.string.widget_default_headline)
                WidgetContent(headline)
            }
        }
    }

    internal companion object {
        val HEADLINE = stringPreferencesKey("headline")
    }
}

@Composable
private fun WidgetContent(headline: String) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = headline,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 2,
        )
    }
}

class AppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AppWidget()
}

object AppWidgets {

    /** Updates every placed copy of the widget. A no-op when none is on the home screen. */
    suspend fun setHeadline(context: Context, headline: String) {
        val widget = AppWidget()
        GlanceAppWidgetManager(context).getGlanceIds(AppWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { it[AppWidget.HEADLINE] = headline }
            widget.update(context, id)
        }
    }
}
