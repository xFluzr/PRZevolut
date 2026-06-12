package com.przevolut.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.databinding.ItemRateBinding

/**
 * Adapter RecyclerView dla listy kursów walut na Dashboardzie.
 */
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
            binding.tvCurrencyCode.text = rate.currency
            binding.tvRate.text = "%.4f PLN".format(rate.mid)
            binding.tvEffectiveDate.text = rate.effectiveDate

            // Dostępność: TalkBack odczyta pełny opis karty
            binding.root.contentDescription =
                "${rate.currency}: kurs %.4f PLN, data ${rate.effectiveDate}".format(rate.mid)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RateEntity>() {
        override fun areItemsTheSame(oldItem: RateEntity, newItem: RateEntity) =
            oldItem.currency == newItem.currency
        override fun areContentsTheSame(oldItem: RateEntity, newItem: RateEntity) =
            oldItem == newItem
    }
}
