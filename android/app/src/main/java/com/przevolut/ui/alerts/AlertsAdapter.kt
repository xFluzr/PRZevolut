package com.przevolut.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.databinding.ItemAlertBinding

/**
 * Adapter RecyclerView dla listy alertów walutowych.
 */
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
            val directionStr = if (alert.direction == "below") "poniżej" else "powyżej"
            binding.tvAlertDescription.text =
                "${alert.currency} $directionStr ${"%.4f".format(alert.targetRate)} PLN"
            binding.tvAlertStatus.text = when {
                alert.isTriggered -> "✅ Wyzwolony"
                alert.isActive -> "🔔 Aktywny"
                else -> "⏸ Nieaktywny"
            }
            binding.btnDeleteAlert.setOnClickListener { onDeleteClick(alert) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AlertResponse>() {
        override fun areItemsTheSame(oldItem: AlertResponse, newItem: AlertResponse) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AlertResponse, newItem: AlertResponse) =
            oldItem == newItem
    }
}
