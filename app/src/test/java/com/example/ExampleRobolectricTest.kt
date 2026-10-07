package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.UrlHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vishwa Vinayak Trust", appName)
  }

  @Test
  fun `verify internal url check`() {
    assertTrue(UrlHandler.isInternalUrl("https://vvt.org.in/"))
    assertTrue(UrlHandler.isInternalUrl("https://vvt.org.in/about-us/"))
    assertTrue(UrlHandler.isInternalUrl("https://www.vvt.org.in/contact/"))
    assertFalse(UrlHandler.isInternalUrl("https://google.com/"))
    assertFalse(UrlHandler.isInternalUrl("https://facebook.com/vvt"))
  }
}
