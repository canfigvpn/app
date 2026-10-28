package ir.ef3.carcare;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String ACTION_REMINDER = "ir.ef3.carcare.DAILY_REMINDER";
    private static final int REQUEST_CODE = 206;

    public static void schedule(Context context, String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) time = "09:00";
        String[] parts = time.split(":");
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        next.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        PendingIntent pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
    }

    public static void cancel(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        PendingIntent pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.cancel(pending);
        pending.cancel();
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("car_care", Context.MODE_PRIVATE);
            if (prefs.getBoolean("reminder_enabled", false)) schedule(context, prefs.getString("reminder_time", "09:00"));
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences("car_care", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("reminder_enabled", false)) return;
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            schedule(context, prefs.getString("reminder_time", "09:00"));
            return;
        }
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 207, open,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        String details = prefs.getString("reminder_details", "");
        String defaultText = "موعد سرویس‌ها و کیلومتر خودروی پژو ۲۰۶ را بررسی کن.";
        String contentText = details == null || details.trim().isEmpty() ? defaultText : details;
        if (contentText.length() > 180) contentText = contentText.substring(0, 177) + "...";
        Notification notification = new NotificationCompat.Builder(context, "car_care_reminders")
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("یادآوری خودرو من")
            .setContentText(contentText)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(content).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(206, notification);
        schedule(context, prefs.getString("reminder_time", "09:00"));
    }
}