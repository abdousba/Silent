package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.*

object LocationHelper {

    data class Mosque(
        val nameAr: String,
        val nameEn: String,
        val latitude: Double,
        val longitude: Double,
        val addressAr: String,
        val addressEn: String,
        var distanceMeter: Float = 0f,
        var bearingDegrees: Float = 0f // Angle relative to North
    ) {
        // Human-readable distance formatted
        fun getFormattedDistance(): String {
            return if (distanceMeter > 1000f) {
                String.format("%.2f كم", distanceMeter / 1000f)
            } else {
                "${distanceMeter.roundToInt()} متر"
            }
        }

        // Compass heading shorthand
        fun getCompassDirection(): String {
            val sections = listOf(
                "شمال ⬆", "شمال شرق ↗", "شرق ➡", "جنوب شرق ↘",
                "جنوب ⬇", "جنوب غرب ↙", "غرب ⬅", "شمال غرب ↖"
            )
            val index = ((bearingDegrees + 22.5) % 360 / 45).toInt()
            return sections[index % 8]
        }
    }

    // Static major mosques in pre-configured cities as beautiful data fallback
    private val cityMosques = mapOf(
        "Makkah" to listOf(
            Mosque("المسجد الحرام", "Al-Masjid al-Haram", 21.4225, 39.8262, "مكة المكرمة، المملكة العربية السعودية", "Makkah, Saudi Arabia"),
            Mosque("مسجد عائشة الراجحي", "Aisha Al Rajhi Mosque", 21.3789, 39.8824, "حي النسيم، مكة المكرمة", "Al Naseem, Makkah"),
            Mosque("مسجد الخيف", "Al-Khaif Mosque", 21.4175, 39.8727, "منى، مكة المكرمة", "Mina, Makkah"),
            Mosque("مسجد نمرة", "Namirah Mosque", 21.3508, 39.8586, "عرفات، مكة المكرمة", "Arafat, Makkah")
        ),
        "Madinah" to listOf(
            Mosque("المسجد النبوي الشريف", "Al-Masjid an-Nabawi", 24.4672, 39.6111, "المدينة المنورة، المملكة العربية السعودية", "Madinah, Saudi Arabia"),
            Mosque("مسجد قباء", "Quba Mosque", 24.4392, 39.6173, "طريق الهجرة، المدينة المنورة", "Hijrah Rd, Madinah"),
            Mosque("مسجد القبلتين", "Al-Qiblatayn Mosque", 24.4839, 39.5786, "طريق خالد بن الوليد، المدينة المنورة", "Khalid bin Al Waleed Rd, Madinah"),
            Mosque("مسجد الميقات", "Miqat Mosque", 24.4132, 39.5445, "ذو الحليفة، المدينة المنورة", "Dhul Hulaifah, Madinah")
        ),
        "Riyadh" to listOf(
            Mosque("جامع الإمام تركي بن عبد الله", "Imam Turki bin Abdullah Mosque", 24.6304, 46.7115, "الديرة، وسط الرياض", "Deerah, Central Riyadh"),
            Mosque("جامع الراجحي الكبير", "Al Rajhi Grand Mosque", 24.6811, 46.7865, "مخرج ١٥، حي الجزيرة، الرياض", "Exit 15, Al Jazira, Riyadh"),
            Mosque("جامع الملك خالد", "King Khalid Grand Mosque", 24.6974, 46.6436, "حي أم الحمام، الرياض", "Umm Al Hamam, Riyadh"),
            Mosque("مسجد جوهرة البابطين", "Jawharat Al Babtain Mosque", 24.8190, 46.6231, "طريق الملك فهد، الرياض", "King Fahd Rd, Riyadh")
        ),
        "Algiers" to listOf(
            Mosque("جامع الجزائر الأعظم", "Djamaa el Djazair", 36.7360, 3.1360, "المحمدية، الجزائر العاصمة", "Mohammadia, Algiers"),
            Mosque("الجامع الجديد", "Djama'a al-Djedid", 36.7845, 3.0632, "القصبة، الجزائر العاصمة", "Casbah, Algiers"),
            Mosque("الجامع الكبير", "Djama'a al-Kebir", 36.7848, 3.0637, "شارع البحرية، الجزائر العاصمة", "Marine St, Algiers"),
            Mosque("مسجد كتشاوة", "Ketchaoua Mosque", 36.7852, 3.0624, "باب الوادي، الجزائر العاصمة", "Bab El Oued, Algiers")
        ),
        "Cairo" to listOf(
            Mosque("جامع الأزهر الشريف", "Al-Azhar Mosque", 30.0457, 31.2627, "الدرب الأحمر، القاهرة القديمة", "El-Darb El-Ahmar, Cairo Landscape"),
            Mosque("مسجد محمد علي", "Muhammad Ali Mosque", 30.0287, 31.2599, "القلعة، الخليفة، القاهرة", "Citadel, El-Khalifa, Cairo"),
            Mosque("مسجد السلطان حسن", "Sultan Hassan Mosque", 30.0321, 31.2562, "ميدان صلاح الدين، القاهرة", "Salah El-Din Sq, Cairo"),
            Mosque("مسجد عمرو بن العاص", "Amr ibn al-As Mosque", 30.0101, 31.2331, "حي الفسطاط، القاهرة القديمة", "Fustat, Old Cairo")
        ),
        "Casablanca" to listOf(
            Mosque("مسجد الحسن الثاني", "Hassan II Mosque", 33.6067, -7.6326, "بالمير، شاطئ الدار البيضاء", "Bd de la Corniche, Casablanca"),
            Mosque("مسجد ولد الحمراء", "Ould el-Hamra Mosque", 33.5992, -7.6186, "المدينة القديمة، الدار البيضاء", "Old Medina, Casablanca"),
            Mosque("مسجد الكودية", "Al-Koudia Mosque", 33.5786, -7.5992, "حي الكودية، الدار البيضاء", "Al-Koudia, Casablanca")
        ),
        "Dubai" to listOf(
            Mosque("مسجد الفاروق عمر بن الخطاب", "Al Farooq Omar Bin Al Khattab", 25.1764, 55.2284, "حي الصفا، دبي", "Al Safa, Dubai"),
            Mosque("جامع دبي الكبير", "Grand Bur Dubai Mosque", 25.2638, 55.2974, "شارع الفهيدي، بر دبي", "Al Fahidi St, Bur Dubai"),
            Mosque("مسجد الرحيم", "Al Rahim Mosque", 25.0747, 55.1328, "مرسى دبي (مارينا)، دبي", "Dubai Marina, Dubai")
        )
    )

