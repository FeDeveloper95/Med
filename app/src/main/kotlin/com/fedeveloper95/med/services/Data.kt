package com.fedeveloper95.med.services

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.Keep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.PREF_SORT_ORDER
import com.fedeveloper95.med.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections
import kotlin.math.max

sealed class EditItem {
    abstract val uniqueId: String

    data class Card(val med: MedData) : EditItem() {
        override val uniqueId = "card_${med.id}"
    }

    data class Group(val divider: MedData, val cards: List<MedData>) : EditItem() {
        override val uniqueId = "group_${divider.id}"
    }
}

/** Supply (inventory) settings for a medicine, as entered in the editor. */
data class InventoryEntry(
    val dosesLeft: Int,
    val dosesPerRefill: Int,
    val lowThreshold: Int
)

@Keep
data class MedData(
    val id: Long = System.currentTimeMillis(),
    val groupId: Long? = null,
    val type: ItemType,
    val title: String,
    val iconName: String? = null,
    val colorCode: String? = null,
    val frequencyLabel: String? = null,
    val creationDate: LocalDate,
    val creationTime: LocalTime = LocalTime.now(),
    val takenHistory: HashMap<LocalDate, LocalTime> = HashMap(),
    val recurrenceDays: List<DayOfWeek>? = null,
    val endDate: LocalDate? = null,
    val notes: String? = null,
    val displayOrder: Int = 0,
    val intervalGap: Int? = null,
    val category: String? = null,
    val notificationType: Int = 0,
    val supplyDosesLeft: Int? = null,
    val supplyDosesPerRefill: Int? = null,
    val supplyLowThreshold: Int? = null,
    val supplyAlertShown: Boolean = false
) : Serializable {

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("groupId", groupId ?: JSONObject.NULL)
        json.put("type", type.name)
        json.put("title", title)
        json.put("iconName", iconName ?: JSONObject.NULL)
        json.put("colorCode", colorCode ?: JSONObject.NULL)
        json.put("frequencyLabel", frequencyLabel ?: JSONObject.NULL)
        json.put("creationDate", creationDate.toString())
        json.put("creationTime", creationTime.toString())

        val historyObj = JSONObject()
        takenHistory.forEach { (k, v) ->
            historyObj.put(k.toString(), v.toString())
        }
        json.put("takenHistory", historyObj)

        val recArray = JSONArray()
        recurrenceDays?.forEach { recArray.put(it.name) }
        json.put("recurrenceDays", if (recurrenceDays == null) JSONObject.NULL else recArray)

        json.put("endDate", endDate?.toString() ?: JSONObject.NULL)
        json.put("notes", notes ?: JSONObject.NULL)
        json.put("displayOrder", displayOrder)
        json.put("intervalGap", intervalGap ?: JSONObject.NULL)
        json.put("category", category ?: JSONObject.NULL)
        json.put("notificationType", notificationType)
        json.put("supplyDosesLeft", supplyDosesLeft ?: JSONObject.NULL)
        json.put("supplyDosesPerRefill", supplyDosesPerRefill ?: JSONObject.NULL)
        json.put("supplyLowThreshold", supplyLowThreshold ?: JSONObject.NULL)
        json.put("supplyAlertShown", supplyAlertShown)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): MedData {
            val history = HashMap<LocalDate, LocalTime>()
            val historyObj = json.optJSONObject("takenHistory")
            if (historyObj != null) {
                historyObj.keys().forEach { key ->
                    history[LocalDate.parse(key)] = LocalTime.parse(historyObj.getString(key))
                }
            }

            val recDays: List<DayOfWeek>? = if (json.isNull("recurrenceDays")) null else {
                val arr = json.getJSONArray("recurrenceDays")
                val list = mutableListOf<DayOfWeek>()
                for (i in 0 until arr.length()) {
                    list.add(DayOfWeek.valueOf(arr.getString(i)))
                }
                list
            }

            return MedData(
                id = json.getLong("id"),
                groupId = if (json.isNull("groupId")) null else json.getLong("groupId"),
                type = ItemType.valueOf(json.getString("type")),
                title = json.getString("title"),
                iconName = if (json.isNull("iconName")) null else json.getString("iconName"),
                colorCode = if (json.isNull("colorCode")) null else json.getString("colorCode"),
                frequencyLabel = if (json.isNull("frequencyLabel")) null else json.getString("frequencyLabel"),
                creationDate = LocalDate.parse(json.getString("creationDate")),
                creationTime = LocalTime.parse(json.getString("creationTime")),
                takenHistory = history,
                recurrenceDays = recDays,
                endDate = if (json.isNull("endDate")) null else LocalDate.parse(json.getString("endDate")),
                notes = if (json.isNull("notes")) null else json.getString("notes"),
                displayOrder = json.optInt("displayOrder", 0),
                intervalGap = if (json.isNull("intervalGap")) null else json.getInt("intervalGap"),
                category = if (json.isNull("category")) null else json.getString("category"),
                notificationType = json.optInt("notificationType", 0),
                supplyDosesLeft = if (json.isNull("supplyDosesLeft")) null else json.optInt("supplyDosesLeft"),
                supplyDosesPerRefill = if (json.isNull("supplyDosesPerRefill")) null else json.optInt("supplyDosesPerRefill"),
                supplyLowThreshold = if (json.isNull("supplyLowThreshold")) null else json.optInt("supplyLowThreshold"),
                supplyAlertShown = json.optBoolean("supplyAlertShown", false)
            )
        }
    }
}

