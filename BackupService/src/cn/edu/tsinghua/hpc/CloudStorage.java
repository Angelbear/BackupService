
package cn.edu.tsinghua.hpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;

import android.net.http.AndroidHttpClient;

public class CloudStorage {
    private static final String MYPREFERENCES_URL_TEP = "http://210.75.5.232:8085/%s/myPreferences/%s";

    public static boolean putFile(String file, String uid, String token, InputStream is, long length) {
        String url = String.format(MYPREFERENCES_URL_TEP, uid, file);
        String authInfo = String.format("CPK %s:%s", uid, token);
        HttpPost p = new HttpPost(url);
        p.setEntity(new InputStreamEntity(is, length));
        p.setHeader("Authorization", authInfo);
        try {
            HttpResponse r = AndroidHttpClient.newInstance("Android").execute(p);
            if (r.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException e) {

        }
        return false;
    }

    public static boolean deleteFile(String file, String uid, String token, InputStream is,
            long length) {

        String authInfo = String.format("CPK %s:%s", uid, token);
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

    public static InputStream downloadFile(String file, String uid, String token) {
        String url = String.format(MYPREFERENCES_URL_TEP, uid, file);
        String authInfo = String.format("CPK %s:%s", uid, token);
        HttpGet g = new HttpGet(url);
        g.setHeader("Authorization", authInfo);
        try {
            HttpResponse r = AndroidHttpClient.newInstance("Android").execute(g);
            return r.getEntity().getContent();
        } catch (IOException e) {

        }

        return null;
    }
}
