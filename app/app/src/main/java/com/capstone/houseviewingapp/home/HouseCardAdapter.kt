package com.capstone.houseviewingapp.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.ItemAddHouseCardBinding
import com.capstone.houseviewingapp.databinding.ItemHouseCardBinding

class HouseCardAdapter(
    private val items: List<HouseCardItem>,
    private val isPremiumUser: Boolean,
    private val onDelete: (HouseCardItem, Int) -> Unit,
    private val onEdit: (HouseCardItem, Int) -> Unit,
    private val onAddClick: () -> Unit,
    private val onCardClick: (HouseCardItem) -> Unit

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    companion object {
        private const val VIEW_TYPE_HOUSE = 0
        private const val VIEW_TYPE_ADD = 1
    }

    inner class ViewHolder(val binding: ItemHouseCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class AddViewHolder(val binding: ItemAddHouseCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (position == items.size) VIEW_TYPE_ADD else VIEW_TYPE_HOUSE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADD -> {
                val binding = ItemAddHouseCardBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddViewHolder(binding)
            }

            else -> {
                val binding = ItemHouseCardBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {   // [추가] 분기 시작
            is AddViewHolder -> {
                holder.binding.addCardRoot.setOnClickListener { onAddClick() }
            }

            is ViewHolder -> {
                val item = items[position]
                with(holder.binding) {
                    if (isPremiumUser) {
                        chip.text = "LIVE 감시중"
                        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(root.context, R.color.green)
                        )
                        chip.setTextColor(ContextCompat.getColor(root.context, R.color.textgreen))
                    } else {
                        chip.text = "실시간 감시 미적용"
                        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(root.context, R.color.risk_amber_bg)
                        )
                        chip.setTextColor(ContextCompat.getColor(root.context, R.color.risk_amber_text))
                    }
                    homeNameText.text = item.homeName
                    addressText.text = item.address
                    progressBar.max = 100
                    val ltv = item.ltv
                    if (ltv == null) {
                        ltvpercentTextView.text = "--"
                        percentText.text = ""
                        progressBar.progress = 0
                        val pendingColor = ContextCompat.getColor(root.context, R.color.icongray)
                        ltvpercentTextView.setTextColor(pendingColor)
                        percentText.setTextColor(pendingColor)
                        progressBar.setIndicatorColor(pendingColor)
                    } else {
                        ltvpercentTextView.text = ltv.toString()
                        percentText.text = "%"
                        progressBar.progress = ltv
                        // TODO: LTV는 나중에 백엔드가 최종 등급을 줌 -> 그걸로 색상 결정하도록 수정 예정
                        val stateColor = when {
                            ltv <= 59 -> R.color.blue
                            ltv <= 79 -> R.color.amber
                            else -> R.color.red
                        }
                        val c = ContextCompat.getColor(root.context, stateColor)
                        ltvpercentTextView.setTextColor(c)
                        percentText.setTextColor(c)
                        progressBar.setIndicatorColor(c)
                    }
                    moreButton.setOnClickListener { anchor ->
                        val context = anchor.context
                        val popupView = LayoutInflater.from(context)
                            .inflate(R.layout.popup_house_card_action, null, false)

                        popupView.measure(
                            View.MeasureSpec.UNSPECIFIED,
                            View.MeasureSpec.UNSPECIFIED
                        )
                        val popupWidth = popupView.measuredWidth

                        val popupWindow = PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                        ).apply {
                            isOutsideTouchable = true
                            elevation = 0f
                            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        }

                        popupView.findViewById<View>(R.id.actionEditRow).setOnClickListener {
                            popupWindow.dismiss()
                            val currentPos = holder.bindingAdapterPosition
                            if (currentPos != RecyclerView.NO_POSITION) {
                                onEdit(items[currentPos], currentPos)
                            }
                        }

                        popupView.findViewById<View>(R.id.actionDeleteRow).setOnClickListener {
                            popupWindow.dismiss()
                            val currentPos = holder.bindingAdapterPosition
                            if (currentPos != RecyclerView.NO_POSITION) {
                                onDelete(items[currentPos], currentPos)
                            }
                        }

                        val xOff = anchor.width - popupWidth
                        popupWindow.showAsDropDown(anchor, xOff, 10)
                    }
                    homeCard.setOnClickListener {
                        onCardClick(item)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size + 1
}