package converter

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.experimental.and

/**
 * An instance of [FrameConverter] that provides conversion of [Frame] to [ByteArray].
 */
internal class ByteArrayFrameConverter : AutoCloseable, FrameConverter<ByteArray>() {

    /**
     * No need for conversion
     */
    override fun convert(bytes: ByteArray?) = null

    /**
     * Convert AUDIO or VIDEO based on frame type
     */
    override fun convert(frame: Frame) = when (frame.type) {
        Frame.Type.AUDIO -> ::getDecodedSamples
        Frame.Type.VIDEO -> ::getDecodedImage
        else -> null
    }?.invoke(frame)

    /**
     * S16LE audio bytes conversion
     */
    private fun getDecodedSamples(frame: Frame) =
        frame.takeIf { it.type == Frame.Type.AUDIO && it.samples != null }?.runCatching {
            (samples?.firstOrNull() as? ShortBuffer)?.run {

                if (audioChannels <= 0 || sampleRate <= 0) return null

                val dataSize = remaining() * audioChannels

                val bytes = ByteArray(dataSize)

                rewind()

                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(this).clear()

                bytes
            }
        }?.getOrNull()

    /**
     * BGRA image bytes conversion
     */
    private fun getDecodedImage(frame: Frame): ByteArray? =
        frame.takeIf { it.type == Frame.Type.VIDEO && it.image != null }?.runCatching {
            (image?.firstOrNull() as? ByteBuffer)?.run {
                val dataSize = imageWidth * imageHeight * imageChannels
                val bytes = ByteArray(dataSize)

                rewind()

                for (dstIndex in 0 until dataSize step imageChannels) {
                    val y = dstIndex / (imageWidth * imageChannels)
                    val x = (dstIndex / imageChannels) % imageWidth
                    val srcIndex = y * imageStride + x * imageChannels

                    repeat(imageChannels) { c ->
                        bytes[dstIndex + c] = get(srcIndex + c) and 0xFF.toByte()
                    }
                }

                bytes
            }
        }?.getOrNull()

    override fun close() = super.close()
}