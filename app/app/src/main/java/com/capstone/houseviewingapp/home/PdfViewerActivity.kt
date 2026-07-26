package com.capstone.houseviewingapp.home

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.capstone.houseviewingapp.BuildConfig
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var openedPdfUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }
        binding.downloadButton.setOnClickListener { downloadCurrentPdf() }

        binding.titleTextView.text = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "PDF 보기" }
        val showReportButton = intent.getBooleanExtra(EXTRA_SHOW_REPORT_BUTTON, false)
        binding.reportIssueTopButton.visibility = View.GONE
        if (showReportButton) {
            binding.reportIssueBottomButton.visibility = View.VISIBLE
            binding.reportIssueBottomButton.setOnClickListener {
                ReportReceivedDialogFragment().show(supportFragmentManager, "ReportReceivedDialog")
            }
        } else {
            binding.reportIssueBottomButton.visibility = View.GONE
        }

        val uriRaw = intent.getStringExtra(EXTRA_URI).orEmpty()
        if (uriRaw.isBlank()) {
            Toast.makeText(this, "PDF 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = Uri.parse(uriRaw)
        val scheme = uri.scheme?.lowercase().orEmpty()
        when (scheme) {
            "http", "https" -> openRemotePdf(uriRaw)
            "content", "file" -> loadPdfFromUri(uri)
            else -> openRemotePdf(uriRaw)
        }
    }

    private fun loadPdfFromUri(uri: Uri) {
        binding.pdfScrollView.isVisible = false
        lifecycleScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                uriToTempFile(uri)?.let { Uri.fromFile(it) }
            }
            if (localUri == null) {
                Toast.makeText(this@PdfViewerActivity, "PDF 파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            val opened = openPdf(localUri)
            if (!opened) {
                Toast.makeText(this@PdfViewerActivity, "PDF를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            openedPdfUri = localUri
            binding.pdfScrollView.isVisible = true
            binding.pdfScrollView.post { renderAllPages() }
        }
    }

    private fun uriToTempFile(uri: Uri): File? {
        return runCatching {
            when (uri.scheme?.lowercase()) {
                "file" -> uri.toFile().takeIf { it.exists() && it.length() > 0L }
                else -> {
                    val out = File(cacheDir, "pdf_viewer_${uri.hashCode()}.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    out.takeIf { it.exists() && it.length() > 0L }
                }
            }
        }.getOrNull()
    }

    private fun openPdf(uri: Uri): Boolean {
        return runCatching {
            closePdf()
            fileDescriptor = when (uri.scheme?.lowercase()) {
                "file" -> ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
                else -> contentResolver.openFileDescriptor(uri, "r")
            }
            val fd = fileDescriptor ?: return false
            pdfRenderer = PdfRenderer(fd)
            true
        }.getOrDefault(false)
    }

    private fun closePdf() {
        pdfRenderer?.close()
        pdfRenderer = null
        fileDescriptor?.close()
        fileDescriptor = null
    }

    private fun renderAllPages() {
        val renderer = pdfRenderer ?: return
        val container = binding.pdfPagesContainer
        container.removeAllViews()
        val baseWidth = (binding.pdfFrame.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels) - dpToPx(8)
        val targetWidth = baseWidth.coerceAtLeast(dpToPx(180))

        for (index in 0 until renderer.pageCount) {
            val page = renderer.openPage(index)
            val bitmap = renderPageBitmap(page, targetWidth)
            page.close()
            container.addView(
                buildPageBlock(
                    bitmap = bitmap,
                    pageIndex = index,
                    pageCount = renderer.pageCount
                )
            )
        }
        binding.pdfScrollView.post { binding.pdfScrollView.scrollTo(0, 0) }
    }

    private fun buildPageBlock(bitmap: Bitmap, pageIndex: Int, pageCount: Int): View {
        val pageContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.leftMargin = dpToPx(8)
                lp.rightMargin = dpToPx(8)
                lp.topMargin = if (pageIndex == 0) dpToPx(8) else dpToPx(16)
                lp.bottomMargin = if (pageIndex == pageCount - 1) dpToPx(16) else 0
            }
            setBackgroundColor(Color.WHITE)
            elevation = dpToPx(1).toFloat()
            setPadding(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
        }

        val pageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.WHITE)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(bitmap)
        }

        pageContainer.addView(pageView)
        return pageContainer
    }

    private fun renderPageBitmap(page: PdfRenderer.Page, targetWidth: Int): Bitmap {
        val renderScale = (targetWidth.toFloat() / page.width.toFloat()).coerceAtLeast(1f)
        val bw = (page.width * renderScale).toInt().coerceAtLeast(1)
        val bh = (page.height * renderScale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val matrix = android.graphics.Matrix().apply {
            setScale(bw.toFloat() / page.width.toFloat(), bh.toFloat() / page.height.toFloat())
        }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return cropOuterWhiteMargins(bitmap)
    }

    private fun openRemotePdf(url: String) {
        binding.pdfScrollView.isVisible = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val resolvedUrl = resolvePdfUrl(url)
                    val token = AuthTokenLocalStore.getAccessToken(this@PdfViewerActivity).orEmpty()
                    downloadPdfToCache(resolvedUrl, token)
                }.getOrElse { PdfLoadResult.Failure("네트워크 요청 예외") }
            }
            val localUri = (result as? PdfLoadResult.Success)?.uri
            if (localUri == null) {
                val reason = (result as? PdfLoadResult.Failure)?.reason ?: "알 수 없는 오류"
                Toast.makeText(this@PdfViewerActivity, "대응 PDF를 불러오지 못했습니다. ($reason)", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            val opened = openPdf(localUri)
            if (!opened) {
                Toast.makeText(this@PdfViewerActivity, "PDF를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            openedPdfUri = localUri
            binding.pdfScrollView.isVisible = true
            binding.pdfScrollView.post { renderAllPages() }
        }
    }

    private fun downloadCurrentPdf() {
        val sourceUri = openedPdfUri
        if (sourceUri == null) {
            Toast.makeText(this, "다운로드할 PDF가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val savedName = withContext(Dispatchers.IO) {
                savePdfToDownloads(sourceUri)
            }
            if (savedName != null) {
                Toast.makeText(this@PdfViewerActivity, "다운로드 완료: $savedName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@PdfViewerActivity, "다운로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePdfToDownloads(sourceUri: Uri): String? {
        return runCatching {
            val displayTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "analysis_report" }
            val safeTitle = displayTitle.replace(Regex("[^A-Za-z0-9가-힣 _.-]"), "_")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
            val fileName = "${safeTitle}_$timestamp.pdf"
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download")
            }
            val targetUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null

            contentResolver.openInputStream(sourceUri).use { input ->
                if (input == null) return null
                contentResolver.openOutputStream(targetUri, "w").use { output ->
                    if (output == null) return null
                    input.copyTo(output)
                }
            }
            fileName
        }.getOrNull()
    }

    private fun downloadPdfToCache(url: String, accessToken: String): PdfLoadResult {
        val httpUrl = url.toHttpUrlOrNull() ?: return PdfLoadResult.Failure("잘못된 URL")
        val attachAuthFirst = shouldAttachAuthHeader(httpUrl.toString(), accessToken)
        val firstToken = if (attachAuthFirst) accessToken else ""
        val secondToken = if (attachAuthFirst) "" else accessToken

        val firstResult = executePdfRequest(httpUrl.toString(), firstToken)
        if (firstResult is PdfLoadResult.Success) return firstResult

        val firstFailure = (firstResult as? PdfLoadResult.Failure)?.reason.orEmpty()
        val shouldRetryWithoutAuth =
            accessToken.isNotBlank() && (firstFailure.contains("HTTP 400") || firstFailure.contains("HTTP 401") || firstFailure.contains("HTTP 403"))
        if (!shouldRetryWithoutAuth) return firstResult

        val secondResult = executePdfRequest(httpUrl.toString(), secondToken)
        if (secondResult is PdfLoadResult.Success) return secondResult
        val secondFailure = (secondResult as? PdfLoadResult.Failure)?.reason.orEmpty()
        return PdfLoadResult.Failure("$firstFailure / retry: $secondFailure")
    }

    private fun executePdfRequest(url: String, accessToken: String): PdfLoadResult {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            // 프록시/중간 캐시가 오래된 PDF를 내려주는 경우를 줄인다.
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
        if (accessToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }
        val request = requestBuilder.build()
        val client = OkHttpClient()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("PdfViewerActivity", "pdf fetch failed url=$url http=${response.code}")
                return PdfLoadResult.Failure("HTTP ${response.code}")
            }
            val body = response.body ?: return PdfLoadResult.Failure("응답 본문 없음")
            val file = File(cacheDir, "analysis_remote_${url.hashCode()}.pdf")
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return PdfLoadResult.Success(Uri.fromFile(file))
        }
    }

    private fun resolvePdfUrl(raw: String): String {
        val trimmed = raw.trim()
        val resolved = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            val base = BuildConfig.API_BASE_URL.trimEnd('/')
            val path = trimmed.trimStart('/')
            "$base/$path"
        }
        return alignPdfUrlHostWithApiBase(resolved)
    }

    private fun alignPdfUrlHostWithApiBase(url: String): String {
        val target = url.toHttpUrlOrNull() ?: return url
        val base = BuildConfig.API_BASE_URL.toHttpUrlOrNull() ?: return url
        val loopbackHosts = setOf("127.0.0.1", "localhost")
        val targetHost = target.host.lowercase()
        if (targetHost !in loopbackHosts) return url
        val baseHost = base.host.lowercase()
        if (baseHost in loopbackHosts) return url
        return target.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
            .toString()
    }

    override fun onDestroy() {
        closePdf()
        super.onDestroy()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun cropOuterWhiteMargins(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 10 || h < 10) return source

        val left = scanEdge(source, horizontal = false, fromStart = true)
        val right = scanEdge(source, horizontal = false, fromStart = false)
        val top = scanEdge(source, horizontal = true, fromStart = true)
        val bottom = scanEdge(source, horizontal = true, fromStart = false)
        if (left >= right || top >= bottom) return source

        // 안전 장치: 과도한 크롭 방지(페이지 12% 이상은 자르지 않음)
        val maxCropX = (w * 0.12f).toInt()
        val maxCropY = (h * 0.12f).toInt()
        val safeLeft = min(left, maxCropX)
        val safeRight = max(right, w - 1 - maxCropX)
        val safeTop = min(top, maxCropY)
        val safeBottom = max(bottom, h - 1 - maxCropY)

        if (safeLeft >= safeRight || safeTop >= safeBottom) return source
        val cropW = safeRight - safeLeft + 1
        val cropH = safeBottom - safeTop + 1
        if (cropW <= 0 || cropH <= 0 || (cropW == w && cropH == h)) return source
        return Bitmap.createBitmap(source, safeLeft, safeTop, cropW, cropH)
    }

    private fun scanEdge(
        bitmap: Bitmap,
        horizontal: Boolean,
        fromStart: Boolean
    ): Int {
        val width = bitmap.width
        val height = bitmap.height
        val primaryLimit = if (horizontal) height else width
        val secondaryLimit = if (horizontal) width else height
        val start = if (fromStart) 0 else primaryLimit - 1
        val end = if (fromStart) primaryLimit else -1
        val step = if (fromStart) 1 else -1
        val sampleStep = 2

        var p = start
        while (p != end) {
            var contentCount = 0
            var s = 0
            while (s < secondaryLimit) {
                val x = if (horizontal) s else p
                val y = if (horizontal) p else s
                if (!isNearWhite(bitmap.getPixel(x, y))) contentCount++
                s += sampleStep
            }
            // 얇은 선/노이즈가 아니라 실제 컨텐츠가 있는 줄로 판단
            if (contentCount >= 8) return p
            p += step
        }
        return if (fromStart) 0 else primaryLimit - 1
    }

    private fun isNearWhite(pixel: Int): Boolean {
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        return r >= 246 && g >= 246 && b >= 246
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SHOW_REPORT_BUTTON = "extra_show_report_button"
    }

    private fun shouldAttachAuthHeader(url: String, accessToken: String): Boolean {
        if (accessToken.isBlank()) return false
        val target = url.toHttpUrlOrNull() ?: return false
        if (target.queryParameterNames.any { it.startsWith("X-Amz-", ignoreCase = true) }) return false

        val base = BuildConfig.API_BASE_URL.toHttpUrlOrNull() ?: return false
        return target.host.equals(base.host, ignoreCase = true) && target.port == base.port
    }

    private sealed class PdfLoadResult {
        data class Success(val uri: Uri) : PdfLoadResult()
        data class Failure(val reason: String) : PdfLoadResult()
    }
}
