package com.fedeveloper95.med

import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class MedItem(
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
    val endDate: LocalDate? = null
) : Serializable