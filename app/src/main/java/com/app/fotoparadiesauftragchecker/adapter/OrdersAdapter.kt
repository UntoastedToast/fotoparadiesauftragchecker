package com.app.fotoparadiesauftragchecker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.orderNumberText.text = "Auftrag: ${order.orderNumber}"
            binding.statusText.text = "Status: ${order.status}"
            binding.priceText.text = "Preis: ${order.price}"
            binding.lastUpdateText.text = "Letztes Update: ${order.lastUpdate}"
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
