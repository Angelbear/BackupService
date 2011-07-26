package cn.edu.tsinghua.hpc;

import java.io.InputStream;

public class CloudStorage {
    private static final String CLOUD_STORAGE_URL = "http://210.75.5.232:8085/";
    private static final String MYPREFERENCES_URL_TEP = "http://210.75.5.232:8085/%s/myPreferences/";

    private int putFile(String uid, String token, InputStream is) {
        String url = String.format(MYPREFERENCES_URL_TEP, uid);
        
        return 0;
    }
}
