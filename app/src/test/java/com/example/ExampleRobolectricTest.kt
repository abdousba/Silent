package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("خشوع", appName)
  }

  @Test
  fun `calculate and print Algiers prayer times`() {
    val calendar = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.JUNE)
      set(Calendar.DAY_OF_MONTH, 11)
    }
    
    // Exact step-by-step trace
    val latitude = 36.7538
    val longitude = 3.0588
    val timezoneOffset = 1.0
    val year = 2026
    val month = 6
    val day = 11

    var y = year
    var m = month
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val a = Math.floor(y / 100.0)
    val b = 2.0 - a + Math.floor(a / 4.0)
    val jdValue = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5
    val jd = jdValue - longitude / (15.0 * 24.0)

    val d = jd - 2451545.0
    
    fun fixAngle(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    val g = fixAngle(357.529 + 0.98560028 * d)
    val q = fixAngle(280.459 + 0.98564736 * d)
    val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2.0 * g)))

    val obliq = 23.439 - 0.00000036 * d
    val declination = Math.toDegrees(asin(sin(Math.toRadians(obliq)) * sin(Math.toRadians(l))))
    
    var ra = Math.toDegrees(Math.atan2(cos(Math.toRadians(obliq)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
    ra = fixAngle(ra)
    
    var eotDiff = q - ra
    if (eotDiff < -180.0) eotDiff += 360.0
    if (eotDiff > 180.0) eotDiff -= 360.0
    val rawEquationOfTime = eotDiff * 4.0

    val midDayDecimal = 12.0 + timezoneOffset - (longitude / 15.0) - (rawEquationOfTime / 60.0)
    
    val shadowFactor = 1.0
    val latMinusDecl = Math.toRadians(Math.abs(latitude - declination))
    val asrAngleRad = atan(1.0 / (shadowFactor + tan(latMinusDecl)))
    val asrAngleAlt = Math.toDegrees(asrAngleRad)
    val asrAngleZenith = 90.0 - asrAngleAlt

    val timings = PrayerTimesCalculator.calculateTimes(
      calendar = calendar,
      latitude = latitude,
      longitude = longitude,
      timezoneOffset = timezoneOffset,
      method = PrayerTimesCalculator.CalculationMethod.ALGERIA,
      juristicRule = PrayerTimesCalculator.JuristicRule.STANDARD
    )
    org.junit.Assert.assertNotNull(timings)
    org.junit.Assert.assertTrue(timings.fajr.isNotEmpty())
    org.junit.Assert.assertTrue(timings.sunrise.isNotEmpty())
    org.junit.Assert.assertTrue(timings.dhuhr.isNotEmpty())
    org.junit.Assert.assertTrue(timings.asr.isNotEmpty())
    org.junit.Assert.assertTrue(timings.maghrib.isNotEmpty())
    org.junit.Assert.assertTrue(timings.isha.isNotEmpty())
  }
}
