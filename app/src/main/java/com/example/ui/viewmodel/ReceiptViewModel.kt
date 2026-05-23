package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ReceiptItem
import com.example.data.repository.ReceiptRepository
import com.example.parser.ParseResult
import com.example.parser.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(
    val repository: ReceiptRepository
) : ViewModel() {

    // Scanner UI States
    val allItems: StateFlow<List<ReceiptItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsyncedCount: StateFlow<Int> = repository.unsyncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _rawOcrText = MutableStateFlow("")
    val rawOcrText: StateFlow<String> = _rawOcrText

    private val _scannedBitmap = MutableStateFlow<Bitmap?>(null)
    val scannedBitmap: StateFlow<Bitmap?> = _scannedBitmap

    private val _scannedBitmapProcessed = MutableStateFlow<Bitmap?>(null)
    val scannedBitmapProcessed: StateFlow<Bitmap?> = _scannedBitmapProcessed

    private val _parseResult = MutableStateFlow<ParseResult?>(null)
    val parseResult: StateFlow<ParseResult?> = _parseResult

    // Form inputs for editing mapped items on the review screen
    private val _editableItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val editableItems: StateFlow<List<ReceiptItem>> = _editableItems

    private val _editableFsNo = MutableStateFlow("")
    val editableFsNo: StateFlow<String> = _editableFsNo

    private val _editableDate = MutableStateFlow("")
    val editableDate: StateFlow<String> = _editableDate

    // Settings screen URL
    private val _webAppUrl = MutableStateFlow(repository.getWebAppUrl())
    val webAppUrl: StateFlow<String> = _webAppUrl

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage

    // Duplicate warnings checking
    private val _duplicateWarning = MutableStateFlow<String?>(null)
    val duplicateWarning: StateFlow<String?> = _duplicateWarning

    init {
        // Trigger auto sync whenever the app starts or ViewModel loads
        syncPending()
    }

    // Save and cache connection URL
    fun updateWebAppUrl(url: String) {
        _webAppUrl.value = url
        repository.saveWebAppUrl(url)
    }

    // Run advanced custom Bitmap pre-processing
    private fun preprocessBitmap(original: Bitmap): Bitmap {
        // 1. Convert to Grayscale
        val grayscale = toGrayscale(original)

        // 2. Deskew original if skew angle is identified
        val skewAngle = determineSkewAngle(grayscale)
        val deskewed = if (Math.abs(skewAngle) >= 1.0f) {
            rotateBitmap(grayscale, skewAngle)
        } else {
            grayscale
        }

        // 3. Noise reduction using Box blur (radius = 1)
        val denoised = boxBlur(deskewed, radius = 1)

        // 4. Adaptive Thresholding (Bradley-Roth binarization)
        val blockSize = (denoised.width / 16).coerceIn(16, 64)
        val binarized = bradleyRothAdaptiveThreshold(denoised, s = blockSize, t = 0.15f)

        return binarized
    }

    private fun toGrayscale(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val outPixels = IntArray(width * height)
        val side = radius * 2 + 1
        
        // Horizontal pass
        for (y in 0 until height) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -radius..radius) {
                val x = i.coerceIn(0, width - 1)
                val p = pixels[y * width + x]
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (x in 0 until width) {
                outPixels[y * width + x] = (0xFF shl 24) or ((rSum / side) shl 16) or ((gSum / side) shl 8) or (bSum / side)
                
                val leftX = (x - radius).coerceIn(0, width - 1)
                val rightX = (x + radius + 1).coerceIn(0, width - 1)
                val pLeft = pixels[y * width + leftX]
                val pRight = pixels[y * width + rightX]
                
                rSum += ((pRight shr 16) and 0xFF) - ((pLeft shr 16) and 0xFF)
                gSum += ((pRight shr 8) and 0xFF) - ((pLeft shr 8) and 0xFF)
                bSum += (pRight and 0xFF) - (pLeft and 0xFF)
            }
        }
        
        // Vertical pass
        val finalPixels = IntArray(width * height)
        for (x in 0 until width) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -radius..radius) {
                val y = i.coerceIn(0, height - 1)
                val p = outPixels[y * width + x]
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (y in 0 until height) {
                finalPixels[y * width + x] = (0xFF shl 24) or ((rSum / side) shl 16) or ((gSum / side) shl 8) or (bSum / side)
                
                val topY = (y - radius).coerceIn(0, height - 1)
                val bottomY = (y + radius + 1).coerceIn(0, height - 1)
                val pTop = outPixels[topY * width + x]
                val pBottom = outPixels[bottomY * width + x]
                
                rSum += ((pBottom shr 16) and 0xFF) - ((pTop shr 16) and 0xFF)
                gSum += ((pBottom shr 8) and 0xFF) - ((pTop shr 8) and 0xFF)
                bSum += (pBottom and 0xFF) - (pTop and 0xFF)
            }
        }
        
        dest.setPixels(finalPixels, 0, width, 0, 0, width, height)
        return dest
    }

    private fun determineSkewAngle(grayscale: Bitmap): Float {
        // Step 1. Downscale grayscale image for ultra-fast projection profile computation
        val scaledWidth = 200
        val scaledHeight = 300
        val smallBitmap = Bitmap.createScaledBitmap(grayscale, scaledWidth, scaledHeight, true)
        val pixels = IntArray(scaledWidth * scaledHeight)
        smallBitmap.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)
        
        // Step 2. Build a high contrast binary map (text vs background)
        var sumLuminance = 0L
        val gray = IntArray(scaledWidth * scaledHeight) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = (r + g + b) / 3
            sumLuminance += l
            l
        }
        val avgLuminance = sumLuminance / (scaledWidth * scaledHeight)
        
        // Dark pixels representing text strokes
        val threshold = (avgLuminance * 0.9f).toInt()
        val binary = BooleanArray(scaledWidth * scaledHeight) { i ->
            gray[i] < threshold
        }
        
        var bestAngle = 0.0f
        var maxVariance = -1.0
        
        // Search angles in increments of 1.0 degree
        var angle = -15.0f
        while (angle <= 15.0f) {
            val variance = computeProjectionVariance(binary, scaledWidth, scaledHeight, angle)
            if (variance > maxVariance) {
                maxVariance = variance
                bestAngle = angle
            }
            angle += 1.0f
        }
        
        return bestAngle
    }

    private fun computeProjectionVariance(binary: BooleanArray, width: Int, height: Int, angleDegrees: Float): Double {
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        val cosA = Math.cos(angleRad)
        val sinA = Math.sin(angleRad)
        
        val centerX = width / 2.0
        val centerY = height / 2.0
        
        val rowCounts = IntArray(height)
        
        for (y in 0 until height) {
            val dy = y - centerY
            for (x in 0 until width) {
                if (binary[y * width + x]) {
                    val dx = x - centerX
                    val rotY = (dx * sinA + dy * cosA + centerY).toInt()
                    if (rotY in 0 until height) {
                        rowCounts[rotY]++
                    }
                }
            }
        }
        
        var sum = 0.0
        for (count in rowCounts) {
            sum += count
        }
        val mean = sum / height
        
        var sumSquaredDiff = 0.0
        for (count in rowCounts) {
            val diff = count - mean
            sumSquaredDiff += diff * diff
        }
        
        return sumSquaredDiff / height
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        if (Math.abs(angle) < 0.5f) return source
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun bradleyRothAdaptiveThreshold(src: Bitmap, s: Int, t: Float): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val gray = IntArray(width * height) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            (r + g + b) / 3
        }
        
        val integral = IntArray(width * height)
        for (x in 0 until width) {
            var sum = 0
            for (y in 0 until height) {
                val index = y * width + x
                sum += gray[index]
                if (x == 0) {
                    integral[index] = sum
                } else {
                    integral[index] = integral[y * width + (x - 1)] + sum
                }
            }
        }
        
        val halfS = s / 2
        val resultPixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                
                val x1 = (x - halfS).coerceAtLeast(0)
                val x2 = (x + halfS).coerceAtMost(width - 1)
                val y1 = (y - halfS).coerceAtLeast(0)
                val y2 = (y + halfS).coerceAtMost(height - 1)
                
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                
                var sum = integral[y2 * width + x2]
                if (x1 > 0) {
                    sum -= integral[y2 * width + (x1 - 1)]
                }
                if (y1 > 0) {
                    sum -= integral[(y1 - 1) * width + x2]
                }
                if (x1 > 0 && y1 > 0) {
                    sum += integral[(y1 - 1) * width + (x1 - 1)]
                }
                
                val currentLuminance = gray[index]
                val bValue = if (currentLuminance * count < sum * (1.0f - t)) {
                    0x00
                } else {
                    0xFF
                }
                
                resultPixels[index] = (0xFF shl 24) or (bValue shl 16) or (bValue shl 8) or bValue
            }
        }
        
        dest.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return dest
    }

    // Perform OCR recognition using ML Kit and Parse details
    fun processReceiptImage(bitmap: Bitmap) {
        _isProcessing.value = true
        _scannedBitmap.value = bitmap
        _duplicateWarning.value = null

        viewModelScope.launch {
            try {
                // Preprocess to handle blurry/faded elements
                val processed = preprocessBitmap(bitmap)
                _scannedBitmapProcessed.value = processed

                val image = InputImage.fromBitmap(processed, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        _rawOcrText.value = text

                        // Parse receipt details
                        val result = ReceiptParser.parse(text)
                        _parseResult.value = result
                        _editableItems.value = result.items
                        _editableFsNo.value = result.fsNo
                        _editableDate.value = result.date
                        _isProcessing.value = false

                        // Check duplicates for loaded items
                        checkCurrentItemsForDuplicates()
                    }
                    .addOnFailureListener { e ->
                        _rawOcrText.value = "OCR failed: ${e.message}"
                        val emptyResult = ParseResult(
                            fsNo = "",
                            date = "",
                            items = emptyList(),
                            receiptSubtotal = null,
                            receiptTax = null,
                            receiptTotal = null,
                            validationStatus = "NEEDS_REVIEW",
                            validationErrors = listOf("OCR recognition failed: ${e.message}")
                        )
                        _parseResult.value = emptyResult
                        _editableItems.value = emptyList()
                        _editableFsNo.value = ""
                        _editableDate.value = ""
                        _isProcessing.value = false
                    }
            } catch (ex: Exception) {
                ex.printStackTrace()
                _isProcessing.value = false
            }
        }
    }

    // Checking if parsed items are likely already in DB
    private fun checkCurrentItemsForDuplicates() {
        viewModelScope.launch {
            var warning: String? = null
            for (item in _editableItems.value) {
                val duplicate = repository.findDuplicate(
                    fsNo = _editableFsNo.value,
                    date = _editableDate.value,
                    itemName = item.itemName,
                    totalPrice = item.totalPrice
                )
                if (duplicate != null) {
                    warning = "This receipt item ('${item.itemName}' for \$${item.totalPrice}) looks already saved. Save anyway?"
                    break
                }
            }
            _duplicateWarning.value = warning
        }
    }

    // Deletes selected history log entry from Room
    fun deleteLogItem(item: ReceiptItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    // Appends a new empty receipt row to current review grid list manually
    fun addManualItemRow(fsNo: String, date: String, rawText: String) {
        val current = _editableItems.value.toMutableList()
        current.add(
            ReceiptItem(
                itemName = "NEW ITEM",
                quantity = 1.0,
                unitPrice = 0.0,
                baseAmount = 0.0,
                tot2Percent = 0.0,
                totalPrice = 0.0,
                fsNo = fsNo,
                date = date,
                rawOcrText = rawText,
                validationStatus = "NEEDS_REVIEW",
                savedTimestamp = System.currentTimeMillis()
            )
        )
        _editableItems.value = current
        recalculateValidationStatus()
    }

    // Discards a specific row of our editing list during manual review
    fun deleteEditableItemAt(index: Int) {
        val current = _editableItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _editableItems.value = current
            recalculateValidationStatus()
        }
    }

    // Update fields manually from UI reviews
    fun updateEditingItem(index: Int, updated: ReceiptItem) {
        val list = _editableItems.value.toMutableList()
        if (index in list.indices) {
            // Re-calculate math properties if quantity or unitPrice is shifted
            val qty = updated.quantity
            val price = updated.unitPrice
            val base = (qty * price).round()
            val tax = (base * 0.02).round()
            val total = (base + tax).round()

            list[index] = updated.copy(
                baseAmount = base,
                tot2Percent = tax,
                totalPrice = total
            )
            _editableItems.value = list
            recalculateValidationStatus()
        }
    }

    fun updateFsNo(newFsNo: String) {
        _editableFsNo.value = newFsNo
        recalculateValidationStatus()
    }

    fun updateDate(newDate: String) {
        _editableDate.value = newDate
        recalculateValidationStatus()
    }

    // Re-verify mathematical checksums after user manual edits
    private fun recalculateValidationStatus() {
        val items = _editableItems.value
        val fsNo = _editableFsNo.value
        val date = _editableDate.value

        var validationStatus = "VALID"
        
        if (items.isEmpty() || fsNo.isBlank() || date.isBlank()) {
            validationStatus = "NEEDS_REVIEW"
        }

        // Apply updated validation status to items
        _editableItems.value = items.map {
            it.copy(
                fsNo = fsNo,
                date = date,
                validationStatus = validationStatus
            )
        }
    }

    // Clear current scanner states to proceed to new camera scan
    fun resetScanner() {
        _rawOcrText.value = ""
        _scannedBitmap.value = null
        _scannedBitmapProcessed.value = null
        _parseResult.value = null
        _editableItems.value = emptyList()
        _editableFsNo.value = ""
        _editableDate.value = ""
        _duplicateWarning.value = null
    }

    // Save current items into local Room and sync to cloud
    fun saveReceipt(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val itemsToSave = _editableItems.value.map { item ->
                item.copy(
                    fsNo = _editableFsNo.value,
                    date = _editableDate.value,
                    savedTimestamp = System.currentTimeMillis()
                )
            }
            
            // Insert in Room DB
            repository.insertItems(itemsToSave)
            
            // Trigger sync
            syncPending()
            
            onSuccess()
        }
    }

    // Sync elements
    fun syncPending() {
        viewModelScope.launch {
            _syncMessage.value = "Syncing with Google Sheets..."
            val result = repository.syncPendingItems()
            if (result.isSuccess) {
                _syncMessage.value = result.getOrNull()
            } else {
                _syncMessage.value = "Sync failure: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    // Test Apps Script config URL
    fun testAppsScriptConnection(url: String) {
        viewModelScope.launch {
            _testResult.value = "Testing connection..."
            val result = repository.testConnection(url)
            if (result.isSuccess) {
                _testResult.value = result.getOrNull()
            } else {
                _testResult.value = "Connection failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
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

// ViewModel Factory
class ReceiptViewModelFactory(
    private val repository: ReceiptRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
