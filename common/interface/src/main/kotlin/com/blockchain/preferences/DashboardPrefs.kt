package com.blockchain.preferences

interface DashboardPrefs {
    var swapIntroCompleted: Boolean
    var isOnboardingComplete: Boolean
    var isCustodialIntroSeen: Boolean

    val isTourComplete: Boolean
    val tourStage: String

    fun setTourComplete()
    fun setTourStage(stageName: String)
    fun resetTour()

    fun hasSentMetric(metricName: String): Boolean
    fun setMetricAsSent(metricName: String)
}