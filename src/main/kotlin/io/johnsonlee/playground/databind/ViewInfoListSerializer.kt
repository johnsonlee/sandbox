package io.johnsonlee.playground.databind

import com.android.ide.common.rendering.api.ViewInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.johnsonlee.playground.sandbox.logger

class ViewInfoListSerializer : JsonSerializer<List<ViewInfo>>() {

    override fun serialize(value: List<ViewInfo>, gen: JsonGenerator, serializers: SerializerProvider) {
        with(gen) {
            writeStartArray()
            value.forEach { viewInfo ->
                serializeViewInfo(viewInfo)
            }
            writeEndArray()
        }
    }

    private fun JsonGenerator.serializeViewInfo(viewInfo: ViewInfo) {
        writeStartObject()
        writeObjectField(ViewInfoMixin::className.name, viewInfo.className)
        writeObjectField(ViewInfoMixin::left.name, viewInfo.left.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::top.name, viewInfo.top.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::right.name, viewInfo.right.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::bottom.name, viewInfo.bottom.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::baseLine.name, viewInfo.baseLine.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::leftMargin.name, viewInfo.leftMargin.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::topMargin.name, viewInfo.topMargin.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::rightMargin.name, viewInfo.rightMargin.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeObjectField(ViewInfoMixin::bottomMargin.name, viewInfo.bottomMargin.takeIf { it != Int.MIN_VALUE } ?: 0)
        writeArrayFieldStart(ViewInfoMixin::children.name)
        viewInfo.children.forEach { child ->
            serializeViewInfo(child)
        }
        writeEndArray()
        writeEndObject()
    }

}