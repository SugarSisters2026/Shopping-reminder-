package ie.nearbylists;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/** Android calls this when the phone comes within range of a saved place. */
public class GeofenceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()
                || event.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return;
        }
        List<Geofence> triggered = event.getTriggeringGeofences();
        if (triggered == null) {
            return;
        }
        for (Geofence fence : triggered) {
            Alerts.show(context, fence.getRequestId(), false);
        }
    }
}
