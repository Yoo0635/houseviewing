package com.capstone.houseviewingapp.login

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.capstone.houseviewingapp.databinding.DialogAgreementBinding

class AgreementDialogFragment : DialogFragment() {
    private var _binding: DialogAgreementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val titleRes = requireArguments().getInt(ARG_TITLE_RES)
        val contentRes = requireArguments().getInt(ARG_CONTENT_RES)

        binding.titleTextView.setText(titleRes)
        val bodyHtml = getString(contentRes)
        binding.contentWebView.apply {
            settings.javaScriptEnabled = false
            settings.defaultTextEncodingName = "utf-8"
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = false
            setBackgroundColor(Color.TRANSPARENT)

            // 다른 앱들처럼 문서 레이아웃(목록/여백/행간)을 CSS로 맞춤
            val html = """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <style>
                    body {
                      margin: 0;
                      padding: 0 2px;
                      font-size: 14px;
                      line-height: 1.65;
                      color: #4A4A4A;
                      word-break: keep-all;
                    }
                    b { color: #111111; }
                    p { margin: 0 0 10px 0; }
                    br { line-height: 1.65; }
                    ol, ul {
                      margin: 8px 0 14px 0;
                      padding-left: 22px;
                    }
                    li {
                      margin: 8px 0;
                    }
                    /* 불릿 크기/간격(불릿 뒤 여백) 개선 */
                    ul { list-style-type: disc; }
                    li::marker { font-size: 1.2em; }
                    /* 번호 목록도 항목 사이 간격 확보 */
                    ol li { padding-left: 2px; }
                  </style>
                </head>
                <body>
                  $bodyHtml
                </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }

        binding.confirmButton.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.88f).toInt()
        dialog?.window?.setLayout(width, height)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE_RES = "title_res"
        private const val ARG_CONTENT_RES = "content_res"

        fun newInstance(@StringRes titleRes: Int, @StringRes contentRes: Int): AgreementDialogFragment {
            return AgreementDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE_RES, titleRes)
                    putInt(ARG_CONTENT_RES, contentRes)
                }
            }
        }
    }
}

