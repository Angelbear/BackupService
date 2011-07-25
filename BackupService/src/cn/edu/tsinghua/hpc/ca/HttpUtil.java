package cn.edu.tsinghua.hpc.ca;

import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;
/**
 * <p>
 * Title: HttpUtil
 * </p>
 *
 * <p>
 * Description: http请求处理类
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2011
 * </p>
 *
 * <p>
 * Company: ccit
 * </p>
 *
 * @author chensonglin
 * @date 2011.5.21
 * @version 1.0
 */
class HttpUtil {
	private static final int CONNECTION_TIMEOUT_INT = 30000; // Sets the timeout until
	private static final int SO_TIMEOUT_INT = 30000; // 响应Http时间30秒
	private static final String SERVER_CHARACTER_ENCODING_MODEL_STR = "GBK";// 服务器端数据的字符编码方式

	public static String sendGet(Context context,String url,Map<String, String> getMap) throws Exception{
		String strResBody = null;

		String param = getBody(getMap);
		String mUrl = url+"?"+param;

		HttpClient httpClient = null;
		HttpGet getRequest = null;
		try {
			// 设置连接超时时间和数据读取超时时间
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams,CONNECTION_TIMEOUT_INT);
			HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT_INT);
			// 新建HttpClient对象
			httpClient = new DefaultHttpClient(httpParams); //

			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); // wifi
			// 判断WiFi状态是否"已打开"
			if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
				Log.v("HttpUtil", "wifi没有打开*************************");
				Uri uri = Uri.parse("content://telephony/carriers/preferapn"); // 获取当前正在使用的APN接入点
				Cursor mCursor = context.getContentResolver().query(uri, null,
						null, null, null);
				if (mCursor != null) {
					mCursor.moveToNext(); // 游标移至第一条记录，当然也只有一条
					String proxyStr = mCursor.getString(mCursor
							.getColumnIndex("proxy")); // cmnet
					if (proxyStr != null && proxyStr.trim().length() > 0) {
						HttpHost proxy = new HttpHost(proxyStr, 80); // cmwap
						httpClient.getParams().setParameter(
								ConnRouteParams.DEFAULT_PROXY, proxy);
					}
				}else{
					Log.v("HttpUtil", "没有发现APN*************************");
				}
			}else{
				Log.v("HttpUtil", "使用wifi连接网络*************************");
			}
			// generate get request
			Log.v("HttpUtil", "发送get请求，url = " + mUrl);
			getRequest = new HttpGet(mUrl);

			// 获取服务器响应
			HttpResponse response = httpClient.execute(getRequest);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new Exception(response.getStatusLine().toString());
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					// 返回
					strResBody = EntityUtils.toString(entity,
							SERVER_CHARACTER_ENCODING_MODEL_STR);
				} finally {
					entity.consumeContent();
				}
			}
		} catch (Exception e) {
			Log.v("HttpUtil", e.toString());
			try {
				if (getRequest != null)
					getRequest.abort();
				if (httpClient != null)
					httpClient.getConnectionManager().shutdown();
			} catch (Exception e1) {
				Log.v("HttpUtil", e1.toString());
			}
		}
		try {
			getRequest.abort();
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpClient.getConnectionManager().shutdown();
		} catch (Exception e1) {
			Log.v("HttpUtil", e1.toString());
		}
		return strResBody;
	}

	private static String getBody(Map map){
		StringBuffer buffer = new StringBuffer();
		for(Iterator itor = map.keySet().iterator();itor.hasNext();) {
			String key = (String)itor.next();
			String value = (String)map.get(key);
			buffer.append(key).append("=").append(value).append("&");
		}
		buffer.replace(buffer.lastIndexOf("&"), buffer.lastIndexOf("&") + 1, "");
		return buffer.toString();
	}
}
