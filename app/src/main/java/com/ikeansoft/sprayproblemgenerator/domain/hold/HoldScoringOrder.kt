package com.ikeansoft.sprayproblemgenerator.domain.hold

import com.ikeansoft.sprayproblemgenerator.model.Hold

fun buildHoldScoringOrder(holds: List<Hold>): List<Int> {
    return holds.indices.sortedWith(
        compareBy<Int> { index -> holds[index].centerY }
            .thenBy { index -> holds[index].centerX }
    )
}
