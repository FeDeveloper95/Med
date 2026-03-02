package com.fedeveloper95.med.services

import android.content.Context
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.MedItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
    val category: String? = null
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
                category = if (json.isNull("category")) null else json.getString("category")
            )
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
            val ois = ObjectInputStream(fis)

            val oldList = ois.readObject() as? ArrayList<MedItem> ?: return emptyList()
            ois.close()

            val migratedList = oldList.map { old ->
                MedData(
                    id = old.id,
                    groupId = old.groupId,
                    type = old.type,
                    title = old.title,
                    iconName = old.iconName,
                    colorCode = old.colorCode,
                    frequencyLabel = old.frequencyLabel,
                    creationDate = old.creationDate,
                    creationTime = old.creationTime,
                    takenHistory = old.takenHistory,
                    recurrenceDays = old.recurrenceDays,
                    endDate = old.endDate,
                    notes = null,
                    displayOrder = 0,
                    intervalGap = null,
                    category = null
                )
            }

            oldFile.renameTo(File(context.filesDir, "med_data_migrated.dat"))

            return migratedList
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }
}