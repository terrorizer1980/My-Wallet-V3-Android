package mobi.lab.veriff.sample.service;

import com.google.firebase.iid.FirebaseInstanceIdService;
import mobi.lab.veriff.service.FireBaseTokenUpdaterService;

public class SampleFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        FireBaseTokenUpdaterService.start(this, FireBaseTokenUpdaterService.ACTION_SEND_TOKEN_IF_NEEDED, null);
    }
}

