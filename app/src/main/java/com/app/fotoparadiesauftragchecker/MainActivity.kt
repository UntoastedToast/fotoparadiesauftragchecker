package com.app.fotoparadiesauftragchecker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.fotoparadiesauftragchecker.adapter.OrdersAdapter
import com.app.fotoparadiesauftragchecker.databinding.ActivityMainBinding
import com.app.fotoparadiesauftragchecker.databinding.DialogAddOrderBinding
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import com.app.fotoparadiesauftragchecker.viewmodel.OrderViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: OrderViewModel by viewModels()
    private val adapter = OrdersAdapter()
    private lateinit var notificationService: NotificationService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showTestNotification()
        } else {
            Snackbar.make(
                binding.root,
                "Benachrichtigungen sind nicht erlaubt",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationService = NotificationService(this)
        
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.ordersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.ordersRecyclerView.adapter = adapter

        // Setup swipe to delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val order = adapter.currentList[position]
                
                // Delete the order
                viewModel.deleteOrder(order)
                
                // Show undo snackbar
                Snackbar.make(
                    binding.root,
                    "Auftrag ${order.orderNumber} gelöscht",
                    Snackbar.LENGTH_LONG
                ).setAction("Rückgängig") {
                    viewModel.addOrder(
                        order.retailerId.toInt(),
                        order.orderNumber
                    )
                }.show()
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val deleteIcon = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_delete_24
                )
                val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2
                
                if (dX > 0) { // Swiping to the right
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + (deleteIcon?.intrinsicWidth ?: 0)
                    deleteIcon?.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.bottom - iconMargin)
                    
                    val background = ColorDrawable()
                    background.color = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError)
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    
                    background.draw(canvas)
                    deleteIcon?.draw(canvas)
                } else if (dX < 0) { // Swiping to the left
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = itemView.right - iconMargin - (deleteIcon?.intrinsicWidth ?: 0)
                    deleteIcon?.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.bottom - iconMargin)
                    
                    val background = ColorDrawable()
                    background.color = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    
                    background.draw(canvas)
                    deleteIcon?.draw(canvas)
                }
                
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.ordersRecyclerView)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshOrders()
        }
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
            binding.swipeRefreshLayout.isRefreshing = false
        }

        viewModel.error.observe(this) { error ->
            binding.swipeRefreshLayout.isRefreshing = false
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun checkNotificationPermission(onGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showTestNotification() {
        notificationService.showOrderReadyNotification(
            orderId = "12345",
            retailerId = "1320"
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_test_notification -> {
                checkNotificationPermission {
                    showTestNotification()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}