    // Function to calculate distance & bearing from start point to end point
    fun calculateDistanceAndBearing(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Pair<Float, Float> {
        val results = FloatArray(2)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        // results[0] contains distance in meters
        // results[1] contains initial bearing in degrees
        val distance = results[0]
        var bearing = results[1]
        if (bearing < 0) {
            bearing += 360f
        }
        return Pair(distance, bearing)
    }

    // Returns beautiful lists of mosques dynamically computed based on coordinates
    fun getNearbyMosques(latitude: Double, longitude: Double, activeCityEn: String): List<Mosque> {
        // Retrieve mosques based on city if possible
        val mosques = (cityMosques[activeCityEn] ?: cityMosques["Makkah"]!!).map {
            it.copy() // Create copies to avoid editing global state
        }

        // Calculate and update distance & bearing
        for (mosque in mosques) {
            val (dist, bear) = calculateDistanceAndBearing(latitude, longitude, mosque.latitude, mosque.longitude)
            mosque.distanceMeter = dist
            mosque.bearingDegrees = bear
        }

        // Sort by closest distance first
        return mosques.sortedBy { it.distanceMeter }
    }

    // Dynamic Mock Mosque Generator around user
    fun generateDynamicNearbyMosques(lat: Double, lng: Double): List<Mosque> {
        val generator = listOf(
            Pair("مسجد التقوى", "Al-Taqwa Mosque") to Pair(0.002, 0.001),
            Pair("جامع الرحمة", "Al-Rahma Mosque") to Pair(-0.003, -0.002),
            Pair("مسجد الفلاح", "Al-Falah Mosque") to Pair(0.0015, -0.0035),
            Pair("جامع النور الكبير", "Al-Noor Grand Mosque") to Pair(-0.004, 0.003),
            Pair("مسجد الإيمان", "Al-Iman Mosque") to Pair(0.0035, 0.002),
            Pair("جامع التوحيد", "Al-Tawhid Mosque") to Pair(-0.001, 0.004)
        )

        val list = generator.map { (names, offset) ->
            val mLat = lat + offset.first
            val mLng = lng + offset.second
            Mosque(
                nameAr = names.first,
                nameEn = names.second,
                latitude = mLat,
                longitude = mLng,
                addressAr = "حي المصلين المجاور، بالقرب منك",
                addressEn = "Worshippers District, Nearby Your Current Location"
            )
        }

        for (mosque in list) {
            val (dist, bear) = calculateDistanceAndBearing(lat, lng, mosque.latitude, mosque.longitude)
            mosque.distanceMeter = dist
            mosque.bearingDegrees = bear
        }

        return list.sortedBy { it.distanceMeter }
    }
}
