package xyz.codeme.szzn.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.codeme.loginer.LoginFragment;
import xyz.codeme.loginer.R;
import xyz.codeme.loginer.utils.MessageBuilder;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

/**
 * 数字中南登陆网络交互类
 * <p/>
 * 首先获取IP，根据用户名/加密后的密码/IP，可登陆/下线/重新登陆(先下线再登陆)。
 * session为内部管理，过期后重登即可重新获取。
 * 登陆操作后可获取账户信息。
 *
 * @author Msir
 * @date 2015/3
 */
public class HttpUtils {
    public final static int IP_FROM_SERVER      = 0xac01;
    public final static int IP_FROM_ROUTER      = 0xac02;
    public final static int CONNECTED_TRUE      = 0xac11;
    public final static int CONNECTED_FALSE     = 0xac12;
    public final static int GET_IP_SUCCESS      = 0xac13;
    public final static int LONIN_SUCCESS       = 0xac14;
    public final static int LONIN_FAILED        = 0xac15;
    public final static int GET_ACCOUNT_SUCCESS = 0xac16;
    public final static int GET_VERSION         = 0xac17;
    public final static int REGISTER_SUCCESS    = 0xac18;
    public final static int GET_LAST_IP_SUCCESS = 0xac19;
    
    private final static String TAG             = "LoggerHttp";

    private final static String szznIndexUrl = "http://61.137.86.87:8080/portalNat444";
    private final static String loginUrl     = szznIndexUrl + "/AccessServices/login";
    private final static String logoutUrl    = szznIndexUrl + "/AccessServices/logout";
    private final static String mainUrl      = szznIndexUrl + "/index.jsp";
    private final static String showUrl      = szznIndexUrl + "/main2.jsp";
    // 192.168.56.1/sz mccode.net
    private final static String apiIndexUrl  = "http://mccode.net/api";
    private final static String saveUrl      = apiIndexUrl + "/saveip.php";
    private final static String getipUrl     = apiIndexUrl + "/getip.php";
    private final static String versionUrl   = apiIndexUrl + "/version.php";
    private final static String registerUrl  = apiIndexUrl + "/register.php";

    private final Fragment fragment;
    private final Handler handler;
    private RequestQueue requestQueue;
    private SparseArray<ProgressDialog> progressDialogs;

    private int ipAccessMethod = IP_FROM_SERVER;
    private String routerURL     = "";
    private String routerReferer = "";
    private String routerCookie  = "";
    private String routerReg     = "";
    private String log           = "";
    private boolean ifSaveIP     = true;
    private int userID;
    private String user;
    private String password;
    private String ip;

    public HttpUtils(Fragment fragment, Handler handler) {
        this.fragment = fragment;
        this.handler = handler;
        this.requestQueue = Volley.newRequestQueue(fragment.getActivity());
        this.progressDialogs = new SparseArray<>();
        HttpURLConnection.setFollowRedirects(false);
    }

    /**
     * 路由器配置
     *
     * @param routerURL     路由管理页
     * @param routerReferer 路由管理头部Referrer
     * @param routerCookie  路由登陆Cookie
     * @param routerReg     匹配IP地址正则表达式
     */
    public void routerConfigure(String routerURL, String routerReferer,
                                String routerCookie, String routerReg) {
        ipAccessMethod = IP_FROM_ROUTER;
        this.routerURL = routerURL;
        this.routerReferer = routerReferer;
        this.routerCookie = routerCookie;
        this.routerReg = routerReg;
    }

    /**
     * 注册新用户
     * @param versionCode 当前版本号
     */
    public void register(int versionCode) {
        KeyValuePairs form = KeyValuePairs.create()
                .add("version", versionCode);

        FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.registerUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try {
                            int resultCode = jsonObj.getInt("code");
                            if (resultCode == 0) {
                                int userID = jsonObj.getInt("userID"); // 获取注册的用户id
                                Log.i(TAG, "register:success");
                                handler.sendMessage(MessageBuilder.simpleMessage(
                                        REGISTER_SUCCESS,
                                        "userID",
                                        userID
                                ));
                            } else {
                                log = resultCode + ":" + jsonObj.getString("msg");
                                Log.w(TAG, "register:" + log);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "register:json error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Log.e(TAG, "register:connect error");
                    }
                });

