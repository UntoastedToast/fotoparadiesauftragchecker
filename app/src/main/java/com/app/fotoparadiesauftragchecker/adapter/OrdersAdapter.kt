package com.app.fotoparadiesauftragchecker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.fotoparadiesauftragchecker.R
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import com.app.fotoparadiesauftragchecker.databinding.ItemOrderBinding

class OrdersAdapter : ListAdapter<OrderStatus, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: OrderStatus) {
            val context = binding.root.context
            
            binding.orderNumberText.text = context.getString(R.string.order_number_format, order.orderNumber)
            binding.shopNumberText.text = context.getString(R.string.shop_number_format, order.retailerId)
            binding.statusChip.text = order.status
            binding.priceText.text = order.price
            binding.lastUpdateText.text = order.lastUpdate

            // Set order name if available
            if (!order.orderName.isNullOrBlank()) {
                binding.orderNameText.text = order.orderName
                binding.orderNameText.visibility = View.VISIBLE
            } else {
                binding.orderNameText.visibility = View.GONE
            }

            // Set chip text color based on status code
            val textColor = when (order.status.uppercase()) {
                "DELIVERED" -> context.getColor(R.color.status_ready)
                "PROCESSING" -> context.getColor(R.color.status_processing)
                "WAITING", "PENDING" -> context.getColor(R.color.status_pending)
                else -> context.getColor(R.color.status_error)
            }
            binding.statusChip.setTextColor(textColor)
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<OrderStatus>() {
        override fun areItemsTheSame(oldItem: OrderStatus, newItem: OrderStatus): Boolean {
            return oldItem.orderNumber == newItem.orderNumber
        }

        override fun areContentsTheSame(oldItem: OrderStatus, newItem: OrderStatus): Boolean {
            return oldItem == newItem
        }
    }
}