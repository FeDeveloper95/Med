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
    val notificationType: Int = 0
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
                notificationType = json.optInt("notificationType", 0)
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

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fedeveloper95.med.RELOAD_DATA") {
                reloadData()
            }
        }
    }

    init {
        loadData()
        syncToWear()

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
        notificationType: Int = 0
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
        rangeEnd: Long? = -2L
    ) {
        val context = getApplication<Application>()
        val freqLabel = if (intervalGap == 14) context.getString(R.string.frequency_unit_biweek)
        else if (days != null) context.getString(R.string.frequency_specific_days)
        else if (times.size > 1) context.getString(
            R.string.frequency_daily_multiple,
            times.size
        )
        else context.getString(R.string.frequency_daily)

        val isMedicine = originalItem.type == ItemType.Medicine
        val isRangeUpdate = rangeStart != -2L

        val relatedItems = if (originalItem.groupId != null) {
            _items.filter { it.groupId == originalItem.groupId }
        } else {
            listOf(originalItem)
        }

        val relatedIds = relatedItems.map { it.id }.toSet()

        if (isMedicine && isRangeUpdate) {
            var editStart = selectedDate
            var editEnd: LocalDate? = null

            if (rangeStart == null && rangeEnd == null) {
                editStart = selectedDate
                editEnd = selectedDate
            } else if (rangeStart == -1L) {
                editStart = selectedDate
                editEnd = originalItem.endDate
            } else if (rangeStart != null) {
                editStart = LocalDate.ofEpochDay(rangeStart / 86400000)
                editEnd = if (rangeEnd != null && rangeEnd != -2L) LocalDate.ofEpochDay(rangeEnd / 86400000) else editStart
            }

            _items.removeAll { it.id in relatedIds }

            val baseNewItem = originalItem.copy(
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                recurrenceDays = days,
                notes = notes,
                intervalGap = intervalGap,
                notificationType = notificationType,
                frequencyLabel = freqLabel
            )

            relatedItems.forEachIndexed { i, oldItem ->
                if (editStart.isAfter(oldItem.creationDate)) {
                    val newEndDate = editStart.minusDays(1)
                    val finalEndDate = if (oldItem.endDate != null && oldItem.endDate.isBefore(newEndDate)) oldItem.endDate else newEndDate
                    _items.add(oldItem.copy(
                        id = System.nanoTime() + i,
                        endDate = finalEndDate
                    ))
                }
            }

            val editedGroupId = System.currentTimeMillis()
            times.forEachIndexed { i, time ->
                val editedPart = baseNewItem.copy(
                    id = System.nanoTime() + 100 + i,
                    groupId = editedGroupId,
                    creationTime = time,
                    creationDate = editStart,
                    endDate = editEnd,
                    takenHistory = HashMap()
                )
                _items.add(editedPart)
                NotificationReceiver.scheduleNotification(getApplication(), editedPart)
            }

            if (editEnd != null) {
                val newCreationDate = editEnd.plusDays(1)
                relatedItems.forEachIndexed { i, oldItem ->
                    if (oldItem.endDate == null || oldItem.endDate.isAfter(editEnd)) {
                        val finalCreationDate = if (oldItem.creationDate.isAfter(newCreationDate)) oldItem.creationDate else newCreationDate
                        _items.add(oldItem.copy(
                            id = System.nanoTime() + 200 + i,
                            creationDate = finalCreationDate
                        ))
                    }
                }
            }

            saveData()
            return
        }

        _items.removeAll { it.id in relatedIds }

        val newGroupId = System.currentTimeMillis()
        times.forEachIndexed { i, time ->
            val newItem = originalItem.copy(
                id = System.nanoTime() + i,
                groupId = newGroupId,
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                creationTime = time,
                creationDate = originalItem.creationDate,
                recurrenceDays = days,
                notes = notes,
                intervalGap = intervalGap,
                notificationType = notificationType,
                frequencyLabel = freqLabel
            )
            _items.add(newItem)
            if (newItem.type == ItemType.Medicine) {
                NotificationReceiver.scheduleNotification(getApplication(), newItem)
            }
        }
        saveData()
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

        val newHistory = HashMap(item.takenHistory)
        if (newHistory.containsKey(date)) newHistory.remove(date) else newHistory[date] =
            LocalTime.now()

        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) _items[index] = item.copy(takenHistory = newHistory)
        saveData()
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

        val evs = itemsToday.filter { (it.type == ItemType.Event || it.type == ItemType.Illness) && it.title.isNotBlank() && it.iconName != "DIVIDER" }.map {
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