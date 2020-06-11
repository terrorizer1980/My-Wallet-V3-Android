package com.blockchain.swap.nabu.datamanagers.featureflags

interface ElegibilityInterface {
    fun isElegibleForCall() : Boolean
}