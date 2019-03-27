package piuk.blockchain.androidcore.data.fees;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeLimits;
import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import org.web3j.tx.Transfer;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManagerKt;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.data.rxjava.RxPinning;
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager;

public class FeeDataManager {

    private final RxPinning rxPinning;
    private FeeApi feeApi;
    private EnvironmentConfig environmentSettings;

    public FeeDataManager(FeeApi feeApi, EnvironmentConfig environmentSettings, RxBus rxBus) {
        this.feeApi = feeApi;
        this.environmentSettings = environmentSettings;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            return Observable.just(createTestnetFeeOptions());
        } else {
            return rxPinning.call(() -> feeApi.getBtcFeeOptions())
                    .onErrorReturnItem(FeeOptions.defaultFee(CryptoCurrency.BTC))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            //No Test environment for Eth
            return Observable.just(createTestnetFeeOptions());
        } else {
            return rxPinning.call(() -> feeApi.getEthFeeOptions())
                    .onErrorReturnItem(FeeOptions.defaultFee(CryptoCurrency.ETHER))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    /**
     * Returns a {@link FeeOptions} object which contains a "regular" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBchFeeOptions() {
        return feeApi.getBchFeeOptions()
                .onErrorReturnItem(FeeOptions.defaultFee(CryptoCurrency.BCH));
    }

    /**
     * Returns a {@link FeeOptions} object for XLM fees.
     */
    public Observable<FeeOptions> getXlmFeeOptions() {
        return feeApi.getXlmFeeOptions()
                .onErrorReturnItem(FeeOptions.defaultFee(CryptoCurrency.XLM));
    }

    private FeeOptions createTestnetFeeOptions() {
        FeeOptions feeOptions = new FeeOptions();
        feeOptions.setRegularFee(1_000L);
        feeOptions.setPriorityFee(10_000L);
        feeOptions.setLimits(new FeeLimits(23, 23));
        feeOptions.setGasLimit(Transfer.GAS_LIMIT.longValue());
        return feeOptions;
    }
}
