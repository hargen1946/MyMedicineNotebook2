package com.example.mymedicinenotebook2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer

private val ButtonPurple = Color(0xFF6750A4)
private val NextQrButtonOrange = Color(0xFFE07A2D)
private val FinishReadingGreen = Color(0xFF3F8F5B)
private val ButtonShape = RoundedCornerShape(32.dp)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7F7F7)
                ) {
                    MedicineNotebookApp()
                }
            }
        }
    }
}

@Composable
fun MedicineNotebookApp() {

    val context = LocalContext.current

    val scanSession = remember {
        QrScanSession()
    }

    var qrCount by remember {
        mutableIntStateOf(0)
    }

    var prescriptionRecord by remember {
        mutableStateOf<PrescriptionRecord?>(null)
    }

    var debugText by remember {
        mutableStateOf("")
    }

    var scanNotice by remember {
        mutableStateOf("")
    }

    var saveStatusText by remember {
        mutableStateOf("")
    }

    var savedRecords by remember {
        mutableStateOf<List<PrescriptionRecord>>(emptyList())
    }

    var selectedHistoryRecord by remember {
        mutableStateOf<PrescriptionRecord?>(null)
    }

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }
    val csvExportLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "text/csv"
                )
        ) { uri ->

            if (uri != null) {

                val records =
                    PrescriptionStorage.loadRecords(
                        context
                    )

                val csvText =
                    CsvExporter.createCsv(
                        records
                    )

                context.contentResolver
                    .openOutputStream(uri)
                    ?.bufferedWriter(
                        Charsets.UTF_8
                    )
                    ?.use { writer ->

                        // Excelでも日本語が文字化けしにくいようにします
                        writer.write("\uFEFF")
                        writer.write(csvText)
                    }

                Toast.makeText(
                    context,
                    "CSVを保存しました",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    val continuousScannerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {
                val qrDataList =
                    result.data
                        ?.getStringArrayListExtra(
                            ContinuousQrScannerActivity.EXTRA_QR_DATA_LIST
                        )
                        .orEmpty()

                scanSession.clear()

                qrDataList.forEach { qrData ->
                    scanSession.addQrData(qrData)
                }

                qrCount =
                    scanSession.getQrCount()

                val allQrData =
                    scanSession.getAllQrData()

                val record =
                    JahisParser.parsePrescription(
                        allQrData
                    )

                if (record.medicines.isEmpty()) {
                    Toast.makeText(
                        context,
                        "薬の情報を見つけられませんでした",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val saved =
                        PrescriptionStorage.saveRecord(
                            context = context,
                            record = record
                        )
                    QrFingerprintStore.rememberAll(
                        context = context,
                        qrDataList = qrDataList
                    )

                    saveStatusText =
                        if (saved) {
                            "記録に保存しました"
                        } else {
                            "この記録は保存済みです"
                        }

                    prescriptionRecord =
                        record

                    currentScreen =
                        AppScreen.RESULT
                }

            } else {
                scanNotice =
                    ""
            }
        }

    when (currentScreen) {

        AppScreen.HOME -> {
            HomeScreen(
                qrCodeCount = qrCount,
                scanNotice = scanNotice,

                onShowHistory = {
                    savedRecords =
                        PrescriptionStorage.loadRecords(
                            context
                        )

                    currentScreen = AppScreen.HISTORY
                },
                onExportCsv = {
                    csvExportLauncher.launch(
                        "お薬手帳.csv"
                    )
                },


                onScanQrCode = {
                    scanNotice = ""

                    val intent =
                        Intent(
                            context,
                            ContinuousQrScannerActivity::class.java
                        )

                    continuousScannerLauncher.launch(
                        intent
                    )
                },

                onFinishReading = {
                    if (qrCount == 0) {
                        Toast.makeText(
                            context,
                            "先にQRコードを読み取ってください",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {
                        val allQrData =
                            scanSession.getAllQrData()

                        val record =
                            JahisParser.parsePrescription(
                                allQrData
                            )

                        if (record.medicines.isEmpty()) {
                            Toast.makeText(
                                context,
                                "薬の情報を見つけられませんでした",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {
                            val saved =
                                PrescriptionStorage.saveRecord(
                                    context = context,
                                    record = record
                                )

                            saveStatusText =
                                if (saved) {
                                    "記録に保存しました"
                                } else {
                                    "この記録は保存済みです"
                                }

                            prescriptionRecord = record
                            currentScreen = AppScreen.RESULT
                        }
                    }
                },

                onShowDebug = {
                    if (qrCount == 0) {
                        Toast.makeText(
                            context,
                            "先にQRコードを読み取ってください",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {
                        debugText =
                            JahisDebugFormatter.format(
                                scanSession.getAllQrData()
                            )

                        currentScreen = AppScreen.DEBUG
                    }
                }
            )
        }

        AppScreen.RESULT -> {
            val record = prescriptionRecord

            if (record != null) {
                PrescriptionResultScreen(
                    prescriptionRecord = record,
                    qrCodeCount = qrCount,
                    saveStatusText = saveStatusText,
                    onBackToHome = {
                        clearSession(
                            scanSession = scanSession,
                            onQrCountChanged = {
                                qrCount = it
                            },
                            onRecordChanged = {
                                prescriptionRecord = it
                            },
                            onDebugChanged = {
                                debugText = it
                            }
                        )

                        scanNotice = ""
                        saveStatusText = ""
                        currentScreen = AppScreen.HOME
                    }
                )
            }
        }

        AppScreen.HISTORY -> {
            HistoryScreen(
                records = savedRecords,
                onRecordClick = { record ->
                    selectedHistoryRecord = record
                    currentScreen = AppScreen.HISTORY_DETAIL
                },
                onBackToHome = {
                    currentScreen = AppScreen.HOME
                }
            )
        }

        AppScreen.HISTORY_DETAIL -> {
            val record = selectedHistoryRecord

            if (record != null) {
                HistoryDetailScreen(
                    prescriptionRecord = record,
                    onDeleteRecord = {
                        val deleted =
                            PrescriptionStorage.deleteRecord(
                                context = context,
                                record = record
                            )

                        Toast.makeText(
                            context,
                            if (deleted) {
                                "この記録を削除しました"
                            } else {
                                "この記録は見つかりませんでした"
                            },
                            Toast.LENGTH_SHORT
                        ).show()

                        savedRecords =
                            PrescriptionStorage.loadRecords(
                                context
                            )

                        selectedHistoryRecord = null
                        currentScreen = AppScreen.HISTORY
                    },
                    onBackToHistory = {
                        currentScreen = AppScreen.HISTORY
                    }
                )
            }
        }

        AppScreen.DEBUG -> {
            DebugResultScreen(
                debugText = debugText,
                onBackToHome = {
                    currentScreen = AppScreen.HOME
                }
            )
        }
    }
}

private fun clearSession(
    scanSession: QrScanSession,
    onQrCountChanged: (Int) -> Unit,
    onRecordChanged: (PrescriptionRecord?) -> Unit,
    onDebugChanged: (String) -> Unit
) {
    scanSession.clear()
    onQrCountChanged(0)
    onRecordChanged(null)
    onDebugChanged("")
}

private enum class AppScreen {
    HOME,
    RESULT,
    HISTORY,
    HISTORY_DETAIL,
    DEBUG
}

@Composable
fun HomeScreen(
    qrCodeCount: Int,
    scanNotice: String,
    onShowHistory: () -> Unit,
    onScanQrCode: () -> Unit,
    onFinishReading: () -> Unit,
    onShowDebug: () -> Unit,
    onExportCsv: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 84.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "お薬手帳",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Medium
        )

        OutlinedButton(
            onClick = onShowHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .height(64.dp),
            shape = ButtonShape,
            border = BorderStroke(
                width = 3.dp,
                color = ButtonPurple
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = ButtonPurple
            )
        ) {
            Text(
                text = "記  録  を  見  る",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,

            )


        }

        if (qrCodeCount == 0) {

            Button(
                onClick = onScanQrCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(64.dp),
                shape = ButtonShape,
                border = BorderStroke(
                    width = 2.dp,
                    color = Color.White
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "QRコードを読み取る",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(
                modifier = Modifier.height(180.dp)
            )

            OutlinedButton(
                onClick = onExportCsv,

                modifier = Modifier
                    .width(170.dp)
                    .height(48.dp),
                shape = ButtonShape,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.Gray
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Gray
                )
            ) {
                Text(
                    text = "CSV出力",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {

            Text(
                text = "✓ QRコードを${qrCodeCount}件読み取りました",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 28.dp)
            )

            Button(
                onClick = onScanQrCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(60.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NextQrButtonOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "次のQRコードを読む",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onFinishReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(60.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinishReadingGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "読み取り終了",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }

        if (scanNotice.isNotBlank()) {
            Text(
                text = scanNotice,
                color = Color(0xFFB3261E),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 25.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
        }
    }
}

@Composable
fun PrescriptionResultScreen(
    prescriptionRecord: PrescriptionRecord,
    qrCodeCount: Int,
    saveStatusText: String,
    onBackToHome: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 28.dp,
                bottom = 24.dp
            )
    ) {

        Text(
            text = "今回のお薬",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )

        if (
            prescriptionRecord.prescriptionDate.isNotBlank() ||
            prescriptionRecord.hospitalName.isNotBlank()
        ) {
            Text(
                text = listOf(
                    prescriptionRecord.prescriptionDate,
                    prescriptionRecord.hospitalName
                )
                    .filter { it.isNotBlank() }
                    .joinToString("　"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        if (
            prescriptionRecord.department.isNotBlank() ||
            prescriptionRecord.doctorName.isNotBlank()
        ) {
            Text(
                text = listOf(
                    prescriptionRecord.department,
                    prescriptionRecord.doctorName
                        .takeIf { it.isNotBlank() }
                        ?.plus(" 先生")
                        .orEmpty()
                )
                    .filter { it.isNotBlank() }
                    .joinToString("　"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Text(
            text = "QRコード ${qrCodeCount}件から読み取りました",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        Text(
            text = saveStatusText,
            color = FinishReadingGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {

            items(
                items = prescriptionRecord.medicines
            ) { medicine ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {

                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    medicine.usage.forEach { usageLine ->
                        Text(
                            text = usageLine,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                start = 16.dp
                            )
                        )
                    }

                    if (medicine.quantityInfo.isNotBlank()) {
                        Text(
                            text = medicine.quantityInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                start = 16.dp
                            )
                        )
                    }
                }

                HorizontalDivider()
            }
        }

        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = ButtonShape,
            border = BorderStroke(
                width = 3.dp,
                color = ButtonPurple
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = ButtonPurple
            )
        ) {
            Text(
                text = "ホームへ戻る",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HistoryDetailScreen(
    prescriptionRecord: PrescriptionRecord,
    onDeleteRecord: () -> Unit,
    onBackToHistory: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 28.dp,
                bottom = 24.dp
            )
    ) {

        Text(
            text = "お薬の記録",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )

        Text(
            text = listOf(
                prescriptionRecord.prescriptionDate,
                prescriptionRecord.hospitalName
            )
                .filter { it.isNotBlank() }
                .joinToString("　"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (
            prescriptionRecord.department.isNotBlank() ||
            prescriptionRecord.doctorName.isNotBlank()
        ) {
            Text(
                text = listOf(
                    prescriptionRecord.department,
                    prescriptionRecord.doctorName
                        .takeIf { it.isNotBlank() }
                        ?.plus(" 先生")
                        .orEmpty()
                )
                    .filter { it.isNotBlank() }
                    .joinToString("　"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {

            items(
                items = prescriptionRecord.medicines
            ) { medicine ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {

                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    medicine.usage.forEach { usageLine ->
                        Text(
                            text = usageLine,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                start = 16.dp
                            )
                        )
                    }

                    if (medicine.quantityInfo.isNotBlank()) {
                        Text(
                            text = medicine.quantityInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                start = 16.dp
                            )
                        )
                    }
                }

                HorizontalDivider()
            }
        }

        OutlinedButton(
            onClick = onBackToHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(64.dp),
            shape = ButtonShape,
            border = BorderStroke(
                width = 3.dp,
                color = ButtonPurple
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = ButtonPurple
            )
        ) {
            Text(
                text = "記録一覧へ戻る",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DebugResultScreen(
    debugText: String,
    onBackToHome: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 20.dp
            )
    ) {

        Text(
            text = "JAHIS解析結果",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            item {
                Text(
                    text = debugText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPurple,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "ホームへ戻る",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
