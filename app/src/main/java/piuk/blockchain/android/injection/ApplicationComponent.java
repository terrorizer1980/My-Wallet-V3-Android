package piuk.blockchain.android.injection;

import dagger.Component;
import info.blockchain.wallet.util.PrivateKeyFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class,
        ServiceModule.class,
        ContextModule.class,
        KycModule.class,
        ContextModule.class,
})
public interface ApplicationComponent {

    // Subcomponent with its own scope (technically unscoped now that we're not deliberately
    // destroying a module between pages)
    PresenterComponent presenterComponent();

    void inject(PrivateKeyFactory privateKeyFactory);
}
