package audio

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AudioSamplerTest {

    private var audioSampler: AudioSampler? = null

    @BeforeEach
    fun beforeEach() {
        audioSampler = AudioSampler.create(44100F)
    }

    @AfterEach
    fun afterEach() {
        audioSampler?.close()
        audioSampler = null
    }

    @Test
    fun `static creation`() {
        assertInstanceOf(AudioSampler::class.java, AudioSampler.create(44100F))
    }

    @Test
    fun `playback cycle`() = runTest {
        audioSampler!!.apply {
            assertNotNull(start())

            assertNotNull(play(byteArrayOf()))

            assertNotNull(stop())
        }
    }

    @Test
    fun `mute toggling`() = runTest {
        audioSampler!!.apply {
            assertDoesNotThrow {
                repeat(5) {
                    val newState = arrayOf(true, false).random()
                    assertEquals(
                        newState,
                        runBlocking { setMuted(newState) }
                    )
                }
            }
        }
    }

    @Test
    fun `volume changing`() = runTest {
        audioSampler!!.apply {

            assertEquals(null, setVolume(-1.0f))

            repeat(10) {
                val newVolume = Random.nextDouble(0.0, 1.0).toFloat()
                assertEquals(
                    newVolume,
                    setVolume(newVolume)
                )
            }
        }
    }
}