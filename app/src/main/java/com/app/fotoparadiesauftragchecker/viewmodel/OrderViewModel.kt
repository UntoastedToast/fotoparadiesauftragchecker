package com.app.fotoparadiesauftragchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import com.app.fotoparadiesauftragchecker.repository.OrderRepository
import com.app.fotoparadiesauftragchecker.repository.RepositoryProvider
import com.app.fotoparadiesauftragchecker.ui.state.OrderUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableLiveData<OrderUiState>(OrderUiState.Loading)
    val uiState: LiveData<OrderUiState> = _uiState

    /**
     * Ereignis-basierte Kommunikation für einmalige Events wie Snackbar-Nachrichten.
     * Wird nach dem Beobachten zurückgesetzt, um mehrfache Verarbeitung zu vermeiden.
     */
    private val _event = MutableLiveData<Event<String>>()
    val event: LiveData<Event<String>> = _event

    private val repository: OrderRepository = RepositoryProvider.getOrderRepository(application)
    private val notificationService = NotificationService(application)

    init {
        // Aufträge laden
        loadOrders()
    }

    /**
     * Lädt alle gespeicherten Aufträge und aktualisiert den UI-State.
     */
    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.postValue(OrderUiState.Loading)
            
            try {
                repository.getAllOrders()
                    .catch { e -> 
                        _uiState.postValue(OrderUiState.Error(
                            e.message ?: "Ein Fehler ist aufgetreten",
                            e
                        ))
                    }
                    .collectLatest { ordersList ->
                        if (ordersList.isEmpty()) {
                            _uiState.postValue(OrderUiState.Empty)
                        } else {
                            _uiState.postValue(OrderUiState.Success(ordersList))
                        }
                    }
            } catch (e: Exception) {
                _uiState.postValue(OrderUiState.Error(
                    e.message ?: "Ein Fehler ist aufgetreten", 
                    e
                ))
            }
        }
    }

    /**
     * Fügt einen neuen Auftrag hinzu.
     */
    fun addOrder(shop: Int, order: Int, name: String? = null) {
        viewModelScope.launch {
            try {
                // Aktuellen UI-State sichern
                val currentState = _uiState.value

                // Lade-Status setzen
                _uiState.postValue(OrderUiState.Loading)
                
                val newOrderStatus = repository.addOrder(shop, order, name)
                
                // UI aktualisieren, wenn der Auftrag hinzugefügt wurde
                if (newOrderStatus != null) {
                    // Bei Erfolg die aktuelle Liste mit dem neuen Auftrag aktualisieren
                    if (currentState is OrderUiState.Success) {
                        val updatedList = currentState.orders.toMutableList()
                        updatedList.add(0, newOrderStatus) // Neuen Auftrag an Position 0 hinzufügen
                        _uiState.postValue(OrderUiState.Success(updatedList))
                    } else {
                        // Falls die Liste vorher leer war oder Fehler aufgetreten ist
                        _uiState.postValue(OrderUiState.Success(listOf(newOrderStatus)))
                    }
                    
                    _event.postValue(Event("Auftrag hinzugefügt: $order"))
                } else {
                    // Bei Fehler zum vorherigen Zustand zurückkehren und Fehler anzeigen
                    currentState?.let { _uiState.postValue(it) }
                    _event.postValue(Event("Fehler beim Hinzufügen des Auftrags"))
                }
            } catch (e: Exception) {
                _uiState.postValue(OrderUiState.Error(
                    e.message ?: "Ein Fehler ist aufgetreten", 
                    e
                ))
            }
        }
    }

    /**
     * Aktualisiert alle Aufträge und prüft auf neue Benachrichtigungen.
     */
    fun refreshOrders() {
        viewModelScope.launch {
            // Wir zeigen keinen Loading-Zustand beim Refresh, da wir den SwipeRefreshLayout-Indikator haben
            try {
                val updatedOrders = repository.refreshOrders()
                
                if (updatedOrders.isEmpty()) {
                    _uiState.postValue(OrderUiState.Empty)
                } else {
                    _uiState.postValue(OrderUiState.Success(updatedOrders))
                }
                
                // Prüfe auf abholbereite Aufträge und sende Benachrichtigungen
                checkForReadyOrders(updatedOrders)
                
                _event.postValue(Event("Aufträge aktualisiert"))
            } catch (e: Exception) {
                _event.postValue(Event(e.message ?: "Fehler beim Aktualisieren"))
                
                // Beim Refresh nicht den UI-Zustand auf Error setzen,
                // sondern nur eine Nachricht zeigen und den aktuellen Zustand beibehalten
                if (_uiState.value !is OrderUiState.Success) {
                    _uiState.postValue(OrderUiState.Error(
                        e.message ?: "Ein Fehler ist aufgetreten",
                        e
                    ))
                }
            }
        }
    }

    /**
     * Löscht einen Auftrag.
     */
    fun deleteOrder(order: OrderStatus) {
        viewModelScope.launch {
            try {
                // Aktuellen Zustand sichern für mögliche Wiederherstellung
                val currentState = _uiState.value
                
                // Aus Repository löschen
                repository.deleteOrder(order)
                
                // Aus UI entfernen, falls wir im Success-Zustand sind
                if (currentState is OrderUiState.Success) {
                    val updatedList = currentState.orders.toMutableList()
                    updatedList.remove(order)
                    
                    if (updatedList.isEmpty()) {
                        _uiState.postValue(OrderUiState.Empty)
                    } else {
                        _uiState.postValue(OrderUiState.Success(updatedList))
                    }
                    
                    _event.postValue(Event("Auftrag gelöscht: ${order.orderNumber}"))
                }
            } catch (e: Exception) {
                _event.postValue(Event("Fehler beim Löschen: ${e.message}"))
            }
        }
    }

    /**
     * Stellt einen gelöschten Auftrag wieder her.
     */
    fun restoreOrder(order: OrderStatus) {
        viewModelScope.launch {
            try {
                addOrder(
                    order.retailerId.toInt(),
                    order.orderNumber,
                    order.orderName
                )
            } catch (e: Exception) {
                _event.postValue(Event("Fehler beim Wiederherstellen: ${e.message}"))
            }
        }
    }

    /**
     * Hilfsmethode zum Prüfen, ob Aufträge abholbereit sind und Benachrichtigungen gesendet werden sollten.
     */
    private suspend fun checkForReadyOrders(orders: List<OrderStatus>) {
        orders.forEach { status ->
            val existingOrder = repository.getOrderById(status.orderNumber.toString())
            
            // Prüfen, ob eine Benachrichtigung gesendet werden sollte
            val shouldNotify = existingOrder?.let {
                !it.notificationSent && 
                it.status != "DELIVERED" && 
                status.status == "DELIVERED"
            } ?: false

            if (shouldNotify) {
                // Status in Datenbank aktualisieren
                repository.updateOrderStatus(
                    orderId = status.orderNumber.toString(),
                    status = status.status,
                    notificationSent = true
                )
                
                // Benachrichtigung senden
                notificationService.showOrderReadyNotification(
                    status.orderNumber.toString(),
                    status.retailerId
                )
            }
        }
    }
    
    /**
     * Helferklasse für einmalige Events, die nicht mehrfach konsumiert werden sollten.
     */
    class Event<out T>(private val content: T) {
        private var hasBeenHandled = false
        
        /**
         * Gibt den Inhalt zurück und markiert dieses Event als behandelt.
         * Nachfolgende Aufrufe geben null zurück.
         */
        fun getContentIfNotHandled(): T? {
            return if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }
        }
    }
}