package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = PanaApplication::class)
class ExampleRobolectricTest {
  @Test
  fun `launch main activity and dump stacktrace if it fails`() {
    try {
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.onActivity { activity ->
        }
      }
      throw RuntimeException("Did not crash!")
    } catch (e: Exception) {
      e.printStackTrace()
      throw e
    }
  }
}
