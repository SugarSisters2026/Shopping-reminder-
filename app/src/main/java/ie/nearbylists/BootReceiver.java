package ie.nearbylists;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Android forgets watched places on restart, so they are put back here. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult result = goAsync();
        Geofences.sync(context, result::finish);
    }
}
