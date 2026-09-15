package com.fedeveloper95.med.services

import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.elements.MainActivity.Tabs.getScheduledMedsForDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Regression tests for the edit rules that protect taken history:
 * every path through [MedUpdatePlanner.plan] (no-op, supply-only, plain
 * rebuild, range rebuild) plus the Stats dedupe helper.
 */
class MedUpdatePlannerTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val noon: LocalTime = LocalTime.of(12, 0)
    private val eightAm: LocalTime = LocalTime.of(8, 0)

    private fun med(
        id: Long,
        title: String = "Sertraline",
        time: LocalTime = noon,
        start: LocalDate = LocalDate.of(2026, 8, 15),
        end: LocalDate? = null,
        history: Map<LocalDate, LocalTime> = emptyMap(),
        supplyLeft: Int? = null,
        groupId: Long? = 1L
    ): MedData = MedData(
        id = id,
        groupId = groupId,
        type = ItemType.Medicine,
        title = title,
        creationDate = start,
        creationTime = time,
        takenHistory = HashMap(history),
        endDate = end,
        supplyDosesLeft = supplyLeft,
        supplyDosesPerRefill = if (supplyLeft != null) 30 else null,
        supplyLowThreshold = if (supplyLeft != null) 4 else null
    )

    private fun hist(vararg days: Int): Map<LocalDate, LocalTime> =
        days.associate { today.withDayOfMonth(it) to LocalTime.of(12, 30) }

    private val noopRequest = MedUpdatePlanner.Request(
        originalItem = med(1),
        title = "Sertraline",
        iconName = null,
        colorCode = null,
        times = listOf(noon),
        days = null,
        notes = null,
        intervalGap = null,
        selectedDate = today
    )

    // ------------------------------------------------------------- no-op path

    @Test
    fun `unchanged save plans None and does not rebuild`() {
        val plan = MedUpdatePlanner.plan(noopRequest, null, listOf(med(1, history = hist(1, 2))))
        assertEquals(MedUpdatePlanner.Plan.None, plan)
    }

    @Test
    fun `unchanged save with supply change plans SupplyOnly`() {
        val supply = InventoryEntry(dosesLeft = 39, dosesPerRefill = 90, lowThreshold = 4)
        val plan = MedUpdatePlanner.plan(noopRequest, supply, listOf(med(1, supplyLeft = 40)))
        assertEquals(MedUpdatePlanner.Plan.SupplyOnly, plan)
    }

    @Test
    fun `non-medicine unchanged save plans None without supply handling`() {
        val event = med(1).copy(type = ItemType.Event)
        val request = noopRequest.copy(originalItem = event)
        val plan = MedUpdatePlanner.plan(request, InventoryEntry(10, 30, 4), listOf(event))
        assertEquals(MedUpdatePlanner.Plan.None, plan)
    }

    @Test
    fun `reordered days set is still unchanged`() {
        val item = med(1, history = hist(1)).copy(recurrenceDays = listOf(DayOfWeek.TUESDAY, DayOfWeek.MONDAY))
        val request = noopRequest.copy(
            originalItem = item,
            days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        )
        val plan = MedUpdatePlanner.plan(request, null, listOf(item))
        assertEquals(MedUpdatePlanner.Plan.None, plan)
    }

    // ---------------------------------------------------------- rebuild paths

    @Test
    fun `real edit rebuilds with full history preserved`() {
        val history = hist(1, 2, 3)
        val request = noopRequest.copy(title = "Sertraline HCL")
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = history))) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(setOf(1L), plan.removeIds)
        assertEquals(1, plan.entries.size)
        assertEquals("Sertraline HCL", plan.entries[0].title)
        assertEquals(history, plan.entries[0].takenHistory)
    }

    @Test
    fun `editing one slot preserves sibling slots and their history`() {
        val morning = med(1, time = eightAm, history = hist(1, 2))
        val evening = med(2, time = LocalTime.of(20, 0), history = hist(2, 3))
        val request = noopRequest.copy(originalItem = morning, title = "Renamed", times = listOf(eightAm))

        val plan = MedUpdatePlanner.plan(request, null, listOf(morning, evening)) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(setOf(1L, 2L), plan.removeIds)
        assertEquals(2, plan.entries.size)
        val rebuiltEvening = plan.entries.first { it.creationTime == LocalTime.of(20, 0) }
        assertEquals("Renamed", rebuiltEvening.title)
        assertEquals(hist(2, 3), rebuiltEvening.takenHistory)
        val rebuiltMorning = plan.entries.first { it.creationTime == eightAm }
        assertEquals(hist(1, 2), rebuiltMorning.takenHistory)
    }

    @Test
    fun `changed time carries its history to the new time`() {
        val history = hist(5, 10)
        val request = noopRequest.copy(times = listOf(LocalTime.of(13, 0)))
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = history))) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(1, plan.entries.size)
        assertEquals(LocalTime.of(13, 0), plan.entries[0].creationTime)
        assertEquals(history, plan.entries[0].takenHistory)
    }

    @Test
    fun `rebuild entries get fresh group and placeholder ids`() {
        val request = noopRequest.copy(title = "New")
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = hist(1)))) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(0L, plan.entries[0].id)
        assertEquals(MedUpdatePlanner.NEW_GROUP, plan.entries[0].groupId)
    }

    @Test
    fun `group scoping is type-safe so events sharing a groupId survive`() {
        val medicine = med(1, history = hist(1))
        val event = med(2).copy(type = ItemType.Event)
        val request = noopRequest.copy(originalItem = medicine, title = "Renamed")

        val plan = MedUpdatePlanner.plan(request, null, listOf(medicine, event)) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(setOf(1L), plan.removeIds)
        assertFalse(plan.removeIds.contains(event.id))
    }

    // ----------------------------------------------------------- range edits

    @Test
    fun `only-this-event edit splits history around the edited day`() {
        // Took Sep 1, 2, 14 (today), and 20; renaming only today's entry.
        val history = hist(1, 2, 14, 20)
        val request = noopRequest.copy(title = "Renamed", rangeStart = null, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = history))) as MedUpdatePlanner.Plan.Rebuild

        val edited = plan.entries.first { it.groupId == MedUpdatePlanner.NEW_GROUP }
        assertEquals(today, edited.creationDate)
        assertEquals(today, edited.endDate)
        assertEquals(mapOf(today to LocalTime.of(12, 30)), edited.takenHistory)

        val before = plan.entries.filter { it.groupId == 1L && it.endDate != null }
        assertEquals(1, before.size)
        assertEquals(today.minusDays(1), before[0].endDate)
        assertEquals(hist(1, 2), before[0].takenHistory)

        val after = plan.entries.filter { it.groupId == 1L && it.endDate == null }
        assertEquals(1, after.size)
        assertEquals(today.plusDays(1), after[0].creationDate)
        assertEquals(hist(20), after[0].takenHistory)
    }

    @Test
    fun `only-this-event edit on never-taken med produces no phantom fragments`() {
        val request = noopRequest.copy(title = "Renamed", rangeStart = null, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = emptyMap()))) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(1, plan.entries.size) // only the edited day
    }

    @Test
    fun `only-this-event edit keeps later records as an after fragment`() {
        // Med starts today, already logged a future date (Sep 20): the before
        // fragment is empty and must be dropped, but the Sep 20 record is real
        // history and must survive as an after fragment.
        val item = med(1, start = today, history = hist(20))
        val request = noopRequest.copy(originalItem = item, title = "Renamed", rangeStart = null, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, null, listOf(item)) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(2, plan.entries.size) // edited day + after fragment
        val after = plan.entries.filter { it.endDate == null }
        assertEquals(1, after.size)
        assertEquals(hist(20), after[0].takenHistory)
    }

    @Test
    fun `all-following edit keeps before fragment with earlier history only`() {
        val history = hist(1, 2, 20)
        val request = noopRequest.copy(title = "Renamed", rangeStart = -1L, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = history))) as MedUpdatePlanner.Plan.Rebuild

        val edited = plan.entries.first { it.groupId == MedUpdatePlanner.NEW_GROUP }
        assertEquals(today, edited.creationDate)
        assertNull(edited.endDate)
        // Everything from editStart onward follows the ongoing entry.
        assertEquals(hist(20), edited.takenHistory)

        val before = plan.entries.first { it.groupId == 1L }
        assertEquals(today.minusDays(1), before.endDate)
        assertEquals(hist(1, 2), before.takenHistory)
    }

    @Test
    fun `range edit on never-taken med produces no phantom fragments`() {
        val request = noopRequest.copy(title = "Renamed", rangeStart = null, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, null, listOf(med(1, history = emptyMap()))) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(1, plan.entries.size)
    }

    @Test
    fun `range edit applies supply to the rebuilt window`() {
        val supply = InventoryEntry(dosesLeft = 7, dosesPerRefill = 30, lowThreshold = 2)
        val request = noopRequest.copy(title = "Renamed", rangeStart = null, rangeEnd = null)
        val plan = MedUpdatePlanner.plan(request, supply, listOf(med(1, supplyLeft = 39, history = hist(1)))) as MedUpdatePlanner.Plan.Rebuild

        val edited = plan.entries.first { it.groupId == MedUpdatePlanner.NEW_GROUP }
        assertEquals(7, edited.supplyDosesLeft)
        assertFalse(edited.supplyAlertShown)
    }

    // ------------------------------------------------------ multi-slot range

    @Test
    fun `range edit covers every slot of a multi-dose med`() {
        val morning = med(1, time = eightAm, history = hist(1, 5, 14, 20))
        val evening = med(2, time = LocalTime.of(20, 0), history = hist(2, 5, 14, 20))
        val request = noopRequest.copy(
            originalItem = morning,
            title = "Renamed",
            times = listOf(eightAm),
            rangeStart = null,
            rangeEnd = null
        )

        val plan = MedUpdatePlanner.plan(request, null, listOf(morning, evening)) as MedUpdatePlanner.Plan.Rebuild

        assertEquals(setOf(1L, 2L), plan.removeIds)
        val editedSlots = plan.entries.filter { it.groupId == MedUpdatePlanner.NEW_GROUP }
        assertEquals(2, editedSlots.size)
        assertEquals(setOf(eightAm, LocalTime.of(20, 0)), editedSlots.map { it.creationTime }.toSet())
        // Both slots keep their taken record for the edited day.
        assertTrue(editedSlots.all { it.takenHistory.containsKey(today) })
        // After-fragments exist for both slots and keep their own later history.
        val afterFragments = plan.entries.filter { it.groupId == 1L && it.endDate == null }
        assertEquals(2, afterFragments.size)
        assertTrue(afterFragments.all { it.takenHistory.containsKey(today.plusDays(1).withDayOfMonth(20)) })
    }

    // ------------------------------------------------------------ Stats dedupe

    @Test
    fun `stats dedupes duplicate schedule fragments`() {
        val original = med(1, history = hist(1))
        val phantom = med(2) // same title/time, no end date — duplicate fragment
        val duplicate = med(3)
        val items = listOf(original, phantom, duplicate, med(4, title = "Other"))

        val scheduled = getScheduledMedsForDate(today, items)
        assertEquals(
            listOf("Sertraline" to noon, "Other" to noon),
            scheduled.map { it.title to it.creationTime }
        )
    }

    @Test
    fun `stats keeps distinct time slots of the same med`() {
        val morning = med(1, time = eightAm)
        val evening = med(2, time = LocalTime.of(20, 0))
        val scheduled = getScheduledMedsForDate(today, listOf(morning, evening))
        assertEquals(2, scheduled.size)
    }
}
