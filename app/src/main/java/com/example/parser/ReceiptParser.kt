package com.example.parser

import com.example.data.local.ReceiptItem
import java.util.regex.Pattern

object ReceiptParser {

    private val fsRegex = """FS\s*NO\.?\s*[:\-\s]?\s*([0-9a-zA-Z]+)""".toRegex(RegexOption.IGNORE_CASE)
    private val dateRegex = """(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""".toRegex()
    
    // Pattern like "2.000x 1568.630" or "20.000x 24.510"
    private val qtyPriceRegex = """^(\d+[\d.,]*)\s*[xX*]\s*(\d+[\d.,]*)$""".toRegex()
    
    // Pattern like "BATREY *3,137.26"
    private val itemLineRegex = """^(.+?)\s*\*\s*([\d.,]+)$""".toRegex()

    fun parse(rawText: String): ParseResult {
        val lines = rawText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var fsNo = ""
        var date = ""
        var subtotal: Double? = null
        var tax: Double? = null
        var total: Double? = null

        // 1. Extract FS No and Date
        for (line in lines) {
            if (fsNo.isEmpty()) {
                val fsMatch = fsRegex.find(line)
                if (fsMatch != null) {
                    fsNo = fsMatch.groupValues[1].trim()
                }
            }
            if (date.isEmpty()) {
                val dateMatch = dateRegex.find(line)
                if (dateMatch != null) {
                    val d = dateMatch.groupValues[1]
                    val m = dateMatch.groupValues[2]
                    var y = dateMatch.groupValues[3]
                    if (y.length == 2) {
                        y = "20$y"
                    }
                    date = "$d/$m/$y"
                }
            }

            // Extract Subtotal, Tax, Total from individual lines
            val uppercaseLine = line.uppercase()
            if (uppercaseLine.contains("SUBTOTAL") || uppercaseLine.contains("SUB TOTAL")) {
                subtotal = cleanNumber(line)
            } else if (uppercaseLine.contains("TAX")) {
                tax = cleanNumber(line)
            } else if (uppercaseLine.contains("TOTAL") && !uppercaseLine.contains("SUBTOTAL") && !uppercaseLine.contains("TAX")) {
                total = cleanNumber(line)
            }
        }

        // If FS and Date are still empty, try scanning rawText globally
        if (fsNo.isEmpty()) {
            val fsMatch = fsRegex.find(rawText)
            if (fsMatch != null) {
                fsNo = fsMatch.groupValues[1].trim()
            }
        }
        if (date.isEmpty()) {
            val dateMatch = dateRegex.find(rawText)
            if (dateMatch != null) {
                val d = dateMatch.groupValues[1]
                val m = dateMatch.groupValues[2]
                var y = dateMatch.groupValues[3]
                if (y.length == 2) {
                    y = "20$y"
                }
                date = "$d/$m/$y"
            }
        }

        val parsedItems = mutableListOf<ReceiptItem>()
        var pendingQty: Double? = null
        var pendingUnitPrice: Double? = null

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val uppercaseLine = line.uppercase()

            if (isExcludedLine(line)) {
                i++
                continue
            }

            // A. Check if it's a quantity line, matching "2.000x 1568.630"
            val qtyMatch = qtyPriceRegex.find(line)
            if (qtyMatch != null) {
                val qty = cleanNumber(qtyMatch.groupValues[1]) ?: 1.0
                val unitPrice = cleanNumber(qtyMatch.groupValues[2]) ?: 0.0
                pendingQty = qty
                pendingUnitPrice = unitPrice

                // Look ahead 1-2 lines to find the item description
                var nextIdx = i + 1
                while (nextIdx < lines.size && lines[nextIdx].isEmpty()) {
                    nextIdx++
                }

                if (nextIdx < lines.size) {
                    val nextLine = lines[nextIdx]
                    if (!isExcludedLine(nextLine)) {
                        val itemMatch = itemLineRegex.find(nextLine)
                        if (itemMatch != null) {
                            val itemName = itemMatch.groupValues[1].trim()
                            val basePrice = cleanNumber(itemMatch.groupValues[2]) ?: (qty * unitPrice)
                            
                            parsedItems.add(createItem(itemName, qty, unitPrice, basePrice, rawText, fsNo, date))
                            pendingQty = null
                            pendingUnitPrice = null
                            i = nextIdx + 1
                            continue
                        } else {
                            val parts = splitNameAndPrice(nextLine)
                            if (parts != null) {
                                parsedItems.add(createItem(parts.first, qty, unitPrice, parts.second, rawText, fsNo, date))
                                pendingQty = null
                                pendingUnitPrice = null
                                i = nextIdx + 1
                                continue
                            }
                        }
                    }
                }
            }

            // B. Direct match for single item line or item line with * separating price
            val itemMatch = itemLineRegex.find(line)
            if (itemMatch != null) {
                val itemName = itemMatch.groupValues[1].trim()
                val basePrice = cleanNumber(itemMatch.groupValues[2]) ?: 0.0

                val qty = pendingQty ?: 1.0
                val unitPrice = pendingUnitPrice ?: basePrice

                parsedItems.add(createItem(itemName, qty, unitPrice, basePrice, rawText, fsNo, date))
                pendingQty = null
                pendingUnitPrice = null
                i++
                continue
            }

            // C. Try manual splitting (last space before a number)
            val parts = splitNameAndPrice(line)
            if (parts != null) {
                val qty = pendingQty ?: 1.0
                val unitPrice = pendingUnitPrice ?: parts.second

                parsedItems.add(createItem(parts.first, qty, unitPrice, parts.second, rawText, fsNo, date))
                pendingQty = null
                pendingUnitPrice = null
                i++
                continue
            }

            i++
        }

