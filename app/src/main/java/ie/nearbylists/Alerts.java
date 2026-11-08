package ie.nearbylists;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** Builds the notification that shows the list. */
final class Alerts {

    private static final String CHANNEL = "nearby";
    private static final long QUIET_PERIOD_MS = 2L * 60L * 60L * 1000L;

    private Alerts() {
    }

    static void show(Context context, String placeId, boolean test) {
        List<Store.Place> places = Store.load(context);
        Store.Place place = Store.find(places, placeId);
        if (place == null) {
            return;
        }
        List<String> outstanding = place.outstanding();
        long now = System.currentTimeMillis();
        if (!test && (outstanding.isEmpty() || now - place.lastAlert < QUIET_PERIOD_MS)) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Near a place on your lists", NotificationManager.IMPORTANCE_HIGH));

        Intent open = new Intent(context, PlaceActivity.class)
                .putExtra(PlaceActivity.EXTRA_ID, place.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, place.id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String summary = outstanding.isEmpty()
                ? "Nothing left on this list"
                : outstanding.size() + (outstanding.size() == 1 ? " thing to get" : " things to get");
        String body = outstanding.isEmpty() ? summary : String.join("\n", outstanding);

        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("Near " + place.name)
                .setContentText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        manager.notify(place.id.hashCode(), notification);

        if (!test) {
            place.lastAlert = now;
            Store.save(context, places);
        }
    }
}
