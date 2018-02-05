package com.wijayaprinting.ui.wage

import com.wijayaprinting.collections.RevertibleObservableList
import com.wijayaprinting.collections.isEmpty
import com.wijayaprinting.db.dao.Recess
import com.wijayaprinting.db.dao.Wage
import com.wijayaprinting.db.schema.Wages
import com.wijayaprinting.db.transaction
import javafx.collections.ObservableList
import kotfx.collections.mutableObservableListOf
import kotfx.properties.IntProperty
import kotfx.properties.SimpleIntProperty
import kotlinx.nosql.equal
import kotlinx.nosql.update
import org.joda.time.DateTime
import org.joda.time.Minutes.minutes
import org.joda.time.Period

/** Data class representing an Attendee with id as its identifier to avoid duplicates in [Set] scenario. */
open class Attendee @JvmOverloads constructor(
        /** Id and name are final values that should be determined upon xlsx reading. */
        val id: Int,
        val name: String,
        val role: String? = null,

        val recesses: ObservableList<Recess> = mutableObservableListOf(),

        /** Attendances and shift should be set in [com.wijayaprinting.manager.controller.AttendanceController]. */
        val attendances: RevertibleObservableList<DateTime> = RevertibleObservableList(),

        /** Wages below are retrieved from sql, or dailyEmpty if there is none. */
        val dailyProperty: IntProperty = SimpleIntProperty(),
        val hourlyOvertimeProperty: IntProperty = SimpleIntProperty()
) {

    /** Dummy for invisible [javafx.scene.control.TreeTableView] root. */
    companion object : Attendee(0, "")

    init {
        transaction {
            Wages.find { wageId.equal(id) }.singleOrNull()?.let { wage ->
                daily = wage.daily
                hourlyOvertime = wage.hourlyOvertime
            }
        }
    }

    fun saveWage() {
        transaction @Suppress("IMPLICIT_CAST_TO_ANY") {
            Wages.find { wageId.equal(id) }.let { wages ->
                if (wages.isEmpty) Wages.insert(Wage(id, daily, hourlyOvertime))
                else wages.single().let { wage ->
                    when {
                        wage.daily != daily && wage.hourlyOvertime != hourlyOvertime -> wages.projection { daily + hourlyOvertime }.update(daily, hourlyOvertime)
                        wage.daily != daily -> wages.projection { daily }.update(daily)
                        else -> wages.projection { hourlyOvertime }.update(hourlyOvertime)
                    }
                }
            }
        }
    }

    fun mergeDuplicates() = attendances.removeAllRevertable((0 until attendances.lastIndex)
            .filter { index -> Period(attendances[index], attendances[index + 1]).toStandardMinutes() < minutes(5) }
            .map { index -> attendances[index] })

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other != null && other is Attendee && other.id == id

    override fun toString(): String = "$id. $name"

    var daily: Int
        get() = dailyProperty.get()
        set(value) = dailyProperty.set(value)

    var hourlyOvertime: Int
        get() = hourlyOvertimeProperty.get()
        set(value) = hourlyOvertimeProperty.set(value)
}