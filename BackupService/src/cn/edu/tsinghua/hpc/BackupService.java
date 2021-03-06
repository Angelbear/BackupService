
package cn.edu.tsinghua.hpc;

import cn.edu.tsinghua.hpc.ca.CAUtils;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimeZone;

public class BackupService extends Service {

    private String uid;

    private String token;

    ConnectionListener listener;

    private static boolean firstStart = true;

    private static final String TAG = "BackupService";

    public interface IMyService {
        public void backup();
    }

    private MyServiceBinder myServiceBinder = new MyServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return myServiceBinder;
    }

    public class MyServiceBinder extends Binder implements IMyService {
        public void backup() {
            if (!preferences_restored) {
                restoreSettings();
            } else {
                backupSettings();
            }
        }
    }

    private static boolean preferences_restored = false;

    private void backupSettings() {
        Log.d(TAG, "begin backup");
        try {
            JSONObject json = new JSONObject();
            json.put(Settings.System.RINGTONE,
                    Settings.System.getString(getContentResolver(), Settings.System.RINGTONE));
            json.put(Settings.System.AUTO_TIME,
                    Settings.System.getInt(getContentResolver(), Settings.System.AUTO_TIME));
            json.put(Settings.System.NOTIFICATION_SOUND, Settings.System.getString(
                    getContentResolver(), Settings.System.NOTIFICATION_SOUND));
            json.put("TimeZone", TimeZone.getDefault().getID());

            if (CloudStorage.getInstance().deleteFile("settings", uid, token)) {
                Log.d(TAG, "delete settings success");
            }
            Log.d(TAG, "Settings are " + json.toString());
            if (CloudStorage.getInstance().putFile("settings", uid, token,
                    new ByteArrayInputStream(json.toString().getBytes()),
                    json.toString().getBytes().length)) {
                Log.d(TAG, "upload settings success");
            }
            BitmapDrawable wallpaper = (BitmapDrawable) getWallpaper();
            FileOutputStream fos;
            try {
                fos = this.openFileOutput("wallpaper", MODE_PRIVATE);
                wallpaper.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
            
            //if (CloudStorage.getInstance().deleteFile("wallpaper", uid, token)) {
            //    Log.d(TAG, "delete wallpaper success");
            //}

            if (CloudStorage.getInstance()
                    .putFile(this.getFileStreamPath("wallpaper").getAbsolutePath(), "wallpaper",
                            uid, token/*
                                       * , this.openFileInput("wallpaper") ,
                                       * this.getFileStreamPath(
                                       * "wallpaper").length()
                                       */)) {
                Log.d(TAG, "upload wallpaper success");
            }
            this.getFileStreamPath("wallpaper").delete();
            Log.d(TAG, "finsh backup");
        } catch (SettingNotFoundException e) {
        } catch (JSONException e) {
        }
    }

    private static String convertStreamToString(InputStream is) throws NullPointerException {
        if (is == null)
            throw new NullPointerException();
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
        Log.d(TAG, "begin restore");
        try {
            //startActivity(new Intent(this, DialogActivity.class)
            //        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            String content = convertStreamToString(CloudStorage.getInstance().downloadFile(
                    "settings", uid, token));
            Log.d(TAG, "Settings are " + content);
            JSONObject json = new JSONObject(content);
            Log.d(TAG, "Settings are " + json.toString());
            Settings.System.putString(getContentResolver(), Settings.System.RINGTONE,
                    json.getString(Settings.System.RINGTONE));
            Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,
                    json.getInt(Settings.System.AUTO_TIME));
            Settings.System.putString(getContentResolver(), Settings.System.NOTIFICATION_SOUND,
                    json.getString(Settings.System.NOTIFICATION_SOUND));
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarm.setTimeZone(json.getString("TimeZone"));
            BackupService.this.setWallpaper(CloudStorage.getInstance().downloadFile("wallpaper",
                    uid, token));
            preferences_restored = true;
            Log.d(TAG, "finish restore");
        } catch (JSONException e) {
            preferences_restored = true;
        } catch (IOException e) {
        } catch (NullPointerException e) {

        }
        //sendBroadcast(new Intent("cn.edu.tsinghua.hpc.restore"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listener = new ConnectionListener(BackupService.this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (firstStart) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            getClass().getName());
                    wl.acquire();
                    while (true) {
                        if (listener.isConnected()) {
                            try {
                                CCITSC mCCIT = new CCITSC(BackupService.this, CAUtils.ip,
                                        CAUtils.port);
                                mCCIT.loginInit(false);
                                LoginView lv = mCCIT.requestLogin(false);
                                uid = lv.getUID();
                                token = new String(Base64.encode(lv.getSignature()));
                                Log.d(TAG, "uid is " + uid + " token is " + token);
                            } catch (Exception e) {
                                continue;
                            }

                            break;
                        }
                    }
                    Intent alarmIntent = new Intent(BackupService.this,
                            RepeatingAlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(BackupService.this, 0,
                            alarmIntent, 0);

                    Log.d(TAG, "startup alarm");
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                            + (5 * 1000), pendingIntent);
                    firstStart = false;
                    wl.release();
                }
            }).start();
        } else {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            getClass().getName());
                    wl.acquire();
                    if (!preferences_restored) {
                        restoreSettings();
                    } else {
                        backupSettings();
                    }
                    Intent alarmIntent = new Intent(BackupService.this,
                            RepeatingAlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(BackupService.this, 0,
                            alarmIntent, 0);
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                            + (60 * 1000), pendingIntent);
                    wl.release();
                }
            }).start();

        }
    }

    @Override
    public void onDestroy() {

    }

}
