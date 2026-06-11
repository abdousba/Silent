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
        CityConfig("القاهرة", "Cairo", 30.0444, 31.2357, 2.0, CalculationMethod.EGYPT_SURVEY),
        CityConfig("الدار البيضاء", "Casablanca", 33.5731, -7.5898, 1.0, CalculationMethod.MWL),
        CityConfig("دبي", "Dubai", 25.2048, 55.2708, 4.0, CalculationMethod.GULF),
        CityConfig("بغداد", "Baghdad", 33.3152, 44.3661, 3.0, CalculationMethod.MWL),
        CityConfig("عمان", "Amman", 31.9539, 35.9106, 3.0, CalculationMethod.MWL),
        CityConfig("المنامة", "Manama", 26.2285, 50.5860, 3.0, CalculationMethod.GULF),

        // --- 58 WILAYAS OF ALGERIA ---
        CityConfig("01- أدرار", "01- Adrar", 27.879, -0.293, 1.0, CalculationMethod.ALGERIA),
        CityConfig("02- الشلف", "02- Chlef", 36.166, 1.333, 1.0, CalculationMethod.ALGERIA),
        CityConfig("03- الأغواط", "03- Laghouat", 33.800, 2.883, 1.0, CalculationMethod.ALGERIA),
        CityConfig("04- أم البواقي", "04- Oum El Bouaghi", 35.875, 7.114, 1.0, CalculationMethod.ALGERIA),
        CityConfig("05- باتنة", "05- Batna", 35.556, 6.174, 1.0, CalculationMethod.ALGERIA),
        CityConfig("06- بجاية", "06- Béjaïa", 36.751, 5.083, 1.0, CalculationMethod.ALGERIA),
        CityConfig("07- بسكرة", "07- Biskra", 34.850, 5.733, 1.0, CalculationMethod.ALGERIA),
        CityConfig("08- بشار", "08- Béchar", 31.617, -2.217, 1.0, CalculationMethod.ALGERIA),
        CityConfig("09- البليدة", "09- Blida", 36.470, 2.828, 1.0, CalculationMethod.ALGERIA),
        CityConfig("10- البويرة", "10- Bouira", 36.375, 3.900, 1.0, CalculationMethod.ALGERIA),
        CityConfig("11- تمنراست", "11- Tamanrasset", 22.785, 5.523, 1.0, CalculationMethod.ALGERIA),
        CityConfig("12- تبسة", "12- Tébessa", 35.404, 8.124, 1.0, CalculationMethod.ALGERIA),
        CityConfig("13- تلمسان", "13- Tlemcen", 34.878, -1.315, 1.0, CalculationMethod.ALGERIA),
        CityConfig("14- تيارت", "14- Tiaret", 35.371, 1.330, 1.0, CalculationMethod.ALGERIA),
        CityConfig("15- تيزي وزو", "15- Tizi Ouzou", 36.712, 4.046, 1.0, CalculationMethod.ALGERIA),
        CityConfig("16- الجزائر العاصمة", "16- Algiers", 36.7538, 3.0588, 1.0, CalculationMethod.ALGERIA),
        CityConfig("17- الجلفة", "17- Djelfa", 34.672, 3.253, 1.0, CalculationMethod.ALGERIA),
        CityConfig("18- جيجل", "18- Jijel", 36.821, 5.766, 1.0, CalculationMethod.ALGERIA),
        CityConfig("19- سطيف", "19- Sétif", 36.191, 5.414, 1.0, CalculationMethod.ALGERIA),
        CityConfig("20- سعيدة", "20- Saïda", 34.830, 0.151, 1.0, CalculationMethod.ALGERIA),
        CityConfig("21- سكيكدة", "21- Skikda", 36.879, 6.903, 1.0, CalculationMethod.ALGERIA),
        CityConfig("22- سيدي بلعباس", "22- Sidi Bel Abbès", 35.190, -0.630, 1.0, CalculationMethod.ALGERIA),
        CityConfig("23- عنابة", "23- Annaba", 36.900, 7.767, 1.0, CalculationMethod.ALGERIA),
        CityConfig("24- قالمة", "24- Guelma", 36.462, 7.429, 1.0, CalculationMethod.ALGERIA),
        CityConfig("25- قسنطينة", "25- Constantine", 36.365, 6.615, 1.0, CalculationMethod.ALGERIA),
        CityConfig("26- المدية", "26- Médéa", 36.264, 2.754, 1.0, CalculationMethod.ALGERIA),
        CityConfig("27- مستغانم", "27- Mostaganem", 35.933, 0.089, 1.0, CalculationMethod.ALGERIA),
        CityConfig("28- المسيلة", "28- M'Sila", 35.706, 4.542, 1.0, CalculationMethod.ALGERIA),
        CityConfig("29- معسكر", "29- Mascara", 35.400, 0.133, 1.0, CalculationMethod.ALGERIA),
        CityConfig("30- ورقلة", "30- Ouargla", 31.950, 5.330, 1.0, CalculationMethod.ALGERIA),
        CityConfig("31- وهران", "31- Oran", 35.698, -0.631, 1.0, CalculationMethod.ALGERIA),
        CityConfig("32- البيض", "32- El Bayadh", 33.683, 1.019, 1.0, CalculationMethod.ALGERIA),
        CityConfig("33- إليزي", "33- Illizi", 26.508, 8.482, 1.0, CalculationMethod.ALGERIA),
        CityConfig("34- برج بوعريريج", "34- Bordj Bou Arréridj", 36.072, 4.761, 1.0, CalculationMethod.ALGERIA),
        CityConfig("35- بومرداس", "35- Boumerdès", 36.760, 3.473, 1.0, CalculationMethod.ALGERIA),
        CityConfig("36- الطارف", "36- El Tarf", 36.767, 8.314, 1.0, CalculationMethod.ALGERIA),
        CityConfig("37- تندوف", "37- Tindouf", 27.676, -8.147, 1.0, CalculationMethod.ALGERIA),
        CityConfig("38- تيسمسيلت", "38- Tissemsilt", 35.607, 1.810, 1.0, CalculationMethod.ALGERIA),
        CityConfig("39- الوادي", "39- El Oued", 33.368, 6.867, 1.0, CalculationMethod.ALGERIA),
        CityConfig("40- خنشلة", "40- Khenchela", 35.436, 7.143, 1.0, CalculationMethod.ALGERIA),
        CityConfig("41- سوق أهراس", "41- Souk Ahras", 36.286, 7.951, 1.0, CalculationMethod.ALGERIA),
        CityConfig("42- تيبازة", "42- Tipaza", 36.592, 2.448, 1.0, CalculationMethod.ALGERIA),
        CityConfig("43- ميلة", "43- Mila", 36.450, 6.264, 1.0, CalculationMethod.ALGERIA),
        CityConfig("44- عين الدفلى", "44- Aïn Defla", 36.263, 1.970, 1.0, CalculationMethod.ALGERIA),
        CityConfig("45- النعامة", "45- Naâma", 33.266, -0.316, 1.0, CalculationMethod.ALGERIA),
        CityConfig("46- عين تموشنت", "46- Aïn Témouchent", 35.298, -1.140, 1.0, CalculationMethod.ALGERIA),
        CityConfig("47- غرداية", "47- Ghardaïa", 32.490, 3.673, 1.0, CalculationMethod.ALGERIA),
        CityConfig("48- غليزان", "48- Relizane", 35.740, 0.556, 1.0, CalculationMethod.ALGERIA),
        CityConfig("49- المغير", "49- El M'Ghair", 33.950, 5.922, 1.0, CalculationMethod.ALGERIA),
        CityConfig("50- المنيعة", "50- El Meniaa", 30.583, 2.883, 1.0, CalculationMethod.ALGERIA),
        CityConfig("51- أولاد جلال", "51- Ouled Djellal", 34.428, 5.068, 1.0, CalculationMethod.ALGERIA),
        CityConfig("52- برج باجي مختار", "52- Bordj Badji Mokhtar", 21.327, 0.954, 1.0, CalculationMethod.ALGERIA),
        CityConfig("53- بني عباس", "53- Béni Abbès", 30.083, -2.167, 1.0, CalculationMethod.ALGERIA),
        CityConfig("54- عين صالح", "54- In Salah", 27.195, 2.483, 1.0, CalculationMethod.ALGERIA),
        CityConfig("55- عين قزام", "55- In Guezzam", 19.569, 5.767, 1.0, CalculationMethod.ALGERIA),
        CityConfig("56- تقرت", "56- Touggourt", 32.958, 6.064, 1.0, CalculationMethod.ALGERIA),
        CityConfig("57- جانت", "57- Djanet", 24.550, 9.483, 1.0, CalculationMethod.ALGERIA),
        CityConfig("58- تيميمون", "58- Timimoun", 29.263, 0.231, 1.0, CalculationMethod.ALGERIA)
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
        
        // Precise Right Ascension (RA) in degrees
        var ra = Math.toDegrees(atan2(cos(Math.toRadians(obliq)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
        ra = fixAngle(ra)
        
        // Precise Equation of Time (EoT) in minutes
        var eotDiff = q - ra
        if (eotDiff < -180.0) eotDiff += 360.0
        if (eotDiff > 180.0) eotDiff -= 360.0
        val rawEquationOfTime = eotDiff * 4.0
        
        // Dhuhr calculation (noon point)
        val midDayDecimal = 12.0 + timezoneOffset - (longitude / 15.0) - (rawEquationOfTime / 60.0)

        // Hour angle equation helper
        fun hourAngle(angle: Double, sign: Int, isBelowHorizon: Boolean = true): Double {
            val targetAngle = if (isBelowHorizon) -angle else angle
            val radiansLat = Math.toRadians(latitude)
            val radiansDecl = Math.toRadians(declination)
            val cosH = (sin(Math.toRadians(targetAngle)) - sin(radiansLat) * sin(radiansDecl)) / (cos(radiansLat) * cos(radiansDecl))
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
        val asrAngle = Math.toDegrees(asrAngleRad)
        val asrHA = hourAngle(asrAngle, 1, false)
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
