package info.blockchain.wallet.prices;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.ApiCode;
import info.blockchain.wallet.prices.data.PriceDatum;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @see <a href=https://api.blockchain.info/price/specs>Blockchain Price API specs</a>
 */
public class PriceApi implements CurrentPriceApi {

    private final PriceEndpoints endpoints;
    private final ApiCode apiCode;

    public PriceApi(PriceEndpoints endpoints, ApiCode apiCode) {
        this.endpoints = endpoints;
        this.apiCode = apiCode;
    }

    /**
     * Returns a {@link List} of {@link PriceDatum} objects, containing a timestamp and a price for
     * that given time.
     *
     * @param cryptoCurrency  The cryptoCurrency cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param fiat The fiat currency in which to return the prices, eg "usd"
     * @param start The start time, in epoch seconds, from which to gather historic data
     * @param scale The scale which you want to use between price data, eg {@link TimeInterval#ONE_DAY}
     * @return An {@link Observable} wrapping a {@link List} of {@link PriceDatum} objects
     * @see TimeInterval
     */
    public Single<List<PriceDatum>> getHistoricPriceSeries(String cryptoCurrency,
                                                               String fiat,
                                                               long start,
                                                               int scale) {
        return endpoints.getHistoricPriceSeries(cryptoCurrency,
                fiat,
                start,
                scale,
                apiCode.getApiCode());
    }

    /**
     * Provides the exchange rate between a cryptocurrency and a fiat currency for this moment in
     * time. Returns a single {@link PriceDatum} object.
     *
     * @param cryptoCurrency  The cryptoCurrency cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param fiat The fiat currency in which to return the price, eg "usd"
     * @return An {@link Observable} wrapping a {@link PriceDatum} object
     */
    public Single<Double> getCurrentPrice(String cryptoCurrency, String fiat) {
        return getCurrentPriceDatum(cryptoCurrency, fiat)
                .map(new Function<PriceDatum, Double>() {
                    @Override
                    public Double apply(PriceDatum priceDatum) {
                        return priceDatum.getPrice();
                    }
                });
    }

    private Single<PriceDatum> getCurrentPriceDatum(String cryptoCurrency, String fiat) {
        return endpoints.getCurrentPrice(cryptoCurrency,
                fiat,
                apiCode.getApiCode());
    }

    /**
     * Provides the exchange rate between a cryptocurrency and a fiat currency for a given moment in
     * epochTime, supplied in seconds since epoch. Returns a single {@link PriceDatum} object.
     *
     * @param cryptoCurrency  The cryptoCurrency cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param fiat The fiat currency in which to return the price, eg "usd"
     * @param epochTime  The epochTime in seconds since epoch for which you want to return a price
     * @return An {@link Observable} wrapping a {@link PriceDatum} object
     */
    public Single<Double> getHistoricPrice(String cryptoCurrency, String fiat, long epochTime) {
        return endpoints.getHistoricPrice(cryptoCurrency,
                fiat,
                epochTime,
                apiCode.getApiCode())
                .map(new Function<PriceDatum, Double>() {
                    @Override
                    public Double apply(PriceDatum priceDatum) {
                        return priceDatum.getPrice();
                    }
                });
    }

    /**
     * Provides a {@link Map} of currency codes to current {@link PriceDatum} objects for a given
     * cryptoCurrency cryptocurrency. For instance, getting "USD" would return the current price, timestamp
     * and volume in an object. This is a direct replacement for the Ticker.
     *
     * @param cryptoCurrency The cryptoCurrency cryptocurrency that you want prices for, eg. ETH
     * @return A {@link Map} of {@link PriceDatum} objects.
     */
    public Single<Map<String, PriceDatum>> getPriceIndexes(String cryptoCurrency) {
        return endpoints.getPriceIndexes(cryptoCurrency, apiCode.getApiCode());
    }

    @NotNull
    @Override
    public Single<BigDecimal> currentPrice(@NotNull CryptoCurrency base, @NotNull String quoteFiatCode) {
        return getCurrentPriceDatum(base.getSymbol(), quoteFiatCode)
                .map(new Function<PriceDatum, BigDecimal>() {
                    @Override
                    public BigDecimal apply(PriceDatum priceDatum) {
                        return BigDecimal.valueOf(priceDatum.getPrice());
                    }
                });
    }
}
