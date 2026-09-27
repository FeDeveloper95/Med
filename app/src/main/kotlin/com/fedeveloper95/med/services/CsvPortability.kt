package com.fedeveloper95.med.services

import com.fedeveloper95.med.ItemType
import java.time.LocalDate
import java.time.LocalTime

object CsvPortability {

    val HEADERS = listOf(
        "id", "group_id", "type", "title", "icon_name", "color_code",
        "frequency_label", "creation_date", "creation_time",
        "taken_dates", "taken_times", "recurrence_days", "end_date",
        "interval_days", "notes", "display_order", "category", "notification_type",
        "medicine_type", "dose", "unit", "total_quantity"
    )

    private const val LIST_SEPARATOR = "|"

    fun toCsv(items: List<MedData>): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(",")).append("\r\n")
        items.forEach { m ->
            val dates = m.takenHistory.keys.sorted()
            val cells = listOf(
                m.id.toString(),
                m.groupId?.toString() ?: "",
                m.type.name,
                m.title,
                m.iconName ?: "",
                m.colorCode ?: "",
                m.frequencyLabel ?: "",
                m.creationDate.toString(),
                m.creationTime.toString(),
                dates.joinToString(LIST_SEPARATOR),
                dates.joinToString(LIST_SEPARATOR) {
                    (m.takenHistory[it] ?: LocalTime.MIDNIGHT).toString()
                },
                m.recurrenceDays?.joinToString(LIST_SEPARATOR) { it.name } ?: "",
                m.endDate?.toString() ?: "",
                m.intervalGap?.toString() ?: "",
                m.notes ?: "",
                m.displayOrder.toString(),
                m.category ?: "",
                m.notificationType.toString(),
                m.medicineType.toString(),
                m.dose.toString(),
                m.unit,
                m.totalQuantity.toString()
            )
            sb.append(cells.joinToString(",") { encodeCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun parseCsv(text: String): List<MedData> {
        val rows = parseRows(text)
        if (rows.isEmpty()) return emptyList()

        val header = rows[0].map { it.trim().lowercase() }
        fun col(name: String) = header.indexOf(name)
        val required = listOf("title", "creation_date", "creation_time")
        val missing = required.filter { col(it) == -1 }
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("CSV is missing required columns: ${missing.joinToString(", ")}")
        }

        val items = mutableListOf<MedData>()
        rows.drop(1).forEachIndexed { ri, cells ->
            fun cell(name: String): String {
                val i = col(name)
                return if (i == -1 || i >= cells.size) "" else cells[i].trim()
            }
            try {
                val rawType = cell("type")
                val type = if (rawType.isEmpty()) ItemType.Medicine
                else ItemType.entries.firstOrNull { it.name.equals(rawType, ignoreCase = true) }
                    ?: throw IllegalArgumentException("unknown type '$rawType'")

                val dates = cell("taken_dates").split(LIST_SEPARATOR).map { it.trim() }
                    .filter { it.isNotEmpty() }
                val times = cell("taken_times").split(LIST_SEPARATOR).map { it.trim() }
                val history = HashMap<LocalDate, LocalTime>()
                dates.forEachIndexed { di, d ->
                    history[LocalDate.parse(d)] = times.getOrNull(di)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { LocalTime.parse(it) }
                        ?: LocalTime.MIDNIGHT
                }

                val recDays = cell("recurrence_days").split(LIST_SEPARATOR)
                    .map { it.trim() }.filter { it.isNotEmpty() }
                    .map { parseDayOfWeek(it) }

                items.add(
                    MedData(
                        id = cell("id").toLongOrNull() ?: System.nanoTime(),
                        groupId = cell("group_id").toLongOrNull(),
                        type = type,
                        title = cell("title"),
                        iconName = cell("icon_name").ifEmpty { null },
                        colorCode = cell("color_code").ifEmpty { null },
                        frequencyLabel = cell("frequency_label").ifEmpty { null },
                        creationDate = LocalDate.parse(cell("creation_date")),
                        creationTime = LocalTime.parse(cell("creation_time")),
                        takenHistory = history,
                        recurrenceDays = recDays.ifEmpty { null },
                        endDate = cell("end_date").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                        notes = cell("notes").ifEmpty { null },
                        displayOrder = cell("display_order").toIntOrNull() ?: 0,
                        intervalGap = cell("interval_days").toIntOrNull(),
                        category = cell("category").ifEmpty { null },
                        notificationType = cell("notification_type").toIntOrNull() ?: 0,
                        medicineType = cell("medicine_type").toIntOrNull() ?: 0,
                        dose = cell("dose").toDoubleOrNull() ?: 0.0,
                        unit = cell("unit"),
                        totalQuantity = cell("total_quantity").toDoubleOrNull() ?: 0.0
                    )
                )
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("CSV row ${ri + 2}: ${e.message}")
            } catch (e: Exception) {
                throw IllegalArgumentException("CSV row ${ri + 2}: ${e.message ?: "invalid value"}")
            }
        }
        return items
    }

    private fun parseDayOfWeek(raw: String): java.time.DayOfWeek =
        java.time.DayOfWeek.valueOf(raw.uppercase())

    private fun encodeCell(raw: String?): String {
        val v = raw ?: ""
        return if (v.contains(',') || v.contains('"') || v.contains('\n') || v.contains('\r')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else v
    }

    private fun parseRows(text: String): List<List<String>> {
        val s = if (text.isNotEmpty() && text[0] == '\uFEFF') text.substring(1) else text
        val rows = mutableListOf<List<String>>()
        var cell = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < s.length && s[i + 1] == '"' -> {
                        cell.append('"'); i++
                    }

                    c == '"' -> inQuotes = false
                    else -> cell.append(c)
                }

                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(cell.toString())
                    cell = StringBuilder()
                }

                c == '\r' -> {
                    if (i + 1 < s.length && s[i + 1] == '\n') i++
                    row.add(cell.toString())
                    cell = StringBuilder()
                    rows.add(row)
                    row = mutableListOf()
                }

                c == '\n' -> {
                    row.add(cell.toString())
                    cell = StringBuilder()
                    rows.add(row)
                    row = mutableListOf()
                }

                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows
    }
}