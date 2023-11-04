package io.johnsonlee.playground.databind

import com.android.ide.common.rendering.api.ViewInfo
import com.fasterxml.jackson.annotation.JsonProperty

abstract class ViewInfoMixin {

    @get:JsonProperty("className")
    abstract val className: String

    @get:JsonProperty("left")
    abstract val left: Int

    @get:JsonProperty("top")
    abstract val top: Int

    @get:JsonProperty("right")
    abstract val right: Int

    @get:JsonProperty("bottom")
    abstract val bottom: Int

    @get:JsonProperty("baseLine")
    abstract val baseLine: Int

    @get:JsonProperty("leftMargin")
    abstract val leftMargin: Int

    @get:JsonProperty("rightMargin")
    abstract val rightMargin: Int

    @get:JsonProperty("topMargin")
    abstract val topMargin: Int

    @get:JsonProperty("bottomMargin")
    abstract val bottomMargin: Int

    @get:JsonProperty("children")
    abstract val children: List<ViewInfo>

}
