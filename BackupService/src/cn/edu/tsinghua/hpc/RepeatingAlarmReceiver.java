package cn.edu.tsinghua.hpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RepeatingAlarmReceiver extends BroadcastReceiver {
	public static final String TAG = "RepeatingAlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "on alarm");
		Intent startServiceIntent = new Intent(context, BackupService.class);
		context.startService(startServiceIntent);
	}

}
