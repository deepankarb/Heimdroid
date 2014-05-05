/**
 * 
 */
package com.heimcontrol.mobile;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author bharddee
 * 
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {

	private static final String TAG = "ConnectivityChangeReceiver";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,
				false)) {
			Log.i(TAG, "No connectivity!");
		} else {
			ConnectivityManager connMan = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo mobInfo = connMan
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifiInfo = connMan
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (mobInfo != null) {
				Log.i(TAG, "Mobile " + mobInfo.getState());
			}
			if (wifiInfo != null) {
				Log.i(TAG, "WiFi " + wifiInfo.getState());
			}
		}
	}

    public static void enable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public static void disable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
    }
    
}