        requestQueue.add(jsonRequest);
    }

    /**
     * 登陆
     */
    public void login(final String user, final String password, final String IP) {
        KeyValuePairs form = KeyValuePairs.create()
                .add("accountID", user + "@zndx.inter")
                .add("password", password)
                .add("brasAddress", "59df7586")
                .add("userIntranetAddress", IP);
        KeyValuePairs headers = KeyValuePairs.create()
                .add("Referer", HttpUtils.mainUrl);

        showProgress(R.string.text_loading_login);

        FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.loginUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try {
                            int resultCode = jsonObj.getInt("resultCode");
                            if (resultCode == 0) {
                                log = parseCode(resultCode);
                                showToast(R.string.success_login);
                                Log.i(TAG, "login:success");
                                getInformation(jsonObj);
                                if(ifSaveIP)
                                    saveIP(user, IP);
                                handler.sendEmptyMessage(LONIN_SUCCESS);
                                return;
                            }
                            log = parseCode(resultCode) + " " + jsonObj.getString("resultDescribe");
                            showMessage(R.string.error_login, log);
                            Log.e(TAG, "login:" + log);
                            if (resultCode != 2) // 除用户连接已经存在情况，发送登陆失败信号
                                handler.sendEmptyMessage(LONIN_FAILED);
                        } catch (JSONException e) {
                            Log.e(TAG, "login:json error");
                            showToast(R.string.error_login);
                            handler.sendEmptyMessage(LONIN_FAILED);
                        } finally {
                            dismissProgress(R.string.text_loading_login);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        dismissProgress(R.string.text_loading_login);
                        showToast(R.string.error_login);
                        Log.e(TAG, "login:connect error");
                    }
                });
        jsonRequest.setHeaders(headers.build());

        requestQueue.add(jsonRequest);
    }

    /**
     * 不回调下线
     * @param IP 需下线IP，任何IP均可
     */
    public void logout(final String IP) {
        logout(IP, false);
    }

    /**
     * 下线
     * @param IP 需下线IP，任何IP均可
     * @param callback 是否回调
     */
    public void logout(final String IP, final boolean callback) {
        KeyValuePairs form = KeyValuePairs.create()
                .add("brasAddress", "59df7586")
                .add("userIntranetAddress", IP);
        KeyValuePairs headers = KeyValuePairs.create()
                .add("Referer", HttpUtils.showUrl);

        showProgress(R.string.text_loading_logout);

        FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.logoutUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try {
                            int resultCode = jsonObj.getInt("resultCode");
                            if (resultCode == 0) {
                                log = parseCode(resultCode);
                                showToast(R.string.success_logout);
                                Log.i(TAG, "logout:success");
                                if(callback) logoutSuccess(); // 回调，用来重新登陆
                                return;
                            }
                            log = parseCode(resultCode) + " " + jsonObj.getString("resultDescribe");
                            showMessage(R.string.error_logout, log);
                            Log.e(TAG, "logout:" + log);
                        } catch (JSONException e) {
                            Log.e(TAG, "logout:json error");
                            showToast(R.string.error_logout);
                        } finally {
                            dismissProgress(R.string.text_loading_logout);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        dismissProgress(R.string.text_loading_logout);
                        showToast(R.string.error_logout);
                        Log.e(TAG, "logout:connect error");
                    }
                });
        jsonRequest.setHeaders(headers.build());

        requestQueue.add(jsonRequest);
    }

    /**
     * 下线函数的回调函数
     */
    private void logoutSuccess() {
        login(user, password, ip);
    }

    /**
     * 重新登陆
     */
    public void relogin(String user, String password, String ip) {
        this.user = user;
        this.password = password;
        this.ip = ip;
        logout(ip, true);
    }

    /**
     * 获取IP地址
     */
    public void getIP() {
        String ip = null;
        // 先从设备获取IP，是10.96开头则显示
        WifiManager wifiManager;
        wifiManager = (WifiManager) fragment.getActivity().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            if ((i & 0xFF) == 10 && ((i >> 8) & 0xFF) == 96) {
                ip = "10.96." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
            }
        } else {
            // 没有连接WiFi
            showToast(R.string.error_nowifi);
            return;
        }
        if (ip != null) {
            handler.sendMessage(MessageBuilder.simpleMessage(
                    GET_IP_SUCCESS,
                    "IP",
                    ip
            ));
            return;
        }
        Log.w(TAG, "local端获取IP失败");
        switch (this.ipAccessMethod) {
            case IP_FROM_SERVER:
                getIPFromServer();
                break;
            case IP_FROM_ROUTER:
                getIPFromRouter();
                break;
        }
    }

    /**
     * 上传保存IP
     *
     * @param user 用户名
     * @param IP   要保存的IP
     */
    public void saveIP(String user, String IP) {
        KeyValuePairs form = KeyValuePairs.create()
                .add("user", user)
                .add("ip", IP)
                .add("userID", userID);

        FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.saveUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try {
                            int resultCode = jsonObj.getInt("code");
                            if (resultCode != 0) {
                                log = resultCode + ":" + jsonObj.getString("msg");
                                Log.w(TAG, "saveip:" + log);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "saveip:json error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Log.e(TAG, "saveip:connect error");
                    }
                });

        requestQueue.add(jsonRequest);
    }

    /**
     * 获取上次登录IP
     *
     * @param user 要获取IP的用户
     */
    public void getLastIP(String user) {
        showProgress(R.string.text_loading_get);
        Thread thread = new Thread(new GetLastIP(this.handler, user));
        thread.start();
    }

    public void closeWifi() {
        WifiManager wifiManager;
        wifiManager = (WifiManager) fragment.getActivity().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        // 等待WiFi关闭
        while(wifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED);

        // 查看网络状态
        ConnectivityManager connectMgr = (ConnectivityManager) fragment.getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectMgr.getActiveNetworkInfo();
        int inc = 0;
        while(info == null) {
            if(inc++ == 200) {
                break;
            }
            info = connectMgr.getActiveNetworkInfo();
        }
    }

    public void openWifi() {
        WifiManager wifiManager;
        wifiManager = (WifiManager) fragment.getActivity().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private class GetLastIP implements Runnable {
        private Handler handler;
        private String user;
        public GetLastIP(Handler handler, String user) {
            this.handler = handler;
            this.user = user;
        }
        @Override
        public void run() {
            // 关闭Wifi
//            closeWifi();
//            showToast(R.string.msg_open_network);

            KeyValuePairs form = KeyValuePairs.create()
                    .add("user", this.user)
                    .add("userID", userID);

            FluentJsonRequest jsonRequest = new FluentJsonRequest(
                    HttpUtils.getipUrl,
                    form.build(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObj) {
                            try {
                                int resultCode = jsonObj.getInt("code");
                                if (resultCode == 0) {
                                    String ip = jsonObj.getString("ip");
//                				String time = jsonObj.getString("time");
                                    handler.sendMessage(MessageBuilder.simpleMessage(
                                            GET_LAST_IP_SUCCESS,
                                            "IP",
                                            ip
                                    ));
                                    showToast(R.string.success_last_ip);
                                } else {
                                    log = resultCode + ":" + jsonObj.getString("msg");
                                    Log.e(TAG, "getlastip:" + log);
                                    showMessage(R.string.error_ip, jsonObj.getString("msg"));
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "getlastip:json error");
                                showToast(R.string.error_ip);
                            } finally {
                                dismissProgress(R.string.text_loading_get);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError arg0) {
                            dismissProgress(R.string.text_loading_get);
                            Log.e(TAG, "getlastip:connect error");
                            showToast(R.string.error_ip);
                        }
                    });

            requestQueue.add(jsonRequest);
        }
    }

    /**
     * 获取用户信息
     */
    public void getInformation(JSONObject jsonObj) {
        try {
            AccountInfo account = new AccountInfo(jsonObj);
            handler.sendMessage(MessageBuilder.bundleMessage(
                    GET_ACCOUNT_SUCCESS,
                    account.parseBundle()
            ));
        } catch(JSONException e) {
            showToast(R.string.error_account);
            Log.w(TAG, "getInformation:error");
        }
    }

    /**
     * 在线检测
     */
    private class ConnectCheck implements Runnable {
        private Handler handler;
        public ConnectCheck(Handler handler) {
            this.handler = handler;
        }
        @Override
        public void run() {
            Process p;
            BufferedReader reader;
            String line;
            try
            {
                p = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 180.76.76.76");
                reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while((line = reader.readLine()) != null) {
                    if(line.toLowerCase().indexOf("ttl") > 0) {
                        handler.sendEmptyMessage(CONNECTED_TRUE);
                        return;
                    }
                }
                handler.sendEmptyMessage(CONNECTED_FALSE);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否在线
     */
    public void isConnected() {
        new Thread(new ConnectCheck(handler)).start();
    }

    public void checkUpdate() {
        showProgress(R.string.text_loading_update);

        FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.versionUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try {
                            int resultCode = jsonObj.getInt("code");
                            if (resultCode == 0) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("versionCode", jsonObj.getInt("versionCode"));
                                bundle.putString("versionName", jsonObj.getString("versionName"));
                                handler.sendMessage(MessageBuilder.bundleMessage(
                                        GET_VERSION,
                                        bundle
                                ));
                            } else {
                                Log.e(TAG, "CheckUpdate:server error");
                                showToast(R.string.error_update);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "CheckUpdate:json error");
                            showToast(R.string.error_update);
                        } finally {
                            dismissProgress(R.string.text_loading_update);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        dismissProgress(R.string.text_loading_update);
                        Log.e(TAG, "CheckUpdate:connect error");
                        showToast(R.string.error_update);
                    }
                });

        requestQueue.add(jsonRequest);
    }
    /**
     * 从数字中南获取IP
     */
    private void getIPFromServer() {
        FluentStringRequest request = new FluentStringRequest(
                Request.Method.GET,
                "http://wap.baidu.com/",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String content) {
                        if (content.indexOf("百度") > 0) // 当前在线
                        {
                            showToast(R.string.msg_online);
                            Log.w(TAG, "server端获取IP失败，在线");
                            return;
                        }

                        Pattern pattern = Pattern.compile("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
                        Matcher match = pattern.matcher(content);
                        if (match.find()) {
                            handler.sendMessage(MessageBuilder.simpleMessage(
                                    GET_IP_SUCCESS,
                                    "IP",
                                    match.group(0)
                            ));
                            Log.i(TAG, "getIPFromServer:success");
                        } else {
                            Log.w(TAG, "server端获取IP失败");
                            showToast(R.string.error_ip);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Log.w(TAG, "server端获取IP失败");
                        showToast(R.string.error_ip);
                    }
                });

        requestQueue.add(request);
    }

    /**
     * 根据配置从路由器web管理页面获取当前IP
     */
    private void getIPFromRouter() {
        KeyValuePairs headers = KeyValuePairs.create()
                .add("Referer", this.routerReferer)
                .add("Cookie", this.routerCookie);
        FluentStringRequest request = new FluentStringRequest(
                Request.Method.GET,
                this.routerURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String content) {
                        Pattern pattern = Pattern.compile(routerReg);
                        Matcher match = pattern.matcher(content);
                        if (match.find()) {
                            handler.sendMessage(MessageBuilder.simpleMessage(
                                    GET_IP_SUCCESS,
                                    "IP",
                                    match.group(0)
                            ));
                            Log.i(TAG, "getIPFromRouter:success");
                        } else {
                            Log.w(TAG, "router端获取IP失败");
                            showToast(R.string.error_router);
                            getIPFromServer();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Log.w(TAG, "router端获取IP失败");
                        showToast(R.string.error_router);
                        getIPFromServer();
                    }
                });
        request.setHeaders(headers.build());
        requestQueue.add(request);
    }

    /**
     * 显示Toast
     *
     * @param resourceId 资源id
     */
    private void showToast(int resourceId) {
        Toast.makeText(fragment.getActivity(), resourceId, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示消息提示
     *
     * @param resourceId 资源id，标题
     * @param log        内容
     */
    private void showMessage(int resourceId, String log) {
        Resources res = fragment.getResources();
        new AlertDialog.Builder(fragment.getActivity())
                .setTitle(res.getString(resourceId))
                .setMessage(log)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    private void showProgress(int resourceId) {
        String content = fragment.getResources().getString(resourceId);
        ProgressDialog progressDialog = ProgressDialog.show(fragment.getActivity(),
                "", content, true, true);
        progressDialogs.put(resourceId, progressDialog);
    }

    private void dismissProgress(int resourceId) {
        ProgressDialog progressDialog;
        progressDialog = progressDialogs.get(resourceId);
        if(progressDialog != null) {
            if(progressDialog.isShowing()) progressDialog.dismiss();
            progressDialogs.remove(resourceId);
        }
    }

    /**
     * 解析json中code含义
     *
     * @param code 返回结果码
     * @return 含义
     */
    private String parseCode(int code) {
        switch (code) {
            case 0:
                return "成功";
            case 1:
                return "其他原因认证拒绝";
            case 2:
                return "用户连接已经存在";
            case 3:
                return "接入服器务繁忙，稍后重试";
            case 4:
                return "未知错误";
            case 6:
                return "认证响应超时";
            case 7:
                return "捕获用户网络地址错误";
            case 8:
                return "服务器网络连接异常";
            case 9:
                return "认证服务脚本执行异常";
            case 10:
                return "校验码错误";
            case 11:
                return "您的密码相对简单，帐号存在被盗风险，请及时修改成强度高的密码";
            case 12:
                return "无法获取您的网络地址,请输入任意其它网站从网关处导航至本认证页面";
            case 13:
                return "无法获取您接入点设备地址，请输入任意其它网站从网关处导航至本认证页面";
            case 14:
                return "无法获取您套餐信息";
            case 16:
                return "请输入任意其它网站导航至本认证页面,并按正常PORTAL正常流程认证";
            case 17:
                return "连接已失效，请输入任意其它网站从网关处导航至本认证页面";
            default:
                return "未知错误";
        }
    }

    /**
     * 获取默认获取IP方式
     */
    public int getIpAccessMethod() {
        return ipAccessMethod;
    }

    /**
     * 修改默认获取IP的方式
     */
    public void setIpAccessMethod(int ipAccessMethod) {
        this.ipAccessMethod = ipAccessMethod;
    }

    public void setIfSaveIP(boolean saveIP) {
        this.ifSaveIP = saveIP;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }
}
