package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReceiptItem
import com.example.ui.viewmodel.ReceiptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReceiptViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parseResult by viewModel.parseResult.collectAsState()
    val editableItems by viewModel.editableItems.collectAsState()
    val editableFsNo by viewModel.editableFsNo.collectAsState()
    val editableDate by viewModel.editableDate.collectAsState()
    val rawOcrText by viewModel.rawOcrText.collectAsState()
    val scannedBitmap by viewModel.scannedBitmap.collectAsState()
    val scannedBitmapProcessed by viewModel.scannedBitmapProcessed.collectAsState()
    val duplicateWarning by viewModel.duplicateWarning.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()

    var showImagePreview by remember { mutableStateOf(false) }
    var showRawOcrText by remember { mutableStateOf(false) }
    var previewModeOriginal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Scan", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    val isValid = parseResult?.validationStatus != "NEEDS_REVIEW"
                    Surface(
                        color = if (isValid) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                        contentColor = if (isValid) Color(0xFF047857) else Color(0xFFB45309),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isValid) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (isValid) "VALID" else "REVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.testTag("top_bar_review")
            )
        },
        containerColor = Color(0xFFF3F4F9), // Set scaffold background to high density slatebg
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Receipt Image and OCR Section (Collapsible)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showImagePreview = !showImagePreview },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Receipt Preview",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Show Receipt Image Preview",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showImagePreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Preview",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = showImagePreview) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                scannedBitmap?.let { bitmap ->
                                    // Row with toggle buttons for Original vs Preprocessed views
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { previewModeOriginal = false },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!previewModeOriginal) Color(0xFF6750A4) else Color(0xFFF1F5F9),
                                                contentColor = if (!previewModeOriginal) Color.White else Color(0xFF475569)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("OCR PREPROCESSED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { previewModeOriginal = true },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (previewModeOriginal) Color(0xFF6750A4) else Color(0xFFF1F5F9),
                                                contentColor = if (previewModeOriginal) Color.White else Color(0xFF475569)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("ORIGINAL SNAP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Preprocessing feature badges / explanation context
                                    if (!previewModeOriginal) {
                                        Text(
                                            text = "Auto-Deskewed • Bradley Adaptive Binarization • Denoised (Box Blur)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF6750A4),
                                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                                        )
                                    }

                                    val activeBitmap = if (!previewModeOriginal) (scannedBitmapProcessed ?: bitmap) else bitmap
                                    Image(
                                        bitmap = activeBitmap.asImageBitmap(),
                                        contentDescription = "Review camera capture receipt image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Inside
                                    )
                                } ?: Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No receipt image available", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Global Receipt Meta Fields (FS NO and Date)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("meta_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                    border = BorderStroke(1.dp, Color(0xFFD1BCFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // High Density Mockup header area inside Receipt Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = "STORE NAME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F378B).copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "NUR BESHIR UMER",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "FS: ${editableFsNo.ifBlank { "00000000" }}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = editableDate.ifBlank { "N/A" },
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF21005D).copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFD1BCFF).copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "METADATA FIELDS (EDITABLE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F378B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editableFsNo,
                                onValueChange = { viewModel.updateFsNo(it) },
                                label = { Text("FS Number", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fs_no_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFD1BCFF),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = editableDate,
                                onValueChange = { viewModel.updateDate(it) },
                                label = { Text("Date (DD/MM/YYYY)", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("date_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFD1BCFF),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // 3. Validation Discrepancy Warnings Box
            parseResult?.let { result ->
                if (result.validationStatus == "NEEDS_REVIEW" && result.validationErrors.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("validation_warning_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Calculations Needs Review",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                result.validationErrors.forEach { err ->
                                    Text(
                                        text = "• $err",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Duplicate Entry Warning Bar
            duplicateWarning?.let { warning ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("duplicate_warning_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                        border = BorderStroke(1.dp, Color(0xFFFFC107))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Duplicate Warning",
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 5. Total Validation Comparisons card
            parseResult?.let { result ->
                item {
                    val computedBase = editableItems.sumOf { it.baseAmount }
                    val computedTax = editableItems.sumOf { it.tot2Percent }
                    val computedTotal = editableItems.sumOf { it.totalPrice }
                    val isMatched = result.validationStatus != "NEEDS_REVIEW"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)), // bg-slate-100
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)), // border-slate-300
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Receipt Total Validation:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569), // text-slate-600
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isMatched) "MATCHED" else "PENDING REVIEW",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatched) Color(0xFF16A34A) else Color(0xFFDC2626) // text-green-600 / text-red-600
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grand Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "ETB ${String.format("%.2f", computedTotal)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCBD5E1).copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Fine mathematics comparison subtext
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("COMPUTED ITEMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Subtotal: ETB ${String.format("%.2f", computedBase)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))
                                    Text("2% TOT: ETB ${String.format("%.2f", computedTax)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("SCANNED DECLARED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Subtotal: ETB ${result.receiptSubtotal?.let { String.format("%.2f", it) } ?: "N/A"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))
                                    Text("Declared Total: ETB ${result.receiptTotal?.let { String.format("%.2f", it) } ?: "N/A"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }
            }

            // Header for Editable Rows List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRACTED ITEMS (${editableItems.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), // text-slate-500
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 6. List of Items (Cards)
            itemsIndexed(editableItems) { idx, item ->
                ItemReviewCard(
                    index = idx,
                    item = item,
                    onUpdate = { updated -> viewModel.updateEditingItem(idx, updated) },
                    onDelete = { viewModel.deleteEditableItemAt(idx) }
                )
            }

            // Quick add item button at bottom of list
            item {
                OutlinedButton(
                    onClick = { viewModel.addManualItemRow(editableFsNo, editableDate, rawOcrText) },
                    modifier = Modifier.fillMaxWidth().testTag("add_item_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Row")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Manual Item Row")
                }
            }

            // 7. Collapsible Raw OCR text view for fine-tuning comparisons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawOcrText = !showRawOcrText },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "OCR Log", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "View Extracted OCR Transcripts Logs",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showRawOcrText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        AnimatedVisibility(visible = showRawOcrText) {
                            Text(
                                text = rawOcrText.ifBlank { "No transcription scanned yet." },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 8. Bottom Spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // 9. High Density Bottom Actions Panel (Docked block)
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE2E8F0), // border-slate-200
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Retake Button (Pill shaped, border 79747E, text 6750A4)
                        OutlinedButton(
                            onClick = {
                                viewModel.resetScanner()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("retake_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                            border = BorderStroke(1.dp, Color(0xFF79747E)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retake Scanner", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RETAKE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Save & Sync Button (Pill shaped, bg 6750A4)
                        Button(
                            onClick = {
                                viewModel.saveReceipt {
                                    viewModel.resetScanner()
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                                .testTag("save_button"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Commit to sheets", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE TO SHEETS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sync Status Footer (Pulsing icon + dynamic counts)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Connected to Google Apps Script",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8) // slate-400
                            )
                        }
                        
                        Text(
                            text = "$unsyncedCount PENDING SYNC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemReviewCard(
    index: Int,
    item: ReceiptItem,
    onUpdate: (ReceiptItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_review_card_$index"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)) // border-slate-200
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // High Density Presentation Row matching mockup HTML exactly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemName.ifBlank { "UNNAMED ITEM" }.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B) // text-slate-800
                )
                Text(
                    text = "ETB ${String.format("%.2f", item.totalPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF0F172A) // text-slate-900
                )
            }
            
            Spacer(modifier = Modifier.height(3.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.3f", item.quantity)} x ${String.format("%.3f", item.unitPrice)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B) // text-slate-500
                )
                Text(
                    text = "+ TOT (2%): ${String.format("%.2f", item.tot2Percent)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF2563EB), // text-blue-600
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
            Spacer(modifier = Modifier.height(10.dp))
            
            // Nested Edit Controls Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT ROW PARAMS #${index + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4), // text-purple
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_item_row_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Discard Row",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Editable Inputs
            OutlinedTextField(
                value = item.itemName,
                onValueChange = { onUpdate(item.copy(itemName = it)) },
                label = { Text("Item Name", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_name_input_$index"),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                    onValueChange = {
                        val parsed = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(quantity = parsed))
                    },
                    label = { Text("Quantity", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quantity_input_$index"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString(),
                    onValueChange = {
                        val parsed = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(unitPrice = parsed))
                    },
                    label = { Text("Unit Price", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("unit_price_input_$index"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

// Inline border helpers deleted
