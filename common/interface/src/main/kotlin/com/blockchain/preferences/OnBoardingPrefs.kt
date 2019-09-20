package com.blockchain.preferences

interface OnBoardingPrefs {
    var swapIntroCompleted: Boolean
    var isOnboardingComplete: Boolean

    val isTourComplete: Boolean
    val tourStage: String

    fun setTourComplete()
    fun setTourStage(stageName: String)
    fun resetTour()
}