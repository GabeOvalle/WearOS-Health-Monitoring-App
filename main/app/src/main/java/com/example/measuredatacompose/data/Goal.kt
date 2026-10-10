package com.example.measuredatacompose.data

/**
 * Represents a goal with a target value and a current value.
 * Goal is achieved if the current value >= the target value.
 *
 * @param E The type of the values, which must be comparable.
 * @property targetValue The target value for the goal.
 * @property currentValue The current value towards achieving the goal.
 */
class Goal<E : Comparable<E>>(targetValue: E?, currentValue: E?) {
    var targetValue: E? = null
    var currentValue: E? = null

    init {
        this.targetValue = targetValue
        this.currentValue = currentValue
    }

    val isAchieved: Boolean
        get() {
            if (targetValue == null || currentValue == null) {
                return false
            }
            return currentValue!! >= targetValue!!
        }
}