package com.example.tripplan.presentation.packing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.PackingItemInput
import com.example.tripplan.domain.model.PackingItemType
import com.example.tripplan.domain.usecase.CreatePackingItemUseCase
import com.example.tripplan.domain.usecase.DeletePackingItemUseCase
import com.example.tripplan.domain.usecase.ObservePackingItemsUseCase
import com.example.tripplan.domain.usecase.TogglePackingItemUseCase
import com.example.tripplan.domain.usecase.UpdatePackingItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class PackingViewModel @Inject constructor(
    private val observeItems: ObservePackingItemsUseCase,
    private val createItem: CreatePackingItemUseCase,
    private val toggleItem: TogglePackingItemUseCase,
    private val deleteItem: DeletePackingItemUseCase,
    private val updateItem: UpdatePackingItemUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PackingUiState())
    val uiState: StateFlow<PackingUiState> = _uiState.asStateFlow()
    private var loadedId: Long? = null

    fun load(tripPlanId: Long) {
        if (loadedId == tripPlanId) return
        loadedId = tripPlanId
        viewModelScope.launch {
            observeItems(tripPlanId).catch {
                _uiState.value = PackingUiState(isLoading = false, isLoadFailed = true)
            }.collect { items ->
                _uiState.value = PackingUiState(
                    isLoading = false,
                    sharedItems = items.filter { it.type == PackingItemType.Shared }.map {
                        PackingItemUiModel(it.id, it.name, it.quantity, it.checkedAt != null)
                    },
                    personalItems = items.filter { it.type == PackingItemType.Personal }.map {
                        PackingItemUiModel(it.id, it.name, it.quantity, it.checkedAt != null)
                    },
                )
            }
        }
    }

    fun add(name: String, type: PackingItemType) {
        val tripPlanId = loadedId ?: return
        viewModelScope.launch { createItem(tripPlanId, PackingItemInput(name = name, type = type)) }
    }
    fun save(name: String, type: PackingItemType, editingId: Long?) {
        val tripPlanId = loadedId ?: return
        viewModelScope.launch {
            if (editingId == null) createItem(tripPlanId, PackingItemInput(name = name, type = type))
            else updateItem(editingId, PackingItemInput(name = name, type = type))
        }
    }
    fun toggle(itemId: Long) { viewModelScope.launch { toggleItem(itemId) } }
    fun delete(itemId: Long) { viewModelScope.launch { deleteItem(itemId) } }
}

data class PackingUiState(
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val sharedItems: List<PackingItemUiModel> = emptyList(),
    val personalItems: List<PackingItemUiModel> = emptyList(),
)

data class PackingItemUiModel(val id: Long, val name: String, val quantity: Int, val checked: Boolean)
