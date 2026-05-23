/**
 * Google Apps Script Web App Backend for NUR BESHIR UMER Receipt Scanner
 * 
 * Instructions:
 * 1. Open Google Sheets (create a new sheet or use an existing one).
 * 2. Click "Extensions" -> "Apps Script".
 * 3. Delete any code in the editor and paste this code.
 * 4. Save the project (e.g., name it "Receipt Scanner Backend").
 * 5. Click "Deploy" -> "New deployment".
 * 6. Select type "Web app".
 * 7. Set "Execute as" to "Me (your-email@gmail.com)".
 * 8. Set "Who has access" to "Anyone" (Required so the Android app can post without user password prompts).
 * 9. Click "Deploy" and authorize permissions.
 * 10. Copy the Web app URL and paste it into the Android App Settings screen.
 */

function doPost(e) {
  var JSON_MIME = ContentService.MimeType.JSON;
  
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({
        success: false,
        message: "Empty request or missing post database content."
      })).setMimeType(JSON_MIME);
    }

    var jsonString = e.postData.contents;
    var data = JSON.parse(jsonString);
    var rows = data.rows;
    
    if (!rows || rows.length === 0) {
      return ContentService.createTextOutput(JSON.stringify({
        success: false,
        message: "No rows provided in payload."
      })).setMimeType(JSON_MIME);
    }
    
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    // Add headers if the Google Sheet is brand new or empty
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "Item Name",
        "Quantity",
        "Unit Price",
        "2% TOT",
        "Total Price",
        "FS No",
        "Date",
        "Raw OCR Text",
        "Validation Status",
        "Saved Timestamp"
      ]);
    }
    
    var addedCount = 0;
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i];
      
      // We skip keeping the Test Connection rows in our final sheet
      if (row.itemName === "TEST_CONNECTION") {
        continue;
      }

      sheet.appendRow([
        row.itemName,
        row.quantity,
        row.unitPrice,
        row.tot2Percent,
        row.totalPrice,
        row.fsNo,
        row.date,
        row.rawOcrText,
        row.validationStatus,
        row.savedTimestamp
      ]);
      addedCount++;
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      success: true,
      message: "Successfully synchronized " + addedCount + " rows."
    })).setMimeType(JSON_MIME);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      message: "Error processing script: " + error.toString()
    })).setMimeType(JSON_MIME);
  }
}

function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    success: true,
    message: "Google Apps Script is active! Please use POST requests for syncing receipts."
  })).setMimeType(ContentService.MimeType.JSON);
}
