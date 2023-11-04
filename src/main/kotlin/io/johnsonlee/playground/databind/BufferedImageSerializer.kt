package io.johnsonlee.playground.databind

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.johnsonlee.playground.util.toBase64
import java.awt.image.BufferedImage

class BufferedImageSerializer : StdSerializer<BufferedImage>(BufferedImage::class.java) {

    override fun serialize(value: BufferedImage, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toBase64("png"))
    }

}