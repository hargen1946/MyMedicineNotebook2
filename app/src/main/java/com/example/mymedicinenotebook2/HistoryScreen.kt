package com.example.mymedicinenotebook2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(
    records: List<PrescriptionRecord>,
    onRecordClick: (PrescriptionRecord) -> Unit,
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
            text = "記録を見る",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "見たい記録をタップしてください",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (records.isEmpty()) {
            Text(
                text = "保存された記録はまだありません。",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                itemsIndexed(records) { index, record ->
                    Text(
                        text = listOf(
                            record.prescriptionDate,
                            record.hospitalName
                        )
                            .filter { it.isNotBlank() }
                            .joinToString("　"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecordClick(record)
                            }
                            .padding(
                                top = 22.dp,
                                bottom = 22.dp
                            )
                    )

                    if (index < records.lastIndex) {
                        HorizontalDivider()
                        Text(
                            text = "",
                            modifier = Modifier.padding(
                                bottom = 10.dp
                            )
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(
                width = 3.dp,
                color = Color(0xFF6750A4)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF6750A4)
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
