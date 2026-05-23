package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ReceiptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ReceiptViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.webAppUrl.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var tempUrl by remember { mutableStateOf(currentUrl) }

    LaunchedEffect(currentUrl) {
        tempUrl = currentUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Connection", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.testTag("top_bar_settings")
            )
        },
        containerColor = Color(0xFFF3F4F9),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Web App URL Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("url_config_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "URL Link",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apps Script Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Enter the Web App URL generated from deploying your Google Apps Script backend.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Google Apps Script URL", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apps_script_url_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Test Connection Button (Pill shaped, outlines)
                        OutlinedButton(
                            onClick = {
                                viewModel.testAppsScriptConnection(tempUrl)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("test_conn_button"),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFF79747E)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                            enabled = tempUrl.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run Test", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEST LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Save URL Button (Pill shaped, primary bg)
                        Button(
                            onClick = {
                                viewModel.updateWebAppUrl(tempUrl)
                                viewModel.clearTestResult()
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .testTag("save_url_button"),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Test Connections Result logs section
            testResult?.let { result ->
                val isSuccess = result.contains("successful", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("test_result_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSuccess) Color(0xFF047857) else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = "Status icon",
                                tint = if (isSuccess) Color(0xFF047857) else Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSuccess) "Connection Success" else "Connection Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) Color(0xFF047857) else Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSuccess) Color(0xFF047857) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Copy-Paste Instructions Card (Educational instructions block)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Instructions",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apps Script Setup Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Follow these simple steps to deploy your Google Sheets receiver:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val stepsList = listOf(
                        "1. Open a Google Sheet and click 'Extensions' -> 'Apps Script'.",
                        "2. Create a script and copy the backend code (available in the root project directory of the workspace as 'google_apps_script.js').",
                        "3. In the upper right hand corner, click 'Deploy' -> 'New deployment'.",
                        "4. Click the gear icon, select 'Web App'.",
                        "5. In settings, choose Execute as: 'Me' and Who has access: 'Anyone'.",
                        "6. Click Deploy, authorize google permissions and paste the generated link here."
                    )
                    stepsList.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
