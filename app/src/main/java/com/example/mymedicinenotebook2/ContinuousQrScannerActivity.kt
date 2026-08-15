package com.example.mymedicinenotebook2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class ContinuousQrScannerActivity : ComponentActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var statusText: TextView
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button

    private val qrDataList =
        mutableListOf<String>()

    /*
     * true の間は、カメラは動かしたままですが
     * 読み取り結果を受け付けません。
     */
    private var waitingForChoice =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        buildScreen()

        barcodeView.decoderFactory =
            DefaultDecoderFactory(
                listOf(
                    BarcodeFormat.QR_CODE
                )
            )

        barcodeView
            .barcodeView
            .cameraSettings
            .isAutoFocusEnabled = true

        if (
            checkSelfPermission(
                Manifest.permission.CAMERA
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.resume()
            startContinuousReading()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun buildScreen() {

        val root =
            FrameLayout(this)

        barcodeView =
            DecoratedBarcodeView(this)

        barcodeView.setStatusText("")

        root.addView(
            barcodeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val controls =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    32,
                    20,
                    32,
                    32
                )

                setBackgroundColor(
                    Color.argb(
                        215,
                        255,
                        255,
                        255
                    )
                )
            }

        statusText =
            TextView(this).apply {
                text =
                    "QRコードをカメラに映してください"

                textSize =
                    20f

                // 端末の横幅に合わせて文字を縮め、一行に収めます
                setSingleLine(true)
                setAutoSizeTextTypeUniformWithConfiguration(
                    14,
                    20,
                    1,
                    TypedValue.COMPLEX_UNIT_SP
                )

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.rgb(
                        45,
                        45,
                        45
                    )
                )
            }

        nextButton =
            Button(this).apply {
                text =
                    "次のQRコードを読む"

                textSize =
                    19f

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        224,
                        122,
                        45
                    )
                )

                visibility =
                    View.GONE

                setOnClickListener {

                    waitingForChoice =
                        false

                    statusText.text =
                        "次のQRコードをカメラに映してください"

                    nextButton.visibility =
                        View.GONE

                    finishButton.visibility =
                        View.GONE
                }
            }

        finishButton =
            Button(this).apply {
                text =
                    "読み取り終了"

                textSize =
                    19f

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        63,
                        143,
                        91
                    )
                )

                visibility =
                    View.GONE

                setOnClickListener {

                    if (
                        finishButton.text ==
                        "ホームへ戻る"
                    ) {
                        setResult(
                            Activity.RESULT_CANCELED
                        )

                        finish()

                    } else {
                        finishReading()
                    }
                }
            }

        controls.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin =
                    16
            }
        )

        controls.addView(
            nextButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            ).apply {
                bottomMargin =
                    16
            }
        )

        controls.addView(
            finishButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            )
        )

        root.addView(
            controls,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        setContentView(root)
    }

    private fun startContinuousReading() {

        barcodeView.decodeContinuous(
            object : BarcodeCallback {

                override fun barcodeResult(
                    result: BarcodeResult?
                ) {

                    if (waitingForChoice) {
                        return
                    }

                    val rawText =
                        result
                            ?.text
                            ?.trim()
                            .orEmpty()

                    if (rawText.isBlank()) {
                        return
                    }

                    if (
                        QrFingerprintStore.contains(
                            context = this@ContinuousQrScannerActivity,
                            qrData = rawText
                        )
                    ) {
                        waitingForChoice = true

                        statusText.text =
                            "既に記録済みのQRコードです。"

                        nextButton.visibility =
                            View.GONE

                        finishButton.text =
                            "ホームへ戻る"

                        finishButton.visibility =
                            View.VISIBLE

                        return
                    }

                    if (
                        qrDataList.any {
                            normalizeQrData(it) ==
                                    normalizeQrData(
                                        rawText
                                    )
                        }
                    ) {
                        waitingForChoice =
                            true

                        statusText.text =
                            "すでに読み取り済みのQRコードを検出しました。\n" +
                                    "「次のQRコードを読む」または\n" +
                                    "「読み取り終了」を押してください。"

                        nextButton.visibility =
                            View.VISIBLE

                        finishButton.visibility =
                            View.VISIBLE

                        return
                    }

                    if (
                        qrDataList.isEmpty() &&
                        !containsHospitalRecord(
                            rawText
                        )
                    ) {
                        waitingForChoice =
                            true

                        statusText.text =
                            "QRコードの順番が違います。\n" +
                                    "最初からやり直してください。"

                        nextButton.visibility =
                            View.GONE

                        finishButton.text =
                            "ホームへ戻る"

                        finishButton.visibility =
                            View.VISIBLE

                        return
                    }

                    qrDataList.add(
                        rawText
                    )

                    waitingForChoice =
                        true

                    statusText.text =
                        "✓ QRコードを${qrDataList.size}件読み取りました"

                    nextButton.visibility =
                        View.VISIBLE

                    finishButton.visibility =
                        View.VISIBLE
                }
            }
        )
    }

    private fun finishReading() {

        if (qrDataList.isEmpty()) {
            return
        }

        val resultIntent =
            Intent().apply {
                putStringArrayListExtra(
                    EXTRA_QR_DATA_LIST,
                    ArrayList(
                        qrDataList
                    )
                )
            }

        setResult(
            Activity.RESULT_OK,
            resultIntent
        )

        finish()
    }

    private fun containsHospitalRecord(
        qrData: String
    ): Boolean {

        return qrData
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                "\r",
                "\n"
            )
            .lines()
            .any { line ->
                line
                    .trimStart()
                    .startsWith("51,")
            }
    }

    private fun normalizeQrData(
        qrData: String
    ): String {

        return qrData
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                "\r",
                "\n"
            )
            .lines()
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
            .joinToString(
                separator =
                    "\n"
            )
            .replace(
                Regex(
                    """\s+"""
                ),
                ""
            )
    }

    override fun onResume() {
        super.onResume()

        if (
            ::barcodeView
                .isInitialized
        ) {
            barcodeView.resume()
        }
    }

    override fun onPause() {

        if (
            ::barcodeView
                .isInitialized
        ) {
            barcodeView.pause()
        }

        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.resume()
            startContinuousReading()
        } else {
            setResult(
                Activity.RESULT_CANCELED
            )

            finish()
        }
    }

    companion object {

        const val EXTRA_QR_DATA_LIST =
            "qr_data_list"

        private const val CAMERA_PERMISSION_REQUEST =
            2001
    }
}
