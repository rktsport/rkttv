package au.rktsport.rkttv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Opens RKT TV when the TV / box is switched on (where the device allows it). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        try {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } catch (Exception ignored) { }
    }
}
