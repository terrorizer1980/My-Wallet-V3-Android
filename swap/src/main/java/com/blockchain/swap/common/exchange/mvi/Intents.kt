package com.blockchain.swap.common.exchange.mvi

import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.Quote
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

/**
 * The intents represent user/system actions
 */
sealed class ExchangeIntent

data class SimpleFieldUpdateIntent(val userValue: BigDecimal, val decimalCursor: Int = 0) : ExchangeIntent()

class SwapIntent : ExchangeIntent()

class QuoteIntent(val quote: Quote) : ExchangeIntent()

class SetFixIntent(val fix: Fix) : ExchangeIntent()

class ToggleFiatCryptoIntent : ExchangeIntent()

class ToggleFromToIntent : ExchangeIntent()

class ChangeCryptoFromAccount(val from: AccountReference) : ExchangeIntent()

class ChangeCryptoToAccount(val to: AccountReference) : ExchangeIntent()

fun Quote.toIntent(): ExchangeIntent = QuoteIntent(this)

class SetTradeLimits(val min: FiatValue, val max: FiatValue) : ExchangeIntent()

class SetUserTier(val tier: Int) : ExchangeIntent()

class IsUserEligiableForFreeEthIntent(val isEligiable: Boolean) : ExchangeIntent()

class ExchangeRateIntent(val prices: List<ExchangeRate.CryptoToFiat>) : ExchangeIntent()

class SetTierLimit(val availableOnTier: FiatValue) : ExchangeIntent()

class ApplyMinimumLimit : ExchangeIntent()

class EnoughFeesLimit(val hasEnoughForFess: Boolean) : ExchangeIntent()

object ApplyMaximumLimit : ExchangeIntent()

class FiatExchangeRateIntent(val c2fRate: ExchangeRate.CryptoToFiat) : ExchangeIntent()

class SpendableValueIntent(val cryptoValue: CryptoValue) : ExchangeIntent()

object ClearQuoteIntent : ExchangeIntent()

object ApplyMaxSpendable : ExchangeIntent()

class LockQuoteIntent(val lockQuote: Boolean) : ExchangeIntent()

class SetEthTransactionInFlight(val ethInFlight: Boolean) : ExchangeIntent()
