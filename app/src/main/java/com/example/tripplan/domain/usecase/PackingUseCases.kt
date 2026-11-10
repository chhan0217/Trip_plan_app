package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.PackingItem
import com.example.tripplan.domain.model.PackingItemInput
import com.example.tripplan.domain.repository.PackingRepository
import com.example.tripplan.domain.rule.PackingItemValidationError
import com.example.tripplan.domain.rule.PackingItemValidator
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObservePackingItemsUseCase @Inject constructor(private val repository: PackingRepository) {
    operator fun invoke(tripPlanId: Long): Flow<List<PackingItem>> = repository.observeItems(tripPlanId)
}

class CreatePackingItemUseCase @Inject constructor(private val repository: PackingRepository) {
    suspend operator fun invoke(tripPlanId: Long, input: PackingItemInput): PackingMutationResult {
        val errors = PackingItemValidator.validate(input)
        if (errors.isNotEmpty()) return PackingMutationResult.Invalid(errors)
        val order = repository.observeItems(tripPlanId).first()
            .filter { it.type == input.type }.maxOfOrNull { it.order }?.plus(1) ?: 0
        val id = repository.createItem(
            PackingItem(
                id = 0,
                tripPlanId = tripPlanId,
                name = input.name.trim(),
                category = input.category.trim(),
                quantity = input.quantity,
                memo = input.memo.trim(),
                type = input.type,
                checkedAt = null,
                order = order,
            )
        )
        return PackingMutationResult.Success(id)
    }
}

class UpdatePackingItemUseCase @Inject constructor(private val repository: PackingRepository) {
    suspend operator fun invoke(itemId: Long, input: PackingItemInput): PackingMutationResult {
        val errors = PackingItemValidator.validate(input)
        if (errors.isNotEmpty()) return PackingMutationResult.Invalid(errors)
        val old = repository.getItem(itemId) ?: return PackingMutationResult.NotFound
        if (old.type != input.type) return PackingMutationResult.TypeCannotChange
        repository.updateItem(old.copy(
            name = input.name.trim(), category = input.category.trim(), quantity = input.quantity,
            memo = input.memo.trim(),
        ))
        return PackingMutationResult.Success(itemId)
    }
}

class TogglePackingItemUseCase @Inject constructor(
    private val repository: PackingRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(itemId: Long): PackingMutationResult {
        val item = repository.getItem(itemId) ?: return PackingMutationResult.NotFound
        repository.updateItem(item.copy(checkedAt = if (item.checkedAt == null) clock.instant() else null))
        return PackingMutationResult.Success(itemId)
    }
}

class DeletePackingItemUseCase @Inject constructor(private val repository: PackingRepository) {
    suspend operator fun invoke(itemId: Long): PackingMutationResult {
        val item = repository.getItem(itemId) ?: return PackingMutationResult.NotFound
        repository.deleteItem(itemId)
        val remaining = repository.observeItems(item.tripPlanId).first().filter { it.type == item.type }
            .sortedWith(compareBy<PackingItem> { it.order }.thenBy { it.id })
            .mapIndexed { index, value -> value.copy(order = index) }
        repository.updateItems(remaining)
        return PackingMutationResult.Success(itemId)
    }
}

class MovePackingItemUseCase @Inject constructor(private val repository: PackingRepository) {
    suspend operator fun invoke(itemId: Long, direction: PackingMoveDirection): PackingMutationResult {
        val item = repository.getItem(itemId) ?: return PackingMutationResult.NotFound
        val items = repository.observeItems(item.tripPlanId).first().filter { it.type == item.type }
            .sortedWith(compareBy<PackingItem> { it.order }.thenBy { it.id })
        val index = items.indexOfFirst { it.id == itemId }
        val target = if (direction == PackingMoveDirection.Up) index - 1 else index + 1
        if (index < 0 || target !in items.indices) return PackingMutationResult.Success(itemId)
        val mutable = items.toMutableList()
        val moved = mutable.removeAt(index)
        mutable.add(target, moved)
        repository.updateItems(mutable.mapIndexed { order, value -> value.copy(order = order) })
        return PackingMutationResult.Success(itemId)
    }
}

enum class PackingMoveDirection { Up, Down }

sealed interface PackingMutationResult {
    data class Success(val itemId: Long) : PackingMutationResult
    data class Invalid(val errors: Set<PackingItemValidationError>) : PackingMutationResult
    data object TypeCannotChange : PackingMutationResult
    data object NotFound : PackingMutationResult
}
