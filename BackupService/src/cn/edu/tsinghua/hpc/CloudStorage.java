
package cn.edu.tsinghua.hpc;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CloudStorage {
    private static final String MYPREFERENCES_URL_TEP = "http://210.75.5.232:8085/%s/myPreferences/%s";

    private static DefaultHttpClient client = null;

    private static CloudStorage mInstance = null;

    private static final String TAG = "BackupService";

    private static final long BLOCK_SIZE = 4096 * 10;

    private CloudStorage() {
        if (client == null) {
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 300000);
            HttpConnectionParams.setSoTimeout(httpParams, 300000);
            /*SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry
                    .register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 8085));
            ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);*/
            client = new DefaultHttpClient(httpParams);
        }
    }

    public static CloudStorage getInstance() {
        if (mInstance == null)
            mInstance = new CloudStorage();
        return mInstance;
    }

    public boolean putFile(String file, String uid, String token, InputStream is, long length) {
        String url = String.format(MYPREFERENCES_URL_TEP, uid, file);
        String authInfo = String.format("ASP %s:%s", uid, token);
        HttpPut p = new HttpPut(url);
        p.setEntity(new InputStreamEntity(is, length));
        p.setHeader("Authorization", authInfo);
        p.setHeader("Content-Range", String.format("bytes %d-%d/%d", 0, length - 1, length));
        try {
            HttpResponse r = client.execute(p);
            if (r.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public String getMd5(File tmpFile) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e2) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(tmpFile);
        } catch (FileNotFoundException e2) {
            return null;
        }
        try {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            is.close();
        } catch (IOException e) {
            return null;
        }
        byte[] digest = md.digest();
        return Base64.encodeToString(digest, Base64.DEFAULT);
    }

    public boolean putFile(String file, String remoteFile, String uid, String token) {
        String url = String.format(MYPREFERENCES_URL_TEP, uid, remoteFile);
        String authInfo = String.format("ASP %s:%s", uid, token);
        File f = new File(file);
        String md5 = getMd5(f);
        Log.d(TAG, "md5 is " + md5);
        long transferLength = f.length();
        Log.d(TAG, "file length is " + f.length());
        long transferedLength = 0;
        while (transferLength > 0) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(f);
                is.skip(transferedLength);
                Log.d(TAG, "open input stream");
            } catch (IOException e) {
                return false;
            }
            HttpPut p = new HttpPut(url);
            p.setHeader("Authorization", authInfo);
            long size = transferLength > BLOCK_SIZE ? BLOCK_SIZE : transferLength;
            p.setHeader("Content-Range", String.format("bytes %d-%d/%d", transferedLength,
                    transferedLength + size - 1, f.length()));
            p.setEntity(new InputStreamEntity(is, size));
            Log.d(TAG, "before Http post");
            while (true) {
                try {
                    for(Header h : p.getAllHeaders()) {
                        Log.d(TAG, h.getName() + ":" +h.getValue());
                    }
                    HttpResponse r = client.execute(p);
                    Log.d(TAG, "after Http post " + r.getStatusLine().getStatusCode());
                    if (r.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                        transferLength -= size;
                        transferedLength += size;
                        is.close();
                        Log.d(TAG, "Transfered " + transferedLength);
                        break;
                    }
                } catch (ClientProtocolException e) {
                    Log.d(TAG, e.toString());
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }

        return false;
    }

    public boolean deleteFile(String file, String uid, String token) {

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
