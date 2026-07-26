package com.capstone.houseviewingapp.analysis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.ItemAnalysisRecordBinding

class AnalysisRecordAdapter(
    private val onLongClickDelete: (item: AnalysisRecordItem) -> Unit,
    private val onDetailClick: (item: AnalysisRecordItem) -> Unit
) : RecyclerView.Adapter<AnalysisRecordAdapter.RecordVH>() {
    private data class UiSpec(
        val badgeText: String,
        val badgeBgRes: Int,
        val textColorRes: Int,
        val iconRes: Int
    )

    private val items = mutableListOf<AnalysisRecordItem>()

    fun submit(newItems: List<AnalysisRecordItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordVH {
        val binding = ItemAnalysisRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordVH(binding)
    }

    override fun onBindViewHolder(holder: RecordVH, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onLongClickDelete(items[pos])
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size

    inner class RecordVH(private val binding: ItemAnalysisRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalysisRecordItem) {
            binding.titleTextView.text = item.title
            binding.addressTextView.text = item.address
            binding.riskTextView.text = summarizeRisk(item.riskSummary)

            val rawScore = item.ltv?.toInt()?.coerceIn(0, 100)
            val normalizedScore = rawScore
            // 등급은 서버 riskLevel 기준으로 고정한다.
            // LTV 점수는 보조지표이며 카드 색/뱃지를 덮어쓰지 않는다.
            val displayLevel = item.level

            val (badgeText, badgeBgRes, textColorRes, iconRes) = when (displayLevel) {
                RiskLevel.RED -> UiSpec(
                    "고위험",
                    R.drawable.bg_analysis_badge_red,
                    R.color.risk_red_text,
                    R.drawable.baseline_warning_24
                )

                RiskLevel.AMBER -> UiSpec(
                    "주의",
                    R.drawable.bg_analysis_badge_amber,
                    R.color.risk_amber_text,
                    R.drawable.baseline_warning_24
                )

                RiskLevel.BLUE -> UiSpec(
                    "안전",
                    R.drawable.bg_analysis_badge_blue,
                    R.color.risk_blue_text,
                    R.drawable.round_check_circle_24
                )
            }

            val c = ContextCompat.getColor(binding.root.context, textColorRes)
            binding.riskBadgeTextView.setTextColor(c)

            binding.riskBadgeTextView.text = badgeText
            binding.riskBadgeTextView.setBackgroundResource(badgeBgRes)

            binding.titleTextView.setTextColor(c)

            binding.riskIconView.setImageResource(iconRes)
            binding.riskIconView.setColorFilter(c)

            // /analyses·/analyses/diff 의 ltvScore(로컬 키 ltv). 없으면 오해 소지 있는 "미산출" 대신 중립 표시.
            binding.scoreDividerView.visibility = android.view.View.VISIBLE
            binding.scoreContainer.visibility = android.view.View.VISIBLE
            if (normalizedScore != null) {
                binding.scoreTextView.text = "${normalizedScore}점"
                binding.scoreTextView.setTextColor(c)
            } else {
                binding.scoreTextView.text = "—"
                binding.scoreTextView.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.textgray)
                )
            }
            binding.detailButton.text = "상세 리포트 확인"
            binding.detailButton.setOnClickListener { onDetailClick(item) }
        }

        private fun summarizeRisk(raw: String): String {
            val text = raw.trim().replace(Regex("\\s+"), " ")
            if (text.isBlank()) return "-"
            val splitToken = listOf(". ", " / ", "; ", "다만 ").firstOrNull { text.contains(it) }
            val primary = if (splitToken != null) text.substringBefore(splitToken).trim() else text
            return if (primary.length > 40) primary.take(40).trimEnd() + "..." else primary
        }
    }
}