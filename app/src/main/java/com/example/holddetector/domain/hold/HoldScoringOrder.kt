package com.example.holddetector.domain.hold

import com.example.holddetector.model.Hold

fun buildHoldScoringOrder(holds: List<Hold>): List<Int> {
    return holds.indices.sortedWith(
        compareBy<Int> { index -> holds[index].centerY }
            .thenBy { index -> holds[index].centerX }
    )
}
