package io.johnsonlee.playground.sandbox

import android.view.BridgeInflater
import com.android.layoutlib.bridge.android.BridgeContext

val BridgeContext.layoutInflater: BridgeInflater
    get() = getSystemService("layout_inflater") as BridgeInflater