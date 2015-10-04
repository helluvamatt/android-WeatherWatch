package com.schneenet.android.weatherwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Intent serviceIntent = new Intent(context, InformationService.class);
        context.startService(serviceIntent);
	}
}
