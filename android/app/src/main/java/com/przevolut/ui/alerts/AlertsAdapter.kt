package com.przevolut.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.przevolut.R
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.databinding.ItemAlertBinding
import com.przevolut.ui.common.CurrencyUi

class AlertsAdapter(
    private val onDeleteClick: (AlertResponse) -> Unit
) : ListAdapter<AlertResponse, AlertsAdapter.AlertViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(private val binding: ItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: AlertResponse) {
            val context = binding.root.context
            val directionIcon = if (alert.direction == "below") {
                R.drawable.ic_arrow_downward
            } else {
                R.drawable.ic_arrow_upward
            }

            binding.tvFlag.text = CurrencyUi.flag(alert.currency)
            binding.tvCurrencyCode.text = alert.currency
            binding.ivDirection.setImageResource(directionIcon)
            binding.tvTargetRate.text = "%.4f PLN".format(alert.targetRate)

            val (statusText, statusColor, stripeColor) = when {
                alert.isTriggered -> Triple(
                    R.string.alert_status_triggered,
                    R.color.alert_triggered,
                    R.color.alert_triggered
                )
                alert.isActive -> Triple(
                    R.string.alert_status_active,
                    R.color.alert_active,
                    R.color.alert_active
                )
                else -> Triple(
                    R.string.alert_status_inactive,
                    R.color.alert_inactive,
                    R.color.alert_inactive
                )
            }

            binding.chipStatus.text = context.getString(statusText)
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, statusColor))
            binding.viewStatusStripe.setBackgroundColor(
                ContextCompat.getColor(context, stripeColor)
            )
            binding.cardAlert.strokeColor = ContextCompat.getColor(context, stripeColor)

            binding.btnDeleteAlert.contentDescription = context.getString(
                R.string.cd_delete_alert,
                alert.currency
            )
            binding.btnDeleteAlert.setOnClickListener { onDeleteClick(alert) }

            val directionLabel = if (alert.direction == "below") "poniżej" else "powyżej"
            binding.root.contentDescription =
                "${alert.currency} $directionLabel ${"%.4f".format(alert.targetRate)} PLN, " +
                    context.getString(statusText)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AlertResponse>() {
        override fun areItemsTheSame(oldItem: AlertResponse, newItem: AlertResponse) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AlertResponse, newItem: AlertResponse) =
            oldItem == newItem
    }
}
