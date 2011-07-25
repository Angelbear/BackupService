
package cn.edu.tsinghua.hpc.ca;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Title: MainActivity
 * </p>
 * <p>
 * Description:�ͻ��˰�ȫ�������demo��֧��CPK��CA������֤��ʽ��
 * </p>
 * <p>
 * Copyright: Copyright (c) 2011
 * </p>
 * <p>
 * Company: ccit
 * </p>
 *
 * @author chensonglin
 * @date 2011.5.27
 * @version 1.0
 */
public class CAUtils {
    public static String ip = "210.75.5.231";// �˸߻�ʵ���һ���ccit�û�ip��ַ�Ͷ˿ں�

    public static String port = "8080";

    LoginView loginViewForCA = null;

    LoginView loginViewForCPK = null;

    private Context context = null;

    private static CAUtils _instance;

    public static final CAUtils getInstance(Context context) {
        if (_instance == null) {
            _instance = new CAUtils(context);
        }
        return _instance;
    }

    private CAUtils(Context context) {
        this.context = context;
    }

    public LoginView requestLoginForCA() {
        try {
            CCITSC ccitsc = new CCITSC(context, ip, port);
            ccitsc.loginInit(false);
            loginViewForCA = ccitsc.requestLogin(false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.v("MainActivity", e.toString());
        }
        return loginViewForCA;
    }

    /**
     * ��¼����CPK��
     *
     * @return loginViewForCPK
     */
    public LoginView requestLoginForCPK() {
        try {
            CCITSC ccitsc = new CCITSC(context, ip, port);
            ccitsc.loginInit(true);
            loginViewForCPK = ccitsc.requestLogin(true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.v("MainActivity", e.toString());
        }
        return loginViewForCPK;
    }

    /**
     * ��֤caǩ��
     *
     * @param context �����Ļ���
     * @param ip �û�����&��֤ƽ̨ip
     * @param port �û�����&��֤ƽ̨port
     * @param uid uid
     * @param signature �û�ǩ��ֵ��base64���룩
     * @return 1����֤ǩ���ɹ� ;2����֤ǩ��ʧ��; null����������
     * @throws Exception
     */
    public String verifySignForCA(Context context, String ip, String port, String uid,
            String signature) throws Exception {
        String result = null;
        String strReponse = null;// Http��Ӧ
        String[] strRepArray = null;// ����Http���ݼ���
        String verifyCaSignUrl = null;// ��֤cpkǩ��url���������ļ��л�ȡ��

        if (context == null || context.equals("") || uid == null || uid.equals("")
                || signature == null || signature.equals("")) {
            result = "2";
            Log.v("MainActivity", "verifyCaSign  param is null!");
            return result;
        }

        // http://ip:port/authcloud/LoginAuth?UID=******&userSignature=******
        verifyCaSignUrl = "http://" + ip + ":" + port + "/authcloud/LoginAuth";
        Log.v("CAUtils", "verifyCaSignUrl= " + verifyCaSignUrl);

        Log.v("CAUtils", "CaSignature= " + signature);
        // ��cpkǩ��ֵ��uri���루get�����ڴ�������п��ܻ��һЩ�����ַ��������罫��+����ɿո���ɷ������֤ǩ��ʧ�ܣ�����Ҫ�Ƚ���uri���룩
        String signatureUri = Uri.encode(signature);
        Log.v("CAUtils", "caSignatureAfterUriEncode= " + signatureUri);

        Map<String, String> map = new HashMap<String, String>();
        map.put("UID", uid);
        map.put("userSignature", signatureUri);

        // ���� Http Get����
        strReponse = HttpUtil.sendGet(context, verifyCaSignUrl, map); // ����retCode=value&retDes=value&verifyResult=value����

        if (strReponse == null) {
            Log.v("CAUtils", "ȥ�����������֤caǩ���������쳣��������ӦΪ��**********************");
            return result;
        }

        Log.v("CAUtils", "strReponse=" + strReponse);

        strRepArray = strReponse.split("&", 3); // ��������

        if ("retCode=1".equals(strRepArray[0])) { // ����ɹ�
            String verifyResStr = strRepArray[2];
            if (verifyResStr.equals("verifyResult=") == false) {
                Log.v("CAUtils", verifyResStr);
                result = "1";// ��֤ǩ���ɹ�
            }
        } else {
            result = "2";// ��֤ǩ��ʧ��
            Log.v("CAUtils", "Verify cpk signature is failed!");
        }
        return result;
    }

    /**
     * ��֤cpkǩ��
     *
     * @param context �����Ļ���
     * @param ip �û�����&��֤ƽ̨ip
     * @param port �û�����&��֤ƽ̨port
     * @param uid uid
     * @param signature �û�ǩ��ֵ
     * @return 0����֤ǩ���ɹ� 1����֤ǩ��ʧ�� 2������Ϊ�� null����������
     * @throws Exception
     */
    public String verifySignForCPK(Context context, String ip, String port, String uid,
            String signature) throws Exception {
        String result = null;
        String strReponse = null;// Http��Ӧ
        String[] strRepArray = null;// ����Http���ݼ���
        String verifyCpkSignUrl = null;// ��֤cpkǩ��url���������ļ��л�ȡ��

        if (context == null || context.equals("") || uid == null || uid.equals("")
                || signature == null || signature.equals("")) {
            result = "2";
            Log.v("CAUtils", "verifyCpkSign  param is null!");
            return result;
        }

        // http://ip:port/authcloud/verifySignForCPK?UID=******&signature
        // =******
        verifyCpkSignUrl = "http://" + ip + ":" + port + "/authcloud/verifySignForCPK";
        Log.v("CAUtils", "verifyCpkSignUrl= " + verifyCpkSignUrl);

        Log.v("CAUtils", "cpkSignature= " + signature);
        // ��cpkǩ��ֵ��uri���루get�����ڴ�������п��ܻ��һЩ�����ַ��������罫��+����ɿո���ɷ������֤ǩ��ʧ�ܣ�����Ҫ�Ƚ���uri���룩
        String signatureUri = Uri.encode(signature);
        Log.v("CAUtils", "cpkSignatureAfterUriEncode= " + signatureUri);

        Map<String, String> map = new HashMap<String, String>();
        map.put("UID", uid);
        map.put("signature", signatureUri);

        // ���� Http Get����
        strReponse = HttpUtil.sendGet(context, verifyCpkSignUrl, map); // ����retCode=value&retDes=value&verifyResult=value����

        if (strReponse == null) {
            Log.v("CAUtils", "ȥ�����������֤cpkǩ���������쳣��������ӦΪ��**********************");
            return result;
        }

        Log.v("CAUtils", "strReponse=" + strReponse);

        strRepArray = strReponse.split("&", 3); // ��������

        if ("retCode=0".equals(strRepArray[0])) { // ����ɹ�
            String verifyResStr = strRepArray[2];
            if (verifyResStr.equals("verifyResult=") == false) {
                Log.v("CAUtils", verifyResStr);
                result = "0";// ��֤ǩ���ɹ�
            }
        } else {
            result = "1";// ��֤ǩ��ʧ��
            Log.v("CAUtils", "Verify cpk signature is failed!");
        }
        return result;
    }

    /**
     * ɾ��˽Կ�ļ���CA��
     */
    public void delPrikeyFileForCA() {
        // �����绰����, ���ֻ���������
        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(context.TELEPHONY_SERVICE);
        // ��ȡ IMSI ��
        String IMSI = mTelephonyMgr.getSubscriberId();

        File sdCardDir = Environment.getExternalStorageDirectory();// ��ȡSDCardĿ¼,2.2��ʱ��Ϊ:/mnt/sdcart
        // 2.1��ʱ��Ϊ��/sdcard������ʹ�þ�̬�����õ�·�����һ�㡣

        String caPrikeyFilePath = sdCardDir + "/." + IMSI + ".keystore";
        File caPrikeyFile = new File(caPrikeyFilePath);
        if (caPrikeyFile.exists()) {
            caPrikeyFile.delete();
        }
    }

    /**
     * ɾ��˽Կ�ļ���CPK��
     */
    public void delPrikeyFileForCPK() {
        // �����绰����, ���ֻ���������
        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(context.TELEPHONY_SERVICE);
        // ��ȡ IMSI ��
        String imsi = mTelephonyMgr.getSubscriberId();

        File sdCardDir = Environment.getExternalStorageDirectory();// ��ȡSDCardĿ¼,2.2��ʱ��Ϊ:/mnt/sdcart
        // 2.1��ʱ��Ϊ��/sdcard������ʹ�þ�̬�����õ�·�����һ�㡣
        File[] fileList = sdCardDir.listFiles(new FileFilter(imsi));

        if (fileList.length == 1) {
            String cpkPrikeyFilePath = fileList[0].getAbsolutePath();
            File cpkPrikeyFile = new File(cpkPrikeyFilePath);
            if (cpkPrikeyFile.exists()) {
                cpkPrikeyFile.delete();
            }
        }
    }

}