class MigrationObjectInputStream(inStream: InputStream) : ObjectInputStream(inStream) {
    override fun readClassDescriptor(): ObjectStreamClass {
        val desc = super.readClassDescriptor()
        return try {
            val clazz = Class.forName(desc.name)
            ObjectStreamClass.lookup(clazz) ?: desc
        } catch (e: Exception) {
            desc
        }
    }
}

object DataRepository {
    private const val OLD_FILE = "med_data.dat"
    private const val NEW_FILE = "med_data_v2.json"

    fun loadData(context: Context): List<MedData> {
        val newFile = File(context.filesDir, NEW_FILE)
        if (newFile.exists()) {
            try {
                val jsonString = newFile.readText()
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<MedData>()
                for (i in 0 until jsonArray.length()) {
                    list.add(MedData.fromJson(jsonArray.getJSONObject(i)))
                }
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val migrated = migrateLegacyData(context)
        if (migrated.isNotEmpty()) {
            saveData(context, migrated)
        }
        return migrated
    }

    fun saveData(context: Context, items: List<MedData>) {
        try {
            val jsonArray = JSONArray()
            items.forEach { jsonArray.put(it.toJson()) }
            File(context.filesDir, NEW_FILE).writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Every data mutation funnels through here — keep the supply widget current.
        MedSupplyWidgetProvider.updateAll(context)
    }

    private fun migrateLegacyData(context: Context): List<MedData> {
        val oldFile = File(context.filesDir, OLD_FILE)
        if (!oldFile.exists()) return emptyList()

        try {
            val fis = FileInputStream(oldFile)
            val ois = MigrationObjectInputStream(fis)

            val oldList = ois.readObject() as? ArrayList<*> ?: return emptyList()
            ois.close()

            val migratedList = oldList.mapNotNull { obj ->
                if (obj is j4.p1) {
                    val migratedType = when (obj.f.name) {
                        "Medicine", "b" -> ItemType.Medicine
                        else -> ItemType.Event
                    }

                    MedData(
                        id = obj.d,
                        groupId = obj.e,
                        type = migratedType,
                        title = obj.g,
                        iconName = obj.h,
                        colorCode = obj.i,
                        frequencyLabel = obj.j,
                        creationDate = obj.k,
                        creationTime = obj.l,
                        takenHistory = obj.m,
                        recurrenceDays = obj.n,
                        endDate = obj.o,
                        notes = null,
                        displayOrder = 0,
                        intervalGap = null,
                        category = null,
                        notificationType = 0
                    )
                } else null
            }

            oldFile.renameTo(File(context.filesDir, "med_data_migrated.dat"))
            return migratedList
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }
}

class MedViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = mutableStateListOf<MedData>()
    val items: List<MedData> get() = _items

    var selectedDate by mutableStateOf(LocalDate.now())
    var editItems by mutableStateOf<List<EditItem>>(emptyList())
    var editHasChanges by mutableStateOf(false)

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fedeveloper95.med.RELOAD_DATA") {
                reloadData()
            }
        }
    }

