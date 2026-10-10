package com.example.measuredatacompose.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text

/**
 * A numpad for entering numbers, with delete & enter.
 *
 * @param onNumberClick Callback when a number is clicked, passing the number (0-9).
 * @param onDelete Callback when the delete button is clicked.
 * @param onEnter Callback when the enter button is clicked.
 */
@Composable
fun Numpad(
    onNumberClick: (Int) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    val numbers = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2) // -1 = delete, -2 = enter
    )

    Column {
        numbers.forEach { row ->
            Row {
                row.forEach { num ->
                    Button(
                        onClick = {
                            when (num) {
                                -1 -> onDelete()
                                -2 -> onEnter()
                                else -> onNumberClick(num)
                            }
                        },
                        modifier = Modifier
                            .size(35.dp, 35.dp),
                    ) {
                        Text(
                            text = when (num) {
                                -1 -> "⌫"
                                -2 -> "↵"
                                else -> num.toString()
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

