package xyz.gianlu.librespot.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootDeviceReceiver extends BroadcastReceiver {
    private static final String TAG_BOOT_BROADCAST_RECEIVER = "BOOT_BROADCAST_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if(Intent.ACTION_BOOT_COMPLETED.equals(action))
        {
            //startServiceDirectly(context);
        }

        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}