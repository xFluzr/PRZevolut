package com.przevolut.ui.dashboard

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.przevolut.R
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.databinding.ItemRateBinding
import com.przevolut.ui.common.CurrencyUi

class RatesAdapter : ListAdapter<RateEntity, RatesAdapter.RateViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateViewHolder {
        val binding = ItemRateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RateViewHolder(private val binding: ItemRateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rate: RateEntity) {
            val context = binding.root.context
            val currencyName = context.getString(CurrencyUi.nameRes(rate.currency))
            val accentColor = ContextCompat.getColor(context, CurrencyUi.colorRes(rate.currency))

            binding.tvFlag.text = CurrencyUi.flag(rate.currency)
            binding.cardFlag.setCardBackgroundColor(accentColor)
            binding.tvCurrencyCode.text = rate.currency
            binding.tvCurrencyName.text = currencyName
            binding.tvRate.text = "%.4f PLN".format(rate.mid)
            binding.tvEffectiveDate.text = rate.effectiveDate

            binding.ivTrend.setImageResource(R.drawable.ic_trend_up)
            binding.tvTrend.text = context.getString(R.string.trend_placeholder)
            binding.tvTrend.setTextColor(ContextCompat.getColor(context, R.color.trend_up))

            binding.root.contentDescription = context.getString(
                R.string.cd_rate_card,
                rate.currency,
                currencyName,
                "%.4f".format(rate.mid)
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RateEntity>() {
        override fun areItemsTheSame(oldItem: RateEntity, newItem: RateEntity) =
            oldItem.currency == newItem.currency

        override fun areContentsTheSame(oldItem: RateEntity, newItem: RateEntity) =
            oldItem == newItem
    }
}

fun resolveAttrColor(context: Context, @AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}
