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
 * Description: http��������
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
	private static final int SO_TIMEOUT_INT = 30000; // ��ӦHttpʱ��30��
	private static final String SERVER_CHARACTER_ENCODING_MODEL_STR = "GBK";// �����������ݵ��ַ����뷽ʽ

	public static String sendGet(Context context,String url,Map<String, String> getMap) throws Exception{
		String strResBody = null;

		String param = getBody(getMap);
		String mUrl = url+"?"+param;

		HttpClient httpClient = null;
		HttpGet getRequest = null;
		try {
			// �������ӳ�ʱʱ������ݶ�ȡ��ʱʱ��
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams,CONNECTION_TIMEOUT_INT);
			HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT_INT);
			// �½�HttpClient����
			httpClient = new DefaultHttpClient(httpParams); //

			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); // wifi
			// �ж�WiFi״̬�Ƿ�"�Ѵ�"
			if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
				Log.v("HttpUtil", "wifiû�д�*************************");
				Uri uri = Uri.parse("content://telephony/carriers/preferapn"); // ��ȡ��ǰ����ʹ�õ�APN�����
				Cursor mCursor = context.getContentResolver().query(uri, null,
						null, null, null);
				if (mCursor != null) {
					mCursor.moveToNext(); // �α�������һ����¼����ȻҲֻ��һ��
					String proxyStr = mCursor.getString(mCursor
							.getColumnIndex("proxy")); // cmnet
					if (proxyStr != null && proxyStr.trim().length() > 0) {
						HttpHost proxy = new HttpHost(proxyStr, 80); // cmwap
						httpClient.getParams().setParameter(
								ConnRouteParams.DEFAULT_PROXY, proxy);
					}
				}else{
					Log.v("HttpUtil", "û�з���APN*************************");
				}
			}else{
				Log.v("HttpUtil", "ʹ��wifi��������*************************");
			}
			// generate get request
			Log.v("HttpUtil", "����get����url = " + mUrl);
			getRequest = new HttpGet(mUrl);

			// ��ȡ��������Ӧ
			HttpResponse response = httpClient.execute(getRequest);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new Exception(response.getStatusLine().toString());
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					// ����
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