        // Perform validations
        val validationErrors = mutableListOf<String>()
        var validationStatus = "VALID"

        if (parsedItems.isEmpty()) {
            validationStatus = "NEEDS_REVIEW"
            validationErrors.add("No items parsed from receipt.")
        }

        if (fsNo.isEmpty()) {
            validationStatus = "NEEDS_REVIEW"
            validationErrors.add("FS NO. not detected.")
        }

        if (date.isEmpty()) {
            validationStatus = "NEEDS_REVIEW"
            validationErrors.add("Date not detected.")
        }

        val sumBaseAmount = parsedItems.sumOf { it.baseAmount }.round(2)
        val sumTot2Percent = parsedItems.sumOf { it.tot2Percent }.round(2)
        val sumTotalPrice = parsedItems.sumOf { it.totalPrice }.round(2)

        // Validate items against SUBTOTAL
        if (subtotal != null) {
            val diff = Math.abs(sumBaseAmount - subtotal)
            if (diff > 0.05) {
                validationStatus = "NEEDS_REVIEW"
                validationErrors.add("Subtotal mismatch! Items sum to $sumBaseAmount, receipt shows $subtotal (diff ${diff.round(2)})")
            }
        }

        // Validate items against TAX (TOT 2%)
        if (tax != null) {
            val diff = Math.abs(sumTot2Percent - tax)
            if (diff > 0.05) {
                validationStatus = "NEEDS_REVIEW"
                validationErrors.add("Tax (2% TOT) mismatch! Calculated sum: $sumTot2Percent, receipt: $tax (diff ${diff.round(2)})")
            }
        }

        // Validate items against TOTAL
        if (total != null) {
            val diff = Math.abs(sumTotalPrice - total)
            if (diff > 0.05) {
                validationStatus = "NEEDS_REVIEW"
                validationErrors.add("Total price mismatch! Calculated: $sumTotalPrice, receipt: $total (diff ${diff.round(2)})")
            }
        }

        // Apply global validation status to each item
        val validatedItems = parsedItems.map {
            it.copy(
                fsNo = fsNo,
                date = date,
                validationStatus = validationStatus
            )
        }

