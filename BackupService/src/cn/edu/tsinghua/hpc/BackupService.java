
package cn.edu.tsinghua.hpc;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import cn.edu.tsinghua.hpc.ca.*;
import java.io.FileOutputStream;

public class BackupService extends Service {

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    private static boolean preferences_restored = false;

    private void backupSettings() {
        try {
            JSONObject json = new JSONObject();
            json.put(Settings.System.RINGTONE, Settings.System.getString(getContentResolver(),
                    Settings.System.RINGTONE));
            json.put(Settings.System.AUTO_TIME, Settings.System.getInt(getContentResolver(),
                    Settings.System.AUTO_TIME));
            BitmapDrawable wallpaper = (BitmapDrawable) getWallpaper();
            FileOutputStream fos = null;
            wallpaper.getBitmap().compress(CompressFormat.PNG, 100, fos);

        } catch (SettingNotFoundException e) {
        } catch (JSONException e) {
        }
    }

    private void restoreSettings(JSONObject json) {
        try {
            Settings.System.putString(getContentResolver(), Settings.System.RINGTONE, json
                    .getString(Settings.System.RINGTONE));
            Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME, json
                    .getInt(Settings.System.RINGTONE));
            preferences_restored = true;
        } catch (JSONException e) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            CAUtils mUtils = CAUtils.getInstance(this);

            LoginView loginViewForCPK = mUtils.requestLoginForCPK();
            String uid = loginViewForCPK.getUID();
            String signature = new String(Base64.encode(loginViewForCPK.getSignature()));//
            String result = mUtils.verifySignForCPK(this, CAUtils.ip, CAUtils.port, uid, signature);
            if (result != null && result.equals("0")) {

            }


        } catch (Exception e1) {
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                // Phase 1: retrieve the imsi info
                while (manager.getSubscriberId() == null) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
                while (true) {
                    if (!preferences_restored) {

                    } else {

                    }
                }

            }
        }).start();
    }

    @Override
    public void onStart(Intent intent, int startId) {

    }

    @Override
    public void onDestroy() {

    }

}
