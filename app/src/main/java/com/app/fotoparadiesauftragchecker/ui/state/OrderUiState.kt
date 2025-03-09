package com.app.fotoparadiesauftragchecker.ui.state

import com.app.fotoparadiesauftragchecker.data.OrderStatus

/**
 * Sealed Class zur Repräsentation aller möglichen UI-Zustände für Orders.
 * Verbessert das UI-State-Management und macht die Zustandsübergänge explizit.
 */
sealed class OrderUiState {
    /**
     * Zeigt an, dass Daten geladen werden.
     */
    object Loading : OrderUiState()

    /**
     * Zeigt an, dass Daten erfolgreich geladen wurden.
     *
     * @param orders Liste der geladenen Aufträge
     */
    data class Success(val orders: List<OrderStatus>) : OrderUiState()

    /**
     * Zeigt an, dass ein Fehler aufgetreten ist.
     *
     * @param message Fehlermeldung
     * @param exception Optionale Exception für detailliertere Logs
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : OrderUiState()

    /**
     * Zeigt an, dass keine Daten vorhanden sind.
     */
    object Empty : OrderUiState()
}
