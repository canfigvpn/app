package ir.ef3.carcare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("car_care", Context.MODE_PRIVATE);
            if (prefs.getBoolean("reminder_enabled", false)) {
                ReminderReceiver.schedule(context, prefs.getString("reminder_time", "09:00"));
            }
        }
    }
}