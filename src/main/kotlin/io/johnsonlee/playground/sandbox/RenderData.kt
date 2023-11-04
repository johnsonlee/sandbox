package io.johnsonlee.playground.sandbox

import com.android.ide.common.rendering.api.ViewInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.johnsonlee.playground.databind.BufferedImageSerializer
import io.johnsonlee.playground.databind.ViewInfoListSerializer
import java.awt.image.BufferedImage

data class RenderData(
    @get:JsonSerialize(using = ViewInfoListSerializer::class)
    val systemViews: List<ViewInfo>,
    @get:JsonSerialize(using = ViewInfoListSerializer::class)
    val rootViews: List<ViewInfo>,
    @get:JsonSerialize(using = BufferedImageSerializer::class)
    val image: BufferedImage
)
