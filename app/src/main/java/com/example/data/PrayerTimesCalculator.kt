package com.example.data

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

object PrayerTimesCalculator {

    enum class CalculationMethod(
        val fajrAngle: Double,
        val ishaUseAngle: Boolean,
        val ishaAngle: Double,
        val ishaIntervalMin: Int = 0 // Used specifically for Umm Al-Qura (e.g. 90 minutes)
    ) {
        UMM_AL_QURA(18.5, false, 0.0, 90),
        EGYPT_SURVEY(19.5, true, 17.5),
        MWL(18.0, true, 17.0),
        ISNA(15.0, true, 15.0),
        KARACHI(18.0, true, 18.0),
        GULF(19.5, false, 0.0, 90),
        ALGERIA(18.0, true, 17.0)
    }

    enum class JuristicRule {
        STANDARD, // Shafi'i, Maliki, Hanbali
        HANAFI
    }

    // Coordinates of major Arab cities as fallbacks
    data class CityConfig(
        val nameAr: String,
        val nameEn: String,
        val latitude: Double,
        val longitude: Double,
        val timezone: Double,
        val method: CalculationMethod
    )

    val preconfiguredCities = listOf(
        CityConfig("مكة المكرمة", "Makkah", 21.4225, 39.8262, 3.0, CalculationMethod.UMM_AL_QURA),
        CityConfig("المدينة المنورة", "Madinah", 24.4672, 39.6111, 3.0, CalculationMethod.UMM_AL_QURA),
        CityConfig("الرياض", "Riyadh", 24.7136, 46.6753, 3.0, CalculationMethod.UMM_AL_QURA),
        CityConfig("الجزائر", "Algiers", 36.7538, 3.0588, 1.0, CalculationMethod.ALGERIA),
        CityConfig("القاهرة", "Cairo", 30.0444, 31.2357, 2.0, CalculationMethod.EGYPT_SURVEY),
        CityConfig("الدار البيضاء", "Casablanca", 33.5731, -7.5898, 1.0, CalculationMethod.MWL),
        CityConfig("دبي", "Dubai", 25.2048, 55.2708, 4.0, CalculationMethod.GULF),
        CityConfig("بغداد", "Baghdad", 33.3152, 44.3661, 3.0, CalculationMethod.MWL),
        CityConfig("عمان", "Amman", 31.9539, 35.9106, 3.0, CalculationMethod.MWL),
        CityConfig("المنامة", "Manama", 26.2285, 50.5860, 3.0, CalculationMethod.GULF)
    )

    data class PrayerTimings(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
        val fajrMinutes: Int,
        val sunriseMinutes: Int,
        val dhuhrMinutes: Int,
        val asrMinutes: Int,
        val maghribMinutes: Int,
        val ishaMinutes: Int
    )

    fun calculateTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double,
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        juristicRule: JuristicRule = JuristicRule.STANDARD
    ): PrayerTimings {
        // 1. Calculate Day of the Year
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val jd = getJulianDate(year, month, day) - longitude / (15.0 * 24.0)

        // 2. Sun's position logic
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2.0 * g)))

        val obliq = 23.439 - 0.00000036 * d
        val declination = Math.toDegrees(asin(sin(Math.toRadians(obliq)) * sin(Math.toRadians(l))))
        val rawEquationOfTime = 9.87 * sin(Math.toRadians(2.0 * l)) - 7.53 * cos(Math.toRadians(g)) - 1.5 * sin(Math.toRadians(g))
        
        // Dhuhr calculation (noon point)
        val midDayDecimal = 12.0 + timezoneOffset - (longitude / 15.0) - (rawEquationOfTime / 60.0)

        // Hour angle equation helper
        fun hourAngle(angle: Double, sign: Int): Double {
            val radiansLat = Math.toRadians(latitude)
            val radiansDecl = Math.toRadians(declination)
            val cosH = (sin(Math.toRadians(-angle)) - sin(radiansLat) * sin(radiansDecl)) / (cos(radiansLat) * cos(radiansDecl))
            if (cosH > 1.0 || cosH < -1.0) {
                return Double.NaN
            }
            val h = Math.toDegrees(acos(cosH))
            return if (sign == -1) -h else h
        }

        // 3. Fajr Hour Angle
        val fajrHA = hourAngle(method.fajrAngle, -1)
        val fajrTime = if (fajrHA.isNaN()) midDayDecimal - 1.5 else midDayDecimal + (fajrHA / 15.0)

        // 4. Sunrise Hour Angle (standard angle 0.833)
        val sunriseHA = hourAngle(0.833, -1)
        val sunriseTime = if (sunriseHA.isNaN()) midDayDecimal - 1.0 else midDayDecimal + (sunriseHA / 15.0)

        // 5. Asr calculation (shadow ratio)
        val shadowFactor = if (juristicRule == JuristicRule.HANAFI) 2.0 else 1.0
        val latMinusDecl = Math.toRadians(abs(latitude - declination))
        val asrAngleRad = atan(1.0 / (shadowFactor + tan(latMinusDecl)))
        val asrAngle = 90.0 - Math.toDegrees(asrAngleRad)
        val asrHA = hourAngle(asrAngle, 1)
        val asrTime = if (asrHA.isNaN()) midDayDecimal + 2.5 else midDayDecimal + (asrHA / 15.0)

        // 6. Maghrib Hour Angle (standard angle 0.833 for sunset)
        val maghribHA = hourAngle(0.833, 1)
        val maghribTime = if (maghribHA.isNaN()) midDayDecimal + 1.0 else midDayDecimal + (maghribHA / 15.0)

        // 7. Isha Hour Angle / Interval
        val ishaTime = if (method.ishaUseAngle) {
            val ishaHA = hourAngle(method.ishaAngle, 1)
            if (ishaHA.isNaN()) midDayDecimal + 2.5 else midDayDecimal + (ishaHA / 15.0)
        } else {
            // Interval based (like Umm Al-Qura, add 90 minutes to Maghrib)
            val maghribMinutes = (maghribTime * 60).roundToInt()
            val ishaMinutes = maghribMinutes + method.ishaIntervalMin
            ishaMinutes / 60.0
        }

        // Helper to format decimal hour to HH:MM string and store minutes of the day
        fun formatDecimal(valMinutes: Int): String {
            val hrs = (valMinutes / 60) % 24
            val mns = valMinutes % 60
            return String.format("%02d:%02d", hrs, mns)
        }

        fun clampMinutes(decimalHrs: Double): Int {
            var minutes = (decimalHrs * 60.0).roundToInt()
            if (minutes < 0) minutes += 1440
            if (minutes >= 1440) minutes %= 1440
            return minutes
        }

        val fM = clampMinutes(fajrTime)
        val sM = clampMinutes(sunriseTime)
        val dM = clampMinutes(midDayDecimal)
        val aM = clampMinutes(asrTime)
        val mM = clampMinutes(maghribTime)
        val iM = clampMinutes(ishaTime)

        return PrayerTimings(
            fajr = formatDecimal(fM),
            sunrise = formatDecimal(sM),
            dhuhr = formatDecimal(dM),
            asr = formatDecimal(aM),
            maghrib = formatDecimal(mM),
            isha = formatDecimal(iM),
            fajrMinutes = fM,
            sunriseMinutes = sM,
            dhuhrMinutes = dM,
            asrMinutes = aM,
            maghribMinutes = mM,
            ishaMinutes = iM
        )
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle
        a %= 360.0
        if (a < 0) a += 360.0
        return a
    }
}
