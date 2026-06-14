package com.example.ui

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class AzkarModel(
    val id: String,
    val text: String,
    val count: Int,
    val explanation: String = "",
    val reference: String = ""
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AzkarTab() {
    val context = LocalContext.current
    var selectedCategory by rememberSaveable { mutableStateOf(0) } // 0: دخول المسجد, 1: الخروج من المسجد, 2: أذكار الصلاة
    
    val mosqueEntryAzkar = remember {
        listOf(
            AzkarModel(
                "me1",
                "أَعُوذُ بِاللهِ الْعَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ. [بِسْمِ اللهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللهِ]، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ.",
                1,
                "إِذَا دَخَلَ الرَّجُلُ الْمَسْجِدَ فَلْيُصَلِّ عَلَى النَّبِيِّ وَلْيَقُلْ هَذَا الدُّعَاءَ.",
                "رواه أبو داود والنسائي"
            ),
            AzkarModel(
                "me2",
                "بِسْمِ اللهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ، اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ.",
                1,
                "يقال عند دخول المسجد والبدء بالصلاة والذكر.",
                "رواه ابن ماجه"
            ),
            AzkarModel(
                "me3",
                "اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُوراً، وَفِي لِسَانِي نُوراً، وَاجْعَلْ فِي سَمْعِي نُوراً، وَاجْعَلْ فِي بَصَرِي نُوراً، وَاجْعَلْ مِنْ خَلْفِي نُوراً، وَمِنْ أَمَامِي نُوراً، وَاجْعَلْ مِنْ فَوْقِي نُوراً، وَمِنْ تَحْتِي نُوراً. اللَّهُمَّ أَعْطِنِي نُوراً.",
                1,
                "دعاء الذهاب إلى المسجد ودخوله لتحصيل النور والسكينة.",
                "رواه مسلم"
            )
        )
    }

    val mosqueExitAzkar = remember {
        listOf(
            AzkarModel(
                "mx1",
                "بِسْمِ اللهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ، اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ.",
                1,
                "يُعصم العبد به من كيد الشيطان ووساوسه عند خروجه إلى مصالح دنياه.",
                "رواه مسلم ص"
            ),
            AzkarModel(
                "mx2",
                "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ.",
                1,
                "صلاة العبد منجاة وسلام بعد الفراغ من الصلاة بالمسجد.",
                "رواه النسائي"
            )
        )
    }

    val prayerAfterAzkar = remember {
        listOf(
            AzkarModel(
                "pa1",
                "أَسْتَغْفِرُ اللهَ (ثَلَاثاً)\nاللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ ذَا الْجَلَالِ وَالْإِكْرَامِ.",
                1,
                "الاستغفار ثلاثاً عقب الصلاة مباشرة لتطهير أي نقص في أدائها.",
                "رواه مسلم"
            ),
            AzkarModel(
                "pa2",
                "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ، لَا إِلَهَ إِلَّا اللهُ وَلَا نَعْبُدُ إِلَّا إِيَّاهُ، لَهُ النِّعْمَةُ وَلَهُ الْفَضْلُ وَلَهُ الثَّنَاءُ الْحَسَنُ، لَا إِلَهَ إِلَّا اللهُ مُخْلِصِينَ لَهُ الدِّينَ وَلَوْ كَرِهَ الْكَافِرُونَ.",
                1,
                "ذكر عظيم العاقبة للتأكيد على التوحيد والإخلاص بعد كل فريضة.",
                "رواه مسلم"
            ),
            AzkarModel(
                "pa3",
                "سُبْحَانَ اللهِ",
                33,
                "التسبيح ثلاثاً وثلاثين لغسل المعاصي ورفع الدرجات في الجنة.",
                "رواه البخاري ومسلم"
            ),
            AzkarModel(
                "pa4",
                "الْحَمْدُ للهِ",
                33,
                "التحميد ثلاثاً وثلاثين حامداً لله تبارك وتعالى على نعمة الإسلام والصلوات.",
                "رواه البخاري ومسلم"
            ),
            AzkarModel(
                "pa5",
                "اللهُ أَكْبَرُ",
                33,
                "التكبير ثلاثاً وثلاثين إعظاماً لله جل جلاله على نصره وعبادته.",
                "رواه البخاري ومسلم"
            ),
            AzkarModel(
                "pa6",
                "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                1,
                "تمام المئة بعد التسبيح والتحميد والتكبير ليغفر الله خطايا العبد وإن كانت مثل زبد البحر.",
                "رواه مسلم"
            ),
            AzkarModel(
                "pa7",
                "قراءة آية الكرسي:\nاللَّهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ وَلَا يَئُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ.",
                1,
                "من قرأ آية الكرسي دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا الموت.",
                "رواه النسائي في السنن الكبرى"
            ),
            AzkarModel(
                "pa8",
                "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ.",
                1,
                "وصية النبي ﷺ لمعاذ بن جبل ألا يدع هذا الدعاء بعد دبر كل صلاة.",
                "رواه أبو داود"
            )
        )
    }

    val currentAzkarList = when (selectedCategory) {
        0 -> mosqueEntryAzkar
        1 -> mosqueExitAzkar
        else -> prayerAfterAzkar
    }

    // State key for tracking user clicks independently between tab switches
    var progressMap = remember { mutableStateMapOf<String, Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmeraldDeepDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "الأذكار والأدعية المستحبة 🤲",
            color = SandText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "أذكار الدخول والخروج من المسجد النبوي والصلوات المكتوبة",
            color = SlateGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp)
        )

        // Custom Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(EmeraldContainer, RoundedCornerShape(12.dp))
                .border(1.dp, IslamicGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val categories = listOf("دخول المسجد", "الخروج من المسجد", "أذكار الصلاة")
            categories.forEachIndexed { index, title ->
                val isSelected = selectedCategory == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) IslamicGold else Color.Transparent)
                        .clickable { selectedCategory = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) EmeraldDeepDark else SandText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action panel or reset tracker buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    progressMap.clear()
                    Toast.makeText(context, "تم إعادة تعيين العدادات بنجاح 🔄", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .background(EmeraldContainer, CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Counter", tint = IslamicGold, modifier = Modifier.size(18.dp))
            }
            Text(
                text = "انقر على الكارت لتعديل العداد وتتبع التسبيح باللمس 👇",
                color = SlateGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Azkar list renderer
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(currentAzkarList) { item ->
                val rawCount = progressMap[item.id] ?: 0
                val current = if (rawCount >= item.count) item.count else rawCount
                val isCompleted = current >= item.count

                AzkarCardItem(
                    item = item,
                    currentCount = current,
                    isCompleted = isCompleted,
                    onItemClick = {
                        if (!isCompleted) {
                            progressMap[item.id] = current + 1
                            triggerVibration(context)
                        } else {
                            // Cycle/Reset simple click triggers
                            progressMap[item.id] = 0
                            triggerVibration(context, durationMs = 50L)
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AzkarCardItem(
    item: AzkarModel,
    currentCount: Int,
    isCompleted: Boolean,
    onItemClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCompleted) IslamicGold.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                IslamicEmerald.copy(alpha = 0.12f) // Muted highlight
            } else {
                EmeraldContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header Row: Count Status and copy helper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.text))
                        Toast.makeText(context, "تم نسخ الذكر إلى الحافظة ✨", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ",
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "العدد المستهدف: ${item.count} / الحالي: $currentCount",
                        color = if (isCompleted) IslamicGold else SlateGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isCompleted) IslamicGold else SlateGray.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "مكتمل",
                                tint = EmeraldDeepDark,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = (item.count - currentCount).toString(),
                                color = IslamicGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The main Arabic text of Zikr
            Text(
                text = item.text,
                color = if (isCompleted) SandText else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            if (item.explanation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldDeepDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.explanation,
                        color = SlateGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 15.sp
                    )
                }
            }

            if (item.reference.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.reference,
                    color = IslamicGold.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Light feedback vibration
fun triggerVibration(context: Context, durationMs: Long = 40L) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(durationMs)
        }
    } catch (e: Exception) {
        // Fallback for security permissions or headless devices
    }
}
