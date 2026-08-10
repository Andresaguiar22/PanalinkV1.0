package com.example.creative.beauty

import com.example.PanaApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = PanaApplication::class)
class BeautyStudioProcessorTest {

    @Test
    fun testBeautyConfigFilterCreation() {
        val config = BeautyConfig(
            skinSmoothing = 0.8f,
            smartLighting = 0.6f,
            warmthAdjustment = 0.7f
        )

        val colorFilter = BeautyStudioProcessor.createBeautyColorFilter(config)
        assertNotNull(colorFilter)
    }
}

