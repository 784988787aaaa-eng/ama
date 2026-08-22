package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Safe equation evaluator using BigDecimal with full operator precedence.
 * Fully supports Western (0-9), Eastern Arabic (٠-٩), and Persian (۰-۹) digits,
 * standard arithmetic operators (+ - * / × ÷), and decimal separators.
 */
fun evaluateSimpleExpression(expr: String): BigDecimal? {
    val trimmed = expr.trim()
    if (trimmed.isEmpty()) return null

    return runCatching {
        val numbers = ArrayList<BigDecimal>()
        val operators = ArrayList<Char>()
        val sb = StringBuilder(16)

        var seenDotInCurrentToken = false

        for (i in 0 until trimmed.length) {
            val c = trimmed[i]
            when {
                c.isWhitespace() -> continue
                c in '0'..'9' -> sb.append(c)
                c in '٠'..'٩' -> sb.append((c - '٠' + '0'.code).toChar())
                c in '۰'..'۹' -> sb.append((c - '۰' + '0'.code).toChar())
                c == '.' || c == ',' || c == '٫' -> {
                    if (!seenDotInCurrentToken) {
                        sb.append('.')
                        seenDotInCurrentToken = true
                    }
                }
                c in "+-*/×÷" -> {
                    if (sb.isEmpty()) {
                        // Allow unary minus at the very start of a token if no previous number
                        if (c == '-' && numbers.isEmpty()) {
                            sb.append('-')
                            continue
                        }
                        return null
                    }
                    val numStr = sb.toString()
                    if (numStr == "-" || numStr == "." || numStr == "-.") return null
                    numbers.add(BigDecimal(numStr))
                    sb.setLength(0)
                    seenDotInCurrentToken = false

                    operators.add(when (c) {
                        '×' -> '*'
                        '÷' -> '/'
                        else -> c
                    })
                }
                else -> return null // Unknown token
            }
        }
        if (sb.isNotEmpty()) {
            val numStr = sb.toString()
            if (numStr == "-" || numStr == "." || numStr == "-.") return null
            numbers.add(BigDecimal(numStr))
        }

        if (numbers.isEmpty() || numbers.size != operators.size + 1) return null

        // Pass 1: Handle multiplication and division
        val numbers2 = ArrayList<BigDecimal>(numbers.size)
        val operators2 = ArrayList<Char>(operators.size)
        numbers2.add(numbers[0])

        for (i in 0 until operators.size) {
            val op = operators[i]
            val nextNum = numbers[i + 1]
            if (op == '*' || op == '/') {
                val prevNum = numbers2.removeAt(numbers2.size - 1)
                val res = if (op == '*') {
                    prevNum.multiply(nextNum)
                } else {
                    if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null
                    prevNum.divide(nextNum, 10, RoundingMode.HALF_UP)
                }
                numbers2.add(res)
            } else {
                operators2.add(op)
                numbers2.add(nextNum)
            }
        }

        // Pass 2: Handle addition and subtraction
        var result = numbers2[0]
        for (i in 0 until operators2.size) {
            val op = operators2[i]
            val nextNum = numbers2[i + 1]
            result = if (op == '+') result.add(nextNum) else result.subtract(nextNum)
        }
        result
    }.getOrNull()
}