package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.PackingItemInput

enum class PackingItemValidationError {
    BlankName,
    InvalidQuantity,
}

object PackingItemValidator {
    fun validate(input: PackingItemInput): Set<PackingItemValidationError> = buildSet {
        if (input.name.isBlank()) add(PackingItemValidationError.BlankName)
        if (input.quantity < 1) add(PackingItemValidationError.InvalidQuantity)
    }
}
