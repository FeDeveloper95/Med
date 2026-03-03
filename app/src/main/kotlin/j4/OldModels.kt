package j4

import androidx.annotation.Keep
import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.HashMap

@Keep
enum class s { Event, Medicine, a, b, c, d }

@Keep
data class p1(
    val d: Long = 0L,
    val e: Long? = null,
    val f: s = s.Event,
    val g: String = "",
    val h: String? = null,
    val i: String? = null,
    val j: String? = null,
    val k: LocalDate = LocalDate.now(),
    val l: LocalTime = LocalTime.now(),
    val m: HashMap<LocalDate, LocalTime> = HashMap(),
    val n: List<DayOfWeek>? = null,
    val o: LocalDate? = null
) : Serializable