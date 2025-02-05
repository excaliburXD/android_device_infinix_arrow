package org.lineageos.infinix.imsinit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsManager;

public class PhoneStateService extends Service {
    private static final String LOG_TAG = "ImsInit";
    private static ServiceState sLastState = null;

    private void handleServiceStateChanged(ServiceState serviceState) {
        Log.i(LOG_TAG, "handleServiceStateChanged");
        if (((sLastState == null) ? true : (sLastState.getDataRegState() != ServiceState.STATE_IN_SERVICE))
                && ((serviceState == null) ? false : (serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE))) {
            SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);

            if (prefs.getBoolean("first_time", true)) {
                Log.i(LOG_TAG, "Skipping first boot");
                prefs.edit().putBoolean("first_time", false).commit();
                return;
            }

            if (ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this)) {
                Log.i(LOG_TAG, "VoLTE enabled, trying to toggle it off and back on");
                ImsManager.setEnhanced4gLteModeSetting(this, false);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        // Ignore
                    }
                    ImsManager.setEnhanced4gLteModeSetting(this, true);
                    Log.i(LOG_TAG, "Waiting for 20 seconds after that clear data of mediatek ims");
                        try {
                            // wait for 20 seconds
                            Thread.sleep(20000);
                            // clearing mediatek ims app data
                            Log.i(LOG_TAG, "Clearing data of mediatek ims app");
                            Runtime runtime = Runtime.getRuntime();
                            runtime.exec("pm clear com.mediatek.ims");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }).start();
            }
        }

        sLastState = serviceState;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // Handle latest state at start
        handleServiceStateChanged(tm.getServiceState());
        tm.listen(new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                handleServiceStateChanged(serviceState);
            }
        }, PhoneStateListener.LISTEN_SERVICE_STATE);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

    }
}