        return ParseResult(
            fsNo = fsNo,
            date = date,
            items = validatedItems,
            receiptSubtotal = subtotal,
            receiptTax = tax,
            receiptTotal = total,
            validationStatus = validationStatus,
            validationErrors = validationErrors
        )
    }

    private fun createItem(
        name: String,
        qty: Double,
        unitPrice: Double,
        printedBaseAmount: Double,
        rawOcrText: String,
        fsNo: String,
        date: String
    ): ReceiptItem {
        val baseAmount = (qty * unitPrice).round(2)
        val tot2Percent = (baseAmount * 0.02).round(2)
        val totalPrice = (baseAmount + tot2Percent).round(2)

        return ReceiptItem(
            itemName = name,
            quantity = qty,
            unitPrice = unitPrice,
            baseAmount = baseAmount,
            tot2Percent = tot2Percent,
            totalPrice = totalPrice,
            fsNo = fsNo,
            date = date,
            rawOcrText = rawOcrText,
            validationStatus = "VALID",
            savedTimestamp = System.currentTimeMillis()
        )
    }

    private fun isExcludedLine(line: String): Boolean {
        val l = line.uppercase()
        return l.contains("SUBTOTAL") || l.contains("SUB TOTAL") ||
                l.contains("TAX") || l.contains("VAT") ||
                l.contains("TOTAL") || l.contains("CASH") ||
                l.contains("CHANGE") || l.contains("ITEM#") ||
                l.contains("FS NO") || l.contains("FSNO") ||
                l.contains("NUR BESHIR") || l.contains("UMER") ||
                l.contains("TIN") || l.contains("TELE") || l.contains("TEL") ||
                l.contains("DATE") || l.contains("TIME") ||
                l.contains("CLERK") || l.contains("CASHIER") ||
                l.contains("WELCOME") || l.contains("THANK") ||
                l.contains("MERCHANT") || l.contains("DUPLICATE")
    }

    private fun splitNameAndPrice(line: String): Pair<String, Double>? {
        val lastSpaceIdx = line.lastIndexOf(' ')
        if (lastSpaceIdx <= 0) return null

        val namePart = line.substring(0, lastSpaceIdx).trim()
        val pricePart = line.substring(lastSpaceIdx + 1).trim()

        if (namePart.isNotEmpty() && namePart.any { it.isLetter() }) {
            val price = cleanNumber(pricePart)
            if (price != null && price > 0.0) {
                val cleanName = namePart.trim('*', ' ', ':', '-')
                if (cleanName.length >= 2) {
                    return Pair(cleanName, price)
                }
            }
        }
        return null
    }

    fun cleanNumber(input: String): Double? {
        // Keeps digits, dots, commas, negative signs
        val sanitized = input.replace("[^\\d.,-]".toRegex(), "")
        if (sanitized.isEmpty()) return null

        // If both a comma and a dot exist, e.g. "3,137.26"
        if (sanitized.contains(",") && sanitized.contains(".")) {
            return sanitized.replace(",", "").toDoubleOrNull()
        }

        // If only comma exists, check if it behaves as decimal or thousands separator
        if (sanitized.contains(",") && !sanitized.contains(".")) {
            val parts = sanitized.split(",")
            if (parts.size == 2 && parts[1].length in 1..2) {
                return sanitized.replace(",", ".").toDoubleOrNull()
            } else {
                return sanitized.replace(",", "").toDoubleOrNull()
            }
        }

        return sanitized.toDoubleOrNull()
    }

    private fun Double.round(decimals: Int = 2): Double {
        return try {
            java.math.BigDecimal(this).setScale(decimals, java.math.RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            val factor = Math.pow(10.0, decimals.toDouble())
            Math.round(this * factor) / factor
        }
    }
}

data class ParseResult(
    val fsNo: String,
    val date: String,
    val items: List<ReceiptItem>,
    val receiptSubtotal: Double?,
    val receiptTax: Double?,
    val receiptTotal: Double?,
    val validationStatus: String,
    val validationErrors: List<String>
)
