package cn.edu.tsinghua.hpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class CloudStorage {
	private static final String MYPREFERENCES_URL_TEP = "http://210.75.5.232:8085/%s/myPreferences/%s";

	private static DefaultHttpClient client = null;
	
	private static CloudStorage mInstance = null;
	
	private static final String TAG = "CloudStorage";
	
	private CloudStorage() {
		if(client == null) {	
			HttpParams httpParams = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParams, 300000);
		    HttpConnectionParams.setSoTimeout(httpParams, 300000);
		    client = new DefaultHttpClient(httpParams);
		}
	}
	
	public static CloudStorage getInstance() {
		if(mInstance == null) 
			mInstance = new CloudStorage();
		return mInstance;
	}
	
	public boolean putFile(String file, String uid, String token,
			InputStream is, long length) {
		String url = String.format(MYPREFERENCES_URL_TEP, uid, file);
		String authInfo = String.format("ASP %s:%s", uid, token);
		HttpPut p = new HttpPut(url);
		p.setEntity(new InputStreamEntity(is, length));
		p.setHeader("Authorization", authInfo);
		try {
			HttpResponse r = client.execute(p);
			if (r.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

	public boolean deleteFile(String file, String uid, String token,
			InputStream is, long length) {

		String authInfo = String.format("ASP %s:%s", uid, token);
		URL url;
		try {
			url = new URL(String.format(MYPREFERENCES_URL_TEP, uid, file));
			URLConnection connection = url.openConnection();
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setRequestMethod("DELETE");
			connection.setRequestProperty("Authorization", authInfo);
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				return true;
			}
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}

		return false;
	}

	public InputStream downloadFile(String file, String uid, String token) {
		String url = String.format(MYPREFERENCES_URL_TEP, uid, file);
		String authInfo = String.format("ASP %s:%s", uid, token);
		HttpGet g = new HttpGet(url);
		g.setHeader("Authorization", authInfo);
		try {
			HttpResponse r = client.execute(g);
			return r.getEntity().getContent();
		} catch (IOException e) {
		}

		return null;
	}
}
