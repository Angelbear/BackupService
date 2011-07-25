
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
 * Description:客户端安全组件测试demo（支持CPK和CA两种认证方式）
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
    public static String ip = "210.75.5.231";// 核高基实验室环境ccit用户ip地址和端口号

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
     * 登录请求（CPK）
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
     * 验证ca签名
     *
     * @param context 上下文环境
     * @param ip 用户管理&认证平台ip
     * @param port 用户管理&认证平台port
     * @param uid uid
     * @param signature 用户签名值（base64编码）
     * @return 1：验证签名成功 ;2：验证签名失败; null：其它错误
     * @throws Exception
     */
    public String verifySignForCA(Context context, String ip, String port, String uid,
            String signature) throws Exception {
        String result = null;
        String strReponse = null;// Http响应
        String[] strRepArray = null;// 解析Http数据集合
        String verifyCaSignUrl = null;// 验证cpk签名url（从配置文件中获取）

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
        // 对cpk签名值做uri编码（get请求在传输过程中可能会对一些特殊字符做处理，如将“+”变成空格，造成服务端验证签名失败，所以要先进行uri编码）
        String signatureUri = Uri.encode(signature);
        Log.v("CAUtils", "caSignatureAfterUriEncode= " + signatureUri);

        Map<String, String> map = new HashMap<String, String>();
        map.put("UID", uid);
        map.put("userSignature", signatureUri);

        // 发送 Http Get请求
        strReponse = HttpUtil.sendGet(context, verifyCaSignUrl, map); // 采用retCode=value&retDes=value&verifyResult=value返回

        if (strReponse == null) {
            Log.v("CAUtils", "去服务端请求验证ca签名，网络异常或服务端响应为空**********************");
            return result;
        }

        Log.v("CAUtils", "strReponse=" + strReponse);

        strRepArray = strReponse.split("&", 3); // 解析数据

        if ("retCode=1".equals(strRepArray[0])) { // 请求成功
            String verifyResStr = strRepArray[2];
            if (verifyResStr.equals("verifyResult=") == false) {
                Log.v("CAUtils", verifyResStr);
                result = "1";// 验证签名成功
            }
        } else {
            result = "2";// 验证签名失败
            Log.v("CAUtils", "Verify cpk signature is failed!");
        }
        return result;
    }

    /**
     * 验证cpk签名
     *
     * @param context 上下文环境
     * @param ip 用户管理&认证平台ip
     * @param port 用户管理&认证平台port
     * @param uid uid
     * @param signature 用户签名值
     * @return 0：验证签名成功 1：验证签名失败 2：参数为空 null：其它错误
     * @throws Exception
     */
    public String verifySignForCPK(Context context, String ip, String port, String uid,
            String signature) throws Exception {
        String result = null;
        String strReponse = null;// Http响应
        String[] strRepArray = null;// 解析Http数据集合
        String verifyCpkSignUrl = null;// 验证cpk签名url（从配置文件中获取）

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
        // 对cpk签名值做uri编码（get请求在传输过程中可能会对一些特殊字符做处理，如将“+”变成空格，造成服务端验证签名失败，所以要先进行uri编码）
        String signatureUri = Uri.encode(signature);
        Log.v("CAUtils", "cpkSignatureAfterUriEncode= " + signatureUri);

        Map<String, String> map = new HashMap<String, String>();
        map.put("UID", uid);
        map.put("signature", signatureUri);

        // 发送 Http Get请求
        strReponse = HttpUtil.sendGet(context, verifyCpkSignUrl, map); // 采用retCode=value&retDes=value&verifyResult=value返回

        if (strReponse == null) {
            Log.v("CAUtils", "去服务端请求验证cpk签名，网络异常或服务端响应为空**********************");
            return result;
        }

        Log.v("CAUtils", "strReponse=" + strReponse);

        strRepArray = strReponse.split("&", 3); // 解析数据

        if ("retCode=0".equals(strRepArray[0])) { // 请求成功
            String verifyResStr = strRepArray[2];
            if (verifyResStr.equals("verifyResult=") == false) {
                Log.v("CAUtils", verifyResStr);
                result = "0";// 验证签名成功
            }
        } else {
            result = "1";// 验证签名失败
            Log.v("CAUtils", "Verify cpk signature is failed!");
        }
        return result;
    }

    /**
     * 删除私钥文件（CA）
     */
    public void delPrikeyFileForCA() {
        // 创建电话管理, 与手机建立连接
        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(context.TELEPHONY_SERVICE);
        // 获取 IMSI 号
        String IMSI = mTelephonyMgr.getSubscriberId();

        File sdCardDir = Environment.getExternalStorageDirectory();// 获取SDCard目录,2.2的时候为:/mnt/sdcart
        // 2.1的时候为：/sdcard，所以使用静态方法得到路径会好一点。

        String caPrikeyFilePath = sdCardDir + "/." + IMSI + ".keystore";
        File caPrikeyFile = new File(caPrikeyFilePath);
        if (caPrikeyFile.exists()) {
            caPrikeyFile.delete();
        }
    }

    /**
     * 删除私钥文件（CPK）
     */
    public void delPrikeyFileForCPK() {
        // 创建电话管理, 与手机建立连接
        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(context.TELEPHONY_SERVICE);
        // 获取 IMSI 号
        String imsi = mTelephonyMgr.getSubscriberId();

        File sdCardDir = Environment.getExternalStorageDirectory();// 获取SDCard目录,2.2的时候为:/mnt/sdcart
        // 2.1的时候为：/sdcard，所以使用静态方法得到路径会好一点。
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
