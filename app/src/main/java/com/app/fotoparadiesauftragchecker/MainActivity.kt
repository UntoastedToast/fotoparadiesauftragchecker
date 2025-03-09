package com.app.fotoparadiesauftragchecker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.fotoparadiesauftragchecker.adapter.OrdersAdapter
import com.app.fotoparadiesauftragchecker.databinding.ActivityMainBinding
import com.app.fotoparadiesauftragchecker.databinding.DialogAddOrderBinding
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import com.app.fotoparadiesauftragchecker.ui.state.OrderUiState
import com.app.fotoparadiesauftragchecker.viewmodel.OrderViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationService: NotificationService
    private val adapter = OrdersAdapter()
    private val viewModel: OrderViewModel by viewModels()

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Snackbar.make(binding.root, R.string.permissions_granted, Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, R.string.permissions_required, Snackbar.LENGTH_LONG).show()
        }
    }
    
    // ActivityResultLauncher für die Settings-Activity
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Prüfen, ob Einstellungen geändert wurden
            val settingsChanged = result.data?.getBooleanExtra(SettingsActivity.SETTINGS_CHANGED, false) ?: false
            if (settingsChanged) {
                // Wenn Einstellungen geändert wurden, Daten neu laden
                viewModel.refreshOrders()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-Edge aktivieren
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Insets für das Layout verwalten
        setupInsets()

        notificationService = NotificationService(this)
        
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupRetryButton()
        setupObservers()
        
        checkAndRequestPermissions()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // AppBar mit Statusbar-Insets ausrichten
            binding.appBarLayout.updatePadding(top = insets.top)
            
            // FAB-Margin für die Navigationsleiste anpassen
            val fabParams = binding.addOrderFab.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            val originalMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
            fabParams.bottomMargin = originalMargin + insets.bottom
            binding.addOrderFab.layoutParams = fabParams
            
            windowInsets
        }
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

            @SuppressLint("StringFormatMatches")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val order = adapter.currentList[position]
                
                // Delete the order
                viewModel.deleteOrder(order)
                
                // Show undo snackbar
                Snackbar.make(
                    binding.root,
                    getString(R.string.order_deleted, order.orderNumber),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.undo)) {
                    viewModel.restoreOrder(order)
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
    
    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            viewModel.refreshOrders()
        }
    }

    private fun showAddOrderDialog() {
        val dialogBinding = DialogAddOrderBinding.inflate(LayoutInflater.from(this))
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_new_order))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val shop = dialogBinding.shopInput.text.toString().toIntOrNull()
                val order = dialogBinding.orderInput.text.toString().toIntOrNull()
                val name = dialogBinding.nameInput.text.toString().takeIf { it.isNotBlank() }

                if (shop == null || order == null) {
                    Snackbar.make(binding.root, getString(R.string.enter_valid_numbers), Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                viewModel.addOrder(shop, order, name)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupObservers() {
        // UI-State beobachten
        viewModel.uiState.observe(this) { state ->
            updateUiForState(state)
        }
        
        // Events beobachten (für Snackbar-Nachrichten)
        viewModel.event.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUiForState(state: OrderUiState) {
        binding.swipeRefreshLayout.isRefreshing = false
        
        // Alle Views standardmäßig ausblenden
        binding.ordersRecyclerView.visibility = View.GONE
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        
        // Je nach Zustand die passende View anzeigen
        when (state) {
            is OrderUiState.Loading -> {
                binding.loadingView.visibility = View.VISIBLE
            }
            is OrderUiState.Success -> {
                binding.ordersRecyclerView.visibility = View.VISIBLE
                adapter.submitList(state.orders)
            }
            is OrderUiState.Error -> {
                binding.errorView.visibility = View.VISIBLE
                binding.errorTextView.text = state.message
            }
            is OrderUiState.Empty -> {
                binding.emptyView.visibility = View.VISIBLE
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Überprüfe alle erforderlichen Berechtigungen
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        // Wenn Berechtigungen fehlen, frage sie an
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // SettingsActivity mit ActivityResultLauncher starten
                val intent = Intent(this, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}