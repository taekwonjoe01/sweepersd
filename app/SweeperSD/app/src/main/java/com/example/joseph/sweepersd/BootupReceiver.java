package com.example.joseph.sweepersd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootupReceiver extends BroadcastReceiver {
    public BootupReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, SweeperService.class);
        context.startService(serviceIntent);
    }
}
