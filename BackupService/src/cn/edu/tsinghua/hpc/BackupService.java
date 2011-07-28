package cn.edu.tsinghua.hpc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimeZone;

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
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import cn.edu.tsinghua.hpc.ca.CAUtils;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

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
			json.put(Settings.System.RINGTONE, Settings.System.getString(
					getContentResolver(), Settings.System.RINGTONE));
			json.put(Settings.System.AUTO_TIME, Settings.System.getInt(
					getContentResolver(), Settings.System.AUTO_TIME));
			json.put(Settings.System.NOTIFICATION_SOUND, Settings.System
					.getString(getContentResolver(),
							Settings.System.NOTIFICATION_SOUND));
			json.put("TimeZone", TimeZone.getDefault().getDisplayName());

			Log.d(TAG, "Settings are " + json.toString());
			CloudStorage.getInstance().putFile("settings", uid, token,
					new ByteArrayInputStream(json.toString().getBytes()),
					json.toString().getBytes().length);
			BitmapDrawable wallpaper = (BitmapDrawable) getWallpaper();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			wallpaper.getBitmap()
					.compress(Bitmap.CompressFormat.PNG, 100, baos);
			CloudStorage.getInstance().putFile("wallpaper", uid, token,
					new ByteArrayInputStream(baos.toByteArray()), baos.size());
			Log.d(TAG, "finsh backup");
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
		Log.d(TAG, "begin restore");
		try {
			String content = convertStreamToString(CloudStorage.getInstance()
					.downloadFile("settings", uid, token));
			Log.d(TAG, "Settings are " + content);
			JSONObject json = new JSONObject(content);
			Log.d(TAG, "Settings are " + json.toString());
			Settings.System.putString(getContentResolver(),
					Settings.System.RINGTONE,
					json.getString(Settings.System.RINGTONE));
			Settings.System.putInt(getContentResolver(),
					Settings.System.AUTO_TIME,
					json.getInt(Settings.System.AUTO_TIME));
			Settings.System.putString(getContentResolver(),
					Settings.System.NOTIFICATION_SOUND,
					json.getString(Settings.System.NOTIFICATION_SOUND));
			AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarm.setTimeZone(json.getString("TimeZone"));
			BackupService.this.setWallpaper(CloudStorage.getInstance()
					.downloadFile("wallpaper", uid, token));
			preferences_restored = true;
			Log.d(TAG, "finish restore");
		} catch (JSONException e) {
			preferences_restored = true;
			Log.d(TAG, e.getLocalizedMessage());
		} catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
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
					while (true) {
						if (listener.isConnected()) {
							try {
								CCITSC mCCIT = new CCITSC(BackupService.this,
										CAUtils.ip, CAUtils.port);
								mCCIT.loginInit(false);
								LoginView lv = mCCIT.requestLogin(false);
								uid = lv.getUID();
								token = new String(Base64.encode(lv
										.getSignature()));
								Log.d(TAG, "uid is " + uid + " token is "
										+ token);
							} catch (Exception e) {
								Log.d(TAG, e.getLocalizedMessage());
							}

							break;
						}
					}
					Intent alarmIntent = new Intent(BackupService.this,
							RepeatingAlarmReceiver.class);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(
							BackupService.this, 0, alarmIntent, 0);

					Log.d(TAG, "startup alarm");
					AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
							System.currentTimeMillis() + (5 * 1000), 60 * 1000,
							pendingIntent);
					firstStart = false;
				}
			}).start();
		} else {
			new Thread(new Runnable() {

				@Override
				public void run() {
					if (!preferences_restored) {
						restoreSettings();
					} else {
						backupSettings();
					}
				}
			}).start();

		}
	}

	@Override
	public void onDestroy() {

	}

}
