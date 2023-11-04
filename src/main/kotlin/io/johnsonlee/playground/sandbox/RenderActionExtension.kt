package io.johnsonlee.playground.sandbox

import android.view.BridgeInflater
import com.android.ide.common.rendering.api.RenderParams
import com.android.layoutlib.bridge.android.BridgeContext
import com.android.layoutlib.bridge.impl.RenderAction

fun <T : RenderParams> RenderAction<T>.getContext(): BridgeContext {
    return RenderAction::class.java.getDeclaredMethod("getContext").apply {
        isAccessible = true
    }.invoke(this) as BridgeContext
}

fun <T : RenderParams> RenderAction<T>.getLayoutInflater(): BridgeInflater {
    return this.getContext().getSystemService("layout_inflater") as BridgeInflater
}
