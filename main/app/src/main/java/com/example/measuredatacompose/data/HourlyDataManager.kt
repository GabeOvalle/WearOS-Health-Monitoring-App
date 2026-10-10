package com.example.measuredatacompose.data

import java.time.ZoneId
import java.util.TreeMap

//Implemented a TreeMap to store the hourly bpmData in a sorted manner
class HourlyDataManager {
    @JvmRecord
    data class HourlyAggregate(val avgBPM: Double, val entries: Int)

    var hourlyData: MutableMap<Int?, HourlyAggregate?>

    constructor() {
        this.hourlyData = TreeMap<Int?, HourlyAggregate?>()
    }
    constructor(hourlyDataList: MutableList<Bpm>) {
        this.hourlyData = TreeMap<Int?, HourlyAggregate?>()
        addAll(hourlyDataList)
    }

    /**
     * Adds a list of Bpm bpmData to the hourly bpmData manager.
     * @param bpmDataList The list of Bpm objects to be added to the hourly bpmData manager.
     */
    fun addAll(bpmDataList: MutableList<Bpm>) {
        for (bpm in bpmDataList) {
            add(bpm)
        }
    }

    fun add(bpm: Bpm) {
        val bpmValue = bpm.value
        if (bpmValue != null && bpmValue > 0) {
            val hour = getBpmHour(bpm)
            if(hour !in 0..<24) {
                throw IllegalArgumentException("Invalid hour extracted from Bpm timestamp: $hour")
            }
            val currentAggregate = hourlyData[hour]

            if (currentAggregate == null) {
                hourlyData[hour] = HourlyAggregate(bpmValue, 1)
            } else {
                val newAvg =
                    (currentAggregate.avgBPM * currentAggregate.entries + bpmValue) / (currentAggregate.entries + 1)
                hourlyData[hour] = HourlyAggregate(newAvg, currentAggregate.entries + 1)
            }
        }
    }

    /**
     * Returns a string representation of the hourly bpmData, showing the average BPM.
     * @return A string representation of the hourly bpmData.
     */
    override
    fun toString(): String {
        val stringBuilder = StringBuilder()
        for ((hour, aggregate) in hourlyData) {
            if (aggregate != null && hour != null) {
                val period = if (hour < 12) "AM" else "PM"
                val formattedHour = if (hour % 12 == 0) 12 else hour % 12
                stringBuilder.append("$formattedHour:00$period - ${aggregate.avgBPM.toInt()} BPM\n")
            }
        }
        if(stringBuilder.isEmpty()) {
            return "No bpmData available"
        }
        return stringBuilder.toString()
    }

    /**
     * Returns the number of hours for which bpmData is available.
     * @return The number of hours with bpmData.
     */
    fun size(): Int {
        return hourlyData.size
    }

    /**
     * Checks if the hourly bpmData manager has no bpmData.
     * @return True if there is no bpmData, false otherwise.
     */
    fun isEmpty(): Boolean {
        return hourlyData.isEmpty()
    }

    /**
     * Checks if there is bpmData for a specific hour.
     * @param hour The hour (0-23) to check for bpmData.
     * @return True if there is bpmData for the specified hour, false otherwise.
     */
    fun includes(hour: Int): Boolean {
        return hourlyData.containsKey(hour)
    }

    /**
     * Retrieves the average BPM for a specific hour.
     * @param hour The hour (0-23) for which to retrieve the average BPM.
     * @return The average BPM for the specified hour.
     * @throws IllegalArgumentException if there is no bpmData for the specified hour.
     */
    fun get(hour: Int): Double {
        val bpm = hourlyData[hour]?.avgBPM
        if (bpm != null) {
            return bpm
        } else {
            throw IllegalArgumentException("No bpmData for hour: $hour")
        }
    }

    /**
     * Extracts the hour from the timestamp of a Bpm object.
     * @param bpm The Bpm object from which to extract the hour.
     * @return The hour (0-23) extracted from the Bpm's timestamp.
     */
    private fun getBpmHour(bpm: Bpm): Int {
        return bpm.timestamp
            .atZone(ZoneId.systemDefault())
            .hour
    }
}

