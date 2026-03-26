package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                    CalculatorScreen()
                }
            }
        }
    }
}

@Composable
private fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }

    val buttons = listOf(
        listOf("AC", "DEL", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=", "")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (expression.isEmpty()) "0" else expression,
                color = Color(0xFFBDBDBD),
                fontSize = 28.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = result,
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            buttons.forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowButtons.forEach { label ->
                        if (label.isEmpty()) {
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            CalcButton(
                                label = label,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val (newExp, newResult) = onButtonClick(expression, result, label)
                                    expression = newExp
                                    result = newResult
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOperator = label in setOf("÷", "×", "-", "+", "=", "%")
    val bgColor = if (isOperator) Color(0xFFFF9800) else Color(0xFF2B2B2B)

    Button(
        onClick = onClick,
        modifier = modifier.size(height = 72.dp, width = 72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor)
    ) {
        Text(text = label, color = Color.White, fontSize = 24.sp)
    }
}

private fun onButtonClick(expression: String, currentResult: String, button: String): Pair<String, String> {
    return when (button) {
        "AC" -> "" to "0"
        "DEL" -> {
            val updated = if (expression.isNotEmpty()) expression.dropLast(1) else ""
            val preview = evaluateExpressionOrNull(updated)?.let(::formatNumber) ?: "0"
            updated to preview
        }
        "=" -> {
            val value = evaluateExpressionOrNull(expression)
            if (value != null) {
                val formatted = formatNumber(value)
                formatted to formatted
            } else {
                expression to currentResult
            }
        }
        "%" -> {
            val value = evaluateExpressionOrNull(expression)
            if (value != null) {
                val divided = value / 100.0
                val formatted = formatNumber(divided)
                formatted to formatted
            } else {
                expression to currentResult
            }
        }
        "." -> {
            if (canAppendDecimal(expression)) {
                val updated = expression + "."
                updated to (evaluateExpressionOrNull(updated)?.let(::formatNumber) ?: currentResult)
            } else {
                expression to currentResult
            }
        }
        in setOf("+", "-", "×", "÷") -> {
            val normalized = expression
                .ifEmpty { if (button == "-") "-" else "" }
                .replace(Regex("[+\-×÷]$"), button)

            if (normalized == expression && expression.isEmpty()) {
                expression to currentResult
            } else {
                normalized to currentResult
            }
        }
        else -> {
            val updated = expression + button
            val preview = evaluateExpressionOrNull(updated)?.let(::formatNumber) ?: currentResult
            updated to preview
        }
    }
}

private fun canAppendDecimal(expression: String): Boolean {
    val lastNumber = expression.split(Regex("[+\-×÷]"))
        .lastOrNull()
        ?: ""
    return !lastNumber.contains(".")
}

private fun evaluateExpressionOrNull(expression: String): Double? {
    if (expression.isBlank()) return null

    val normalized = expression.replace("×", "*").replace("÷", "/")
    val tokens = Regex("-?\\d+(\\.\\d+)?|[+*/-]")
        .findAll(normalized)
        .map { it.value }
        .toList()

    if (tokens.isEmpty()) return null

    val rebuilt = tokens.joinToString(separator = "")
    if (rebuilt != normalized) return null

    val values = mutableListOf<Double>()
    val operators = mutableListOf<String>()

    var index = 0
    while (index < tokens.size) {
        val token = tokens[index]
        if (token.matches(Regex("-?\\d+(\\.\\d+)?"))) {
            values.add(token.toDouble())
        } else {
            operators.add(token)
        }
        index++
    }

    var i = 0
    while (i < operators.size) {
        val op = operators[i]
        if (op == "*" || op == "/") {
            if (i + 1 >= values.size || i >= values.size) return null
            val left = values[i]
            val right = values[i + 1]
            val res = if (op == "*") left * right else {
                if (right == 0.0) return null
                left / right
            }
            values[i] = res
            values.removeAt(i + 1)
            operators.removeAt(i)
        } else {
            i++
        }
    }

    var result = values.firstOrNull() ?: return null
    for (j in operators.indices) {
        val right = values.getOrNull(j + 1) ?: return null
        result = when (operators[j]) {
            "+" -> result + right
            "-" -> result - right
            else -> return null
        }
    }
    return result
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }
}