    init {
        InventoryService.createNotificationChannel(application)
        loadData()
        // A supply restored from a backup or rebooted below its threshold must
        // alert without waiting for the next dose event.
        if (InventoryService.evaluateAll(application, _items)) {
            saveData()
        }
        syncToWear()

        // Re-arm dose alarms for every medicine on startup. Alarms die on
        // force-stop or a crashed process, and until now they only came back
        // when the user happened to edit or take a dose.
        _items.filter { it.type == ItemType.Medicine }.forEach { item ->
            try {
                NotificationReceiver.scheduleNotification(getApplication(), item)
            } catch (e: Exception) {
            }
        }

        val filter = IntentFilter("com.fedeveloper95.med.RELOAD_DATA")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                application,
                updateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(updateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadEditItems(date: LocalDate) {
        val initial = _items.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == date
                ItemType.Illness -> false
                ItemType.Medicine -> {
                    val isAfterStart = !date.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !date.isAfter(item.endDate)
                    val isCorrectDay =
                        item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(date.dayOfWeek)
                    val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(
                        item.creationDate,
                        date
                    ) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))

        val result = mutableListOf<EditItem>()
        var currentGroupDivider: MedData? = null
        val currentGroupCards = mutableListOf<MedData>()

        for (item in initial) {
            if (item.iconName == "DIVIDER") {
                if (currentGroupDivider != null) {
                    result.add(EditItem.Group(currentGroupDivider, currentGroupCards.toList()))
                } else if (currentGroupCards.isNotEmpty()) {
                    currentGroupCards.forEach { result.add(EditItem.Card(it)) }
                }

                if (item.title.isNotBlank()) {
                    currentGroupDivider = item
                    currentGroupCards.clear()
                } else {
                    currentGroupDivider = null
                    currentGroupCards.clear()
                }
            } else {
                if (currentGroupDivider != null) {
                    currentGroupCards.add(item)
                } else {
                    result.add(EditItem.Card(item))
                }
            }
        }
        if (currentGroupDivider != null) {
            result.add(EditItem.Group(currentGroupDivider, currentGroupCards.toList()))
        } else if (currentGroupCards.isNotEmpty()) {
            currentGroupCards.forEach { result.add(EditItem.Card(it)) }
        }
        editItems = result
        editHasChanges = false
    }

    fun swapTopEditItems(from: Int, to: Int) {
        val newItems = editItems.toMutableList()
        Collections.swap(newItems, from, to)
        editItems = newItems
        editHasChanges = true
    }

    fun swapInnerEditItems(group: EditItem.Group, from: Int, to: Int) {
        val gIdx = editItems.indexOf(group)
        if (gIdx != -1) {
            val newCards = group.cards.toMutableList()
            Collections.swap(newCards, from, to)
            val newItems = editItems.toMutableList()
            newItems[gIdx] = group.copy(cards = newCards)
            editItems = newItems
            editHasChanges = true
        }
    }

    fun removeEditItem(uniqueId: String) {
        editItems = editItems.filter { it.uniqueId != uniqueId }
        editHasChanges = true
    }

    fun removeGroupComplete(group: EditItem.Group) {
        val newItems = editItems.toMutableList()
        val gIdx = newItems.indexOf(group)
        if (gIdx != -1) {
            newItems.removeAt(gIdx)
            newItems.addAll(gIdx, group.cards.map { EditItem.Card(it) })
            editItems = newItems
            editHasChanges = true
        }
    }

    fun removeCardFromGroup(group: EditItem.Group, cardId: Long) {
        val newItems = editItems.toMutableList()
        val gIdx = newItems.indexOf(group)
        if (gIdx != -1) {
            val newCards = group.cards.filter { it.id != cardId }
            val theCard = group.cards.find { it.id == cardId }
            if (newCards.isEmpty()) {
                newItems.removeAt(gIdx)
                if (theCard != null) newItems.add(gIdx, EditItem.Card(theCard))
            } else {
                newItems[gIdx] = group.copy(cards = newCards)
                if (theCard != null) newItems.add(gIdx + 1, EditItem.Card(theCard))
            }
            editItems = newItems
            editHasChanges = true
        }
    }

    fun addCardToGroup(cardItem: EditItem.Card, targetGroupId: Long) {
        val newItems = editItems.toMutableList()
        newItems.remove(cardItem)
        val gIdx = newItems.indexOfFirst { it is EditItem.Group && it.divider.id == targetGroupId }
        if (gIdx != -1) {
            val group = newItems[gIdx] as EditItem.Group
            newItems[gIdx] = group.copy(cards = group.cards + cardItem.med)
        }
        editItems = newItems
        editHasChanges = true
    }

    fun deleteCardFromGroup(group: EditItem.Group, cardId: Long) {
        val newItems = editItems.toMutableList()
        val gIdx = newItems.indexOf(group)
        if (gIdx != -1) {
            val newCards = group.cards.filter { it.id != cardId }
            if (newCards.isEmpty()) newItems.removeAt(gIdx)
            else newItems[gIdx] = group.copy(cards = newCards)
            editItems = newItems
            editHasChanges = true
        }
    }

    fun addEditGroup(groupName: String, selectedDate: LocalDate) {
        val newDivider = MedData(
            id = System.nanoTime(),
            groupId = System.currentTimeMillis(),
            type = ItemType.Event,
            title = groupName,
            iconName = "DIVIDER",
            colorCode = "dynamic",
            frequencyLabel = "",
            creationDate = selectedDate,
            creationTime = LocalTime.MIN,
            takenHistory = hashMapOf(),
            recurrenceDays = null,
            endDate = null,
            notes = null,
            displayOrder = 0,
            intervalGap = null,
            category = null
        )
        editItems = editItems + EditItem.Group(newDivider, emptyList())
        editHasChanges = true
    }

    fun saveEditOrder(selectedDate: LocalDate) {
        val flatList = mutableListOf<MedData>()
        var lastWasGroup = false

        for (item in editItems) {
            when (item) {
                is EditItem.Card -> {
                    if (lastWasGroup) {
                        flatList.add(
                            MedData(
                                id = System.nanoTime(),
                                groupId = System.currentTimeMillis(),
                                type = ItemType.Event,
                                title = "",
                                iconName = "DIVIDER",
                                colorCode = "dynamic",
                                frequencyLabel = "",
                                creationDate = selectedDate,
                                creationTime = LocalTime.MIN,
                                takenHistory = hashMapOf(),
                                recurrenceDays = null,
                                endDate = null,
                                notes = null,
                                displayOrder = 0,
                                intervalGap = null,
                                category = null
                            )
                        )
                    }
                    flatList.add(item.med)
                    lastWasGroup = false
                }

                is EditItem.Group -> {
                    flatList.add(item.divider)
                    flatList.addAll(item.cards)
                    lastWasGroup = true
                }
            }
        }

        val flatListIds = flatList.map { it.id }.toSet()
        val updatedAllItems = _items.filter {
            it.creationDate != selectedDate || it.iconName != "DIVIDER" || flatListIds.contains(it.id)
        }.toMutableList()

        flatList.forEachIndexed { newIndex, item ->
            val globalIndex = updatedAllItems.indexOfFirst { it.id == item.id }
            if (globalIndex != -1) {
                updatedAllItems[globalIndex] =
                    updatedAllItems[globalIndex].copy(displayOrder = newIndex)
            } else if (item.iconName == "DIVIDER") {
                updatedAllItems.add(item.copy(displayOrder = newIndex))
            }
        }

        _items.clear()
        _items.addAll(updatedAllItems)
        saveData()
        editHasChanges = false
        val prefs =
            getApplication<Application>().getSharedPreferences("med_settings", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_SORT_ORDER, "custom").apply()
        getApplication<Application>().sendBroadcast(
            Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(
                getApplication<Application>().packageName
            )
        )
    }

    fun addItem(
        type: ItemType,
        title: String,
        iconName: String? = null,
        colorCode: String? = null,
        times: List<LocalTime>,
        days: List<DayOfWeek>?,
        notes: String? = null,
        category: String? = null,
        intervalGap: Int? = null,
        notificationType: Int = 0,
        supply: InventoryEntry? = null
    ) {
        val groupId = System.currentTimeMillis()
        val baseDate = selectedDate
        val context = getApplication<Application>()

        var currentOrder = (_items.maxOfOrNull { it.displayOrder } ?: 0) + 1

        val itemsOnDate = _items.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == selectedDate
                ItemType.Illness -> {
                    val isAfterStart = !selectedDate.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !selectedDate.isAfter(item.endDate)
                    isAfterStart && isBeforeEnd
                }

                ItemType.Medicine -> {
                    val isAfterStart = !selectedDate.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !selectedDate.isAfter(item.endDate)
                    val isCorrectDay =
                        item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(
                            selectedDate.dayOfWeek
                        )
                    val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(
                        item.creationDate,
                        selectedDate
                    ) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedBy { it.displayOrder }

        var inGroup = false
        for (item in itemsOnDate) {
            if (item.iconName == "DIVIDER") {
                inGroup = item.title.isNotBlank()
            }
        }
        if (inGroup) {
            val emptyDivider = MedData(
                id = System.nanoTime(),
                groupId = System.currentTimeMillis(),
                type = ItemType.Event,
                title = "",
                iconName = "DIVIDER",
                colorCode = "dynamic",
                frequencyLabel = "",
                creationDate = selectedDate,
                creationTime = LocalTime.MIN,
                takenHistory = hashMapOf(),
                recurrenceDays = null,
                endDate = null,
                notes = null,
                displayOrder = currentOrder++,
                category = null,
                intervalGap = null,
                notificationType = 0
            )
            _items.add(emptyDivider)
        }

        times.forEach { time ->
            val newItem = MedData(
                id = System.nanoTime(),
                groupId = groupId,
                type = type,
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                frequencyLabel = if (intervalGap == 14) context.getString(R.string.frequency_unit_biweek)
                else if (days != null) context.getString(R.string.frequency_specific_days)
                else if (times.size > 1) context.getString(
                    R.string.frequency_daily_multiple,
                    times.size
                )
                else context.getString(R.string.frequency_daily),
                creationDate = baseDate,
                creationTime = time,
                recurrenceDays = days,
                endDate = null,
                notes = notes,
                displayOrder = currentOrder++,
                category = category,
                intervalGap = intervalGap,
                notificationType = notificationType
            )
            _items.add(newItem)
            if (type == ItemType.Medicine) {
                NotificationReceiver.scheduleNotification(getApplication(), newItem)
                applySupplySettings(newItem, supply)
            }
        }
        saveData()
    }

    fun updateItem(
        originalItem: MedData,
        title: String,
        iconName: String?,
        colorCode: String?,
        times: List<LocalTime>,
        days: List<DayOfWeek>?,
        notes: String?,
        intervalGap: Int?,
        notificationType: Int = 0,
        rangeStart: Long? = -2L,
        rangeEnd: Long? = -2L,
        supply: InventoryEntry? = null
    ) {
        val context = getApplication<Application>()
        val freqLabel = if (intervalGap == 14) context.getString(R.string.frequency_unit_biweek)
        else if (days != null) context.getString(R.string.frequency_specific_days)
        else if (times.size > 1) context.getString(
            R.string.frequency_daily_multiple,
            times.size
        )
        else context.getString(R.string.frequency_daily)

        // All edit rules (no-op guard, slot preservation, history inheritance,
        // phantom-fragment dropping) live in the pure, unit-tested planner.
        val plan = MedUpdatePlanner.plan(
            MedUpdatePlanner.Request(
                originalItem = originalItem,
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                times = times,
                days = days,
                notes = notes,
                intervalGap = intervalGap,
                notificationType = notificationType,
                freqLabel = freqLabel,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                selectedDate = selectedDate
            ),
            supply,
            _items.filter { it.type == originalItem.type }
        )

        when (plan) {
            is MedUpdatePlanner.Plan.None -> return

            is MedUpdatePlanner.Plan.SupplyOnly -> {
                applySupplySettings(originalItem, supply)
                return
            }

            is MedUpdatePlanner.Plan.Rebuild -> {
                // Entries being replaced get new IDs — cancel alerts keyed to
                // the old IDs so no orphaned low-supply notification survives.
                plan.removeIds.forEach { id ->
                    _items.firstOrNull { it.id == id }?.let {
                        InventoryService.cancelLowSupplyNotification(context, it)
                    }
                }
                _items.removeAll { it.id in plan.removeIds }

                val newGroupId = System.currentTimeMillis()
                plan.entries.forEachIndexed { i, entry ->
                    val withIds = entry.copy(
                        id = System.nanoTime() + i,
                        groupId = if (entry.groupId == MedUpdatePlanner.NEW_GROUP) newGroupId else entry.groupId
                    )
                    _items.add(withIds)
                    if (withIds.type == ItemType.Medicine) {
                        NotificationReceiver.scheduleNotification(getApplication(), withIds)
                    }
                }
            }
        }
        saveData()
        refreshLowSupplyAlerts()
    }

    /** Re-evaluates every item against its low-supply threshold and persists changes. */
    private fun refreshLowSupplyAlerts() {
        if (InventoryService.evaluateAll(getApplication(), _items)) {
            saveData()
        }
    }

    fun deleteItem(item: MedData, deleteDate: LocalDate) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index == -1) return

        if (!deleteDate.isAfter(item.creationDate)) {
            _items.removeAt(index)
        } else {
            val updatedItem = item.copy(endDate = deleteDate.minusDays(1))
            _items[index] = updatedItem
        }

        val itemsOnDate = _items.filter {
            when (it.type) {
                ItemType.Event -> it.creationDate == deleteDate
                ItemType.Illness -> {
                    val isAfterStart = !deleteDate.isBefore(it.creationDate)
                    val isBeforeEnd = it.endDate == null || !deleteDate.isAfter(it.endDate)
                    isAfterStart && isBeforeEnd
                }

                ItemType.Medicine -> {
                    val isAfterStart = !deleteDate.isBefore(it.creationDate)
                    val isBeforeEnd = it.endDate == null || !deleteDate.isAfter(it.endDate)
                    val isCorrectDay =
                        it.recurrenceDays.isNullOrEmpty() || it.recurrenceDays.contains(deleteDate.dayOfWeek)
                    val isCorrectGap = it.intervalGap == null || ChronoUnit.DAYS.between(
                        it.creationDate,
                        deleteDate
                    ) % it.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))

        var changed = true
        var currentList = itemsOnDate
        val dividersToRemove = mutableSetOf<Long>()
        while (changed) {
            changed = false
            val toRemove = currentList.filterIndexed { i, current ->
                if (current.iconName == "DIVIDER") {
                    val next = currentList.getOrNull(i + 1)
                    next == null || next.iconName == "DIVIDER"
                } else false
            }
            if (toRemove.isNotEmpty()) {
                dividersToRemove.addAll(toRemove.map { it.id })
                currentList = currentList.filterNot { it in toRemove }
                changed = true
            }
        }

        if (dividersToRemove.isNotEmpty()) {
            _items.removeAll { it.id in dividersToRemove }
        }

        saveData()
    }

    /**
     * Applies supply (inventory) settings from the editor to all entries of the
     * item's group. `null` supply turns tracking off. Turning tracking on for a
     * group that had none seeds every dose slot with the full amount; switching
     * to per-refill mode scales an existing running count.
     */
    fun applySupplySettings(item: MedData, supply: InventoryEntry?) {
        val targets = if (item.groupId != null) {
            _items.filter { it.groupId == item.groupId }
        } else {
            listOf(item)
        }

        val refillsEnabled = supply != null && supply.dosesPerRefill > 0
        val scale = if (refillsEnabled && item.supplyDosesPerRefill != null && item.supplyDosesPerRefill > 0) {
            supply.dosesPerRefill.toFloat() / item.supplyDosesPerRefill
        } else 1f

        targets.forEach { target ->
            val newLeft = when {
                supply == null -> null
                target.supplyDosesLeft == null -> supply.dosesLeft
                refillsEnabled -> max(0, Math.round(target.supplyDosesLeft * scale))
                else -> target.supplyDosesLeft
            }
            val index = _items.indexOfFirst { it.id == target.id }
            if (index != -1) {
                _items[index] = target.copy(
                    supplyDosesLeft = newLeft,
                    supplyDosesPerRefill = supply?.dosesPerRefill?.takeIf { refillsEnabled },
                    supplyLowThreshold = supply?.lowThreshold,
                    supplyAlertShown = false
                )
            }
        }
        saveData()
    }

    /**
     * Applies supply settings to the group that was just created by [addItem].
     * The new group carries the entered title on each of its entries.
     */
    fun setSupplyOnNewestGroup(title: String, supply: InventoryEntry?) {
        if (supply == null) return
        val newest = _items.lastOrNull { it.type == ItemType.Medicine && it.title == title } ?: return
        if (supply == null) {
            // Sanity fallback: created without tracking (should not happen when
            // called from the medicine sheet, which omits the argument then).
            return
        }
        applySupplySettings(newest, supply)
    }

    fun restoreItem(item: MedData) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            _items[index] = item
        } else {
            _items.add(item)
        }
        saveData()
    }

    fun toggleMedicine(item: MedData, date: LocalDate) {
        if (item.type != ItemType.Medicine) return
        if (date.isAfter(LocalDate.now())) return

        val index = _items.indexOfFirst { it.id == item.id }
        if (index == -1) return
        // Work from the stored item, not the (possibly stale) UI copy: a dose
        // logged from a notification or watch may have changed history or stock
        // since this card was rendered.
        val current = _items[index]
        val newHistory = HashMap(current.takenHistory)
        if (newHistory.containsKey(date)) newHistory.remove(date) else newHistory[date] =
            LocalTime.now()

        _items[index] = applyInventoryChange(
            getApplication(),
            current.copy(takenHistory = newHistory),
            isTaken = newHistory.containsKey(date)
        )
        saveData()

        // Re-arm the alarm so a dose logged early (or un-done) updates the
        // schedule immediately, matching the notification's Take action.
        try {
            NotificationReceiver.scheduleNotification(getApplication(), _items[index])
        } catch (e: Exception) {
        }
    }

    fun confirmIllness(item: MedData, date: LocalDate) {
        if (item.type != ItemType.Illness) return
        val newHistory = HashMap(item.takenHistory)
        newHistory[date] = LocalTime.now()
        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) _items[index] = item.copy(takenHistory = newHistory)
        saveData()
    }

    fun reloadData() {
        loadData()
        syncToWear()
    }

    private fun syncToWear() {
        WearSyncManager.initialize(getApplication())
        val today = LocalDate.now()

        val itemsToday = _items.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == today
                ItemType.Illness -> {
                    val isAfterStart = !today.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                    isAfterStart && isBeforeEnd
                }

                ItemType.Medicine -> {
                    val isAfterStart = !today.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                    val isCorrectDay =
                        item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(today.dayOfWeek)
                    val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(
                        item.creationDate,
                        today
                    ) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))

        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val meds = itemsToday.filter { it.type == ItemType.Medicine }.map {
            val taken = if (it.takenHistory.containsKey(today)) "✅ " else ""
            "$taken${it.creationTime.format(formatter)} - ${it.title}"
        }

        val evs =
            itemsToday.filter { (it.type == ItemType.Event || it.type == ItemType.Illness) && it.title.isNotBlank() && it.iconName != "DIVIDER" }
                .map {
                    "${it.creationTime.format(formatter)} - ${it.title}"
                }

        WearSyncManager.syncData(meds, evs)
    }

    private fun saveData() {
        DataRepository.saveData(getApplication(), _items)
        syncToWear()
    }

    private fun loadData() {
        val list = DataRepository.loadData(getApplication())
        _items.clear()
        _items.addAll(list)
    }
}