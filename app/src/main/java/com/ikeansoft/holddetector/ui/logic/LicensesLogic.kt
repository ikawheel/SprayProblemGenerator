package com.example.holddetector.ui

internal fun buildLicensesScreenState(
    state: MainUiState,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.LICENSES,
        screenBackStack = pushedScreenBackStack(state, AppScreen.LICENSES),
        message = null
    )
}
