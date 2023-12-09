package decoder

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import javax.sound.sampled.AudioFormat
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class DecoderTest {
    companion object {
        private lateinit var decoder: Decoder

        private val fileUrls = ClassLoader.getSystemResources("files")
            .nextElement()
            .let(URL::getFile)
            .let(::File)
            .listFiles()
            ?.filter(File::exists)
            ?.map(File::getAbsolutePath)!!

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            require(fileUrls.isNotEmpty())
        }
    }

    @BeforeEach
    fun beforeEach() {
        decoder = Decoder.create()
    }

    @AfterEach
    fun afterEach() {
        decoder.close()
    }

    @Test
    fun `static creation`() {
        assertNotNull(Decoder.create())
    }

    @Test
    fun `create a media`() {
        assertNotNull(Decoder.createMedia(fileUrls.random()))
    }

    @Test
    fun `take a snapshot`() = runTest {
        decoder.initialize(fileUrls.first { it.contains("video") })

        assertTrue(decoder.snapshot(0L)?.bytes?.isNotEmpty()!!)
    }

    @Test
    fun `initialize and dispose`() = runTest {
        fileUrls.forEach { fileUrl ->
            decoder.initialize(fileUrl)

            assertTrue(decoder.isInitialized)

            assertNotNull(decoder.media)

            with(decoder.media!!) {

                assertEquals(fileUrl, url)

                assertEquals(File(fileUrl).name, name)

                if (fileUrl.contains("audio")) {
                    assertEquals(
                        AudioFormat(
                            44100F,
                            16,
                            2,
                            true,
                            false
                        ).toString(),
                        audioFormat!!.toString()
                    )
                }

                assertEquals(5.seconds.inWholeNanoseconds, durationNanos)

                assertEquals(frameRate, frameRate)

                if (fileUrl.contains("video")) assertEquals(500 to 500, size)
            }

            decoder.dispose()

            assertFalse(decoder.isInitialized)

            assertNull(decoder.media)
        }
    }

    @Test
    fun `decode media`() = runTest {

        val fileUrl = fileUrls.random()

        decoder.initialize(fileUrl)

        val audioFrames = mutableListOf<DecodedFrame.Audio>()

        val videoFrames = mutableListOf<DecodedFrame.Video>()

        var endFrame: DecodedFrame.End? = null

        while (endFrame == null) {
            decoder.nextFrame()?.let { frame ->
                when (frame) {
                    is DecodedFrame.Audio -> audioFrames.add(frame)
                    is DecodedFrame.Video -> videoFrames.add(frame)
                    is DecodedFrame.End -> {
                        endFrame = frame
                    }
                }
            }
        }

        if (fileUrl.contains("audio")) assertTrue(audioFrames.isNotEmpty())

        if (fileUrl.contains("video")) assertTrue(videoFrames.isNotEmpty())

        assertEquals(DecodedFrame.End(5.seconds.inWholeNanoseconds), endFrame)

        decoder.dispose()
    }

    @Test
    fun `seek to desired timestamp and return actual position`() = runTest {
        decoder.initialize(fileUrls.random())

        var previousTimestamp: Long? = null

        repeat(5) {
            val randomTimestampMicros =
                (0L..decoder.media!!.durationNanos).random().nanoseconds.inWholeMicroseconds

            assertNotEquals(previousTimestamp, decoder.seekTo(randomTimestampMicros))

            previousTimestamp = randomTimestampMicros
        }
    }
}