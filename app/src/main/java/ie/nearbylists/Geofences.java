package ie.nearbylists;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/** Tells Android which places to watch. Android allows 100 per app. */
final class Geofences {

    private static final int MAX_PLACES = 100;

    private Geofences() {
    }

    static boolean allowed(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    static void sync(Context context, Runnable done) {
        final Context app = context.getApplicationContext();
        if (!allowed(app)) {
            finish(done);
            return;
        }
        final List<Geofence> fences = new ArrayList<>();
        for (Store.Place place : Store.load(app)) {
            if (fences.size() == MAX_PLACES) {
                break;
            }
            fences.add(new Geofence.Builder()
                    .setRequestId(place.id)
                    .setCircularRegion(place.lat, place.lng, place.radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setNotificationResponsiveness(60000)
                    .build());
        }

        final GeofencingClient client = LocationServices.getGeofencingClient(app);
        final PendingIntent pending = pendingIntent(app);
        client.removeGeofences(pending).addOnCompleteListener(removed -> {
            if (fences.isEmpty()) {
                finish(done);
                return;
            }
            GeofencingRequest request = new GeofencingRequest.Builder()
                    .setInitialTrigger(0)
                    .addGeofences(fences)
                    .build();
            client.addGeofences(request, pending).addOnCompleteListener(added -> finish(done));
        });
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private static void finish(Runnable done) {
        if (done != null) {
            done.run();
        }
    }
}
