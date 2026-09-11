package com.store88.pos.ui.customer

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.store88.pos.UiState
import com.store88.pos.ui.theme.Store88Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Shows [CustomerDisplayScreen] on a secondary Display (客顯) when available.
 * Only uses [DisplayManager.DISPLAY_CATEGORY_PRESENTATION] displays to avoid
 * attaching to the cashier screen (which can ANR on single-display devices).
 */
class CustomerDisplayController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateOwner: SavedStateRegistryOwner,
    private val uiFlow: StateFlow<UiState>,
    @Suppress("unused") private val scope: CoroutineScope,
) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var presentation: CustomerDisplayPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            mainHandler.post { attachIfNeeded() }
        }
        override fun onDisplayRemoved(displayId: Int) {
            mainHandler.post {
                if (presentation?.display?.displayId == displayId) dismiss()
                attachIfNeeded()
            }
        }
        override fun onDisplayChanged(displayId: Int) {
            mainHandler.post { attachIfNeeded() }
        }
    }

    val hasSecondaryDisplay: Boolean
        get() = findCustomerDisplay() != null

    fun start() {
        displayManager.registerDisplayListener(displayListener, mainHandler)
        // Defer so MainActivity onCreate / first frame are not blocked
        mainHandler.post { attachIfNeeded() }
    }

    fun stop() {
        runCatching { displayManager.unregisterDisplayListener(displayListener) }
        dismiss()
    }

    fun refresh() {
        mainHandler.post { attachIfNeeded() }
    }

    private fun attachIfNeeded() {
        val display = findCustomerDisplay()
        if (display == null) {
            dismiss()
            return
        }
        val current = presentation
        if (current != null && current.display.displayId == display.displayId && current.isShowing) {
            return
        }
        dismiss()
        try {
            val p = CustomerDisplayPresentation(context, display, lifecycleOwner, savedStateOwner, uiFlow)
            p.show()
            presentation = p
            Log.i(TAG, "Customer display shown on displayId=${display.displayId} name=${display.name}")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to show customer display", t)
            presentation = null
        }
    }

    private fun dismiss() {
        runCatching { presentation?.dismiss() }
        presentation = null
    }

    /**
     * Prefer true presentation / customer displays only.
     * Do not fall back to "any non-default" display — that can pick the same UI
     * surface on some tablets/emulators and freeze the app.
     */
    private fun findCustomerDisplay(): Display? {
        val activityDisplayId = try {
            context.display?.displayId
        } catch (_: Throwable) {
            Display.DEFAULT_DISPLAY
        } ?: Display.DEFAULT_DISPLAY

        val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val candidate = presentationDisplays.firstOrNull { d ->
            d.displayId != activityDisplayId &&
                d.displayId != Display.DEFAULT_DISPLAY &&
                d.state != Display.STATE_OFF
        }
        if (candidate != null) return candidate

        // Some OEM dual-screen POS expose the customer panel without PRESENTATION category
        return displayManager.displays.firstOrNull { d ->
            d.displayId != activityDisplayId &&
                d.displayId != Display.DEFAULT_DISPLAY &&
                d.state != Display.STATE_OFF &&
                looksLikeCustomerPanel(d)
        }
    }

    private fun looksLikeCustomerPanel(d: Display): Boolean {
        val name = d.name.orEmpty().lowercase()
        return name.contains("customer") ||
            name.contains("presentation") ||
            name.contains("hdmi") ||
            name.contains("overlay") ||
            name.contains("second") ||
            name.contains("副") ||
            name.contains("客")
    }

    companion object {
        private const val TAG = "CustomerDisplay"
    }
}

private class CustomerDisplayPresentation(
    outerContext: Context,
    display: Display,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateOwner: SavedStateRegistryOwner,
    private val uiFlow: StateFlow<UiState>,
) : Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Store88Theme {
                    val ui by uiFlow.collectAsState()
                    CustomerDisplayScreen(ui)
                }
            }
        }
        setContentView(composeView)
    }
}

/** In-app preview for single-screen tablets / emulator (same UI as 客顯). */
@Composable
fun CustomerDisplayPreviewHost(ui: UiState) {
    Store88Theme {
        CustomerDisplayScreen(ui)
    }
}
