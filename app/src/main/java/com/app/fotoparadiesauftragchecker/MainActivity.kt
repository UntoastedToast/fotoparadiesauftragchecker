package com.app.fotoparadiesauftragchecker

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.fotoparadiesauftragchecker.adapter.OrdersAdapter
import com.app.fotoparadiesauftragchecker.databinding.ActivityMainBinding
import com.app.fotoparadiesauftragchecker.databinding.DialogAddOrderBinding
import com.app.fotoparadiesauftragchecker.viewmodel.OrderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: OrderViewModel by viewModels()
    private val adapter = OrdersAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.ordersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.ordersRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.addOrderFab.setOnClickListener {
            showAddOrderDialog()
        }
    }

    private fun showAddOrderDialog() {
        val dialogBinding = DialogAddOrderBinding.inflate(LayoutInflater.from(this))
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Neuen Auftrag hinzufügen")
            .setView(dialogBinding.root)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val shop = dialogBinding.shopInput.text.toString().toIntOrNull()
                val order = dialogBinding.orderInput.text.toString().toIntOrNull()

                if (shop == null || order == null) {
                    Snackbar.make(binding.root, "Bitte gültige Nummern eingeben", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                viewModel.addOrder(shop, order)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.orders.observe(this) { orders ->
            adapter.submitList(orders)
        }

        viewModel.error.observe(this) { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }
    }
}