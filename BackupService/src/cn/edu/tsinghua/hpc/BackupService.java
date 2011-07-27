
package cn.edu.tsinghua.hpc;

import cn.edu.tsinghua.hpc.ca.CAUtils;

import com.ccit.phone.LoginView;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BackupService extends Service {

    private String uid;

    private String token;

    ConnectionListener listener;

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    private static boolean preferences_restored = false;

    private void backupSettings() {
        try {
            JSONObject json = new JSONObject();
            json.put(Settings.System.RINGTONE,
                    Settings.System.getString(getContentResolver(), Settings.System.RINGTONE));
            json.put(Settings.System.AUTO_TIME,
                    Settings.System.getInt(getContentResolver(), Settings.System.AUTO_TIME));
            CloudStorage.putFile("settings", uid, token, new ByteArrayInputStream(json.toString()
                    .getBytes()), json.toString().getBytes().length);
            BitmapDrawable wallpaper = (BitmapDrawable) getWallpaper();
            Parcel p = Parcel.obtain();
            wallpaper.getBitmap().writeToParcel(p, 0);
            CloudStorage.putFile("wallpaper", uid, token,
                    new ByteArrayInputStream(p.createByteArray()), p.dataSize());
        } catch (SettingNotFoundException e) {
        } catch (JSONException e) {
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    private void restoreSettings() {
        try {
            JSONObject json = new JSONObject(convertStreamToString(CloudStorage.downloadFile(
                    "settings", uid, token)));
            Settings.System.putString(getContentResolver(), Settings.System.RINGTONE,
                    json.getString(Settings.System.RINGTONE));
            Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,
                    json.getInt(Settings.System.RINGTONE));
            BackupService.this.setWallpaper(CloudStorage.downloadFile("wallpaper", uid, token));
            preferences_restored = true;
        } catch (JSONException e) {
        } catch (IOException e) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listener = new ConnectionListener(BackupService.this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        while (true) {
            if (listener.isConnected()) {
                try {
                    CAUtils mUtils = CAUtils.getInstance(this);
                    LoginView loginViewForCPK = mUtils.requestLoginForCPK();
                    uid = loginViewForCPK.getUID();
                    token = new String(Base64.encode(loginViewForCPK.getSignature()));
                    String result = mUtils.verifySignForCPK(this, CAUtils.ip, CAUtils.port, uid,
                            token);
                    if (result != null && result.equals("0")) {
                    }
                } catch (Exception e) {
                    return;
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
                                restoreSettings();
                            } else {
                                backupSettings();
                            }
                        }

                    }
                }).start();
                break;
            }
        }
    }

    @Override
    public void onDestroy() {

    }

}
