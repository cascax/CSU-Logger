package xyz.codeme.szzn.http;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import xyz.codeme.loginer.MainActivity;
import xyz.codeme.loginer.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

/**
 * 数字中南登陆网络交互类
 * 
 * 首先获取IP，根据用户名/加密后的密码/IP，可登陆/登出/重新登陆(先登出再登陆)。
 * session为内部管理，过期后重登即可重新获取。
 * 登陆操作后可获取账户信息。
 * 
 * @author Msir
 * @date 2015/3
 *
 */
public class HttpUtils
{
	public final static int IP_FROM_SERVER = 0xac01;
	public final static int IP_FROM_ROUTER = 0xac02;
	public final static int ERROR_NOIP_FROM_SERVER = 0xace1;
	public final static int ERROR_NOIP_FROM_ROUTER = 0xace2;
	public final static int ERROR_NOIP = 0xace3;
	public final static int ERROR_IP_CONFIG = 0xace4;
	public final static int ERROR_LOGOUT = 0xace5;
	public final static int ERROR_LOGIN = 0xace6;
	public final static int NO_ERROR = 0;
	
	private final static String loginUrl = "http://61.137.86.87:8080/portalNat444/AccessServices/login";
	private final static String logoutUrl = "http://61.137.86.87:8080/portalNat444/AccessServices/logout";
	private final static String mainUrl = "http://61.137.86.87:8080/portalNat444/index.jsp";
	private final static String showUrl = "http://61.137.86.87:8080/portalNat444/main2.jsp";
	// 192.168.56.1/sz/ szzn.sinaapp.com/ codeme.xyz/api/
	private final static String saveUrl = "http://codeme.xyz/api/saveip.php";
	private final static String getipUrl = "http://codeme.xyz/api/getip.php";
	
	private final MainActivity activity;
	RequestQueue requestQueue;
	
	private int ipAccessMethod = IP_FROM_SERVER;
	private String routerURL = "";
	private String routerReferer = "";
	private String routerCookie = "";
	private String routerReg = "";
	private String session;
	private int errorCode = NO_ERROR;
	private String log = "";
	private boolean ifConnected = false;

	public HttpUtils(MainActivity activity)
	{
		this.activity = activity;
		this.requestQueue = Volley.newRequestQueue(activity);
		this.RefreshSession();
	}
	/**
	 * 路由器配置
	 * @param routerURL
	 * 		路由管理页
	 * @param routerReferer
	 * 		路由管理头部Referrer
	 * @param routerCookie
	 * 		路由登陆Cookie
	 * @param routerReg
	 * 		匹配IP地址正则表达式
	 */
	public void routerConfigure(String routerURL, String routerReferer,
			String routerCookie, String routerReg)
	{
		ipAccessMethod = IP_FROM_ROUTER;
		this.routerURL = routerURL;
		this.routerReferer = routerReferer;
		this.routerCookie = routerCookie;
		this.routerReg = routerReg;
	}
	/**
	 * 登陆
	 * @param user
	 * @param password
	 * @param IP
	 */
	public void login(String user, String password, String IP)
	{
		KeyValuePairs form = KeyValuePairs.create()
				.add("accountID", user + "@zndx.inter")
				.add("password", password)
				.add("brasAddress", "59df7586")
				.add("userIntranetAddress", IP);
		KeyValuePairs headers = KeyValuePairs.create()
				.add("Referer", HttpUtils.mainUrl)
				.add("Cookie", "JSESSIONID=" + this.session);
		
		final ProgressDialog progressDialog = ProgressDialog.show(activity, "Loading...", "正在登陆"); 
		
		FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.loginUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try
						{
                			int resultCode = jsonObj.getInt("resultCode");
                			if(resultCode == 0)
                			{
                				errorCode = NO_ERROR;
                				log = parseCode(resultCode);
                				ifConnected = true;
                				showToast(R.string.success_login);
                				return;
                			}
                			errorCode = ERROR_LOGIN;
                			log = "Login:" + parseCode(resultCode) + "," + jsonObj.getString("resultDescribe");
                			showToast(R.string.error_login);
                			Log.w(MainActivity.TAG, log);
						}
                        catch (JSONException e)
						{
							e.printStackTrace();
							showToast(R.string.error_login);
						}
                        finally
                        {
                        	if (progressDialog.isShowing() && progressDialog != null)
                            	progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
                    	if (progressDialog.isShowing() && progressDialog != null)
                        	progressDialog.dismiss();
						showToast(R.string.error_login);
                    }
                });
        jsonRequest.setHeaders(headers.build());
        
        requestQueue.add(jsonRequest);
	}
	
	/**
	 * 登出
	 * @param IP
	 */
	public void logout(String IP)
	{
		KeyValuePairs form = KeyValuePairs.create()
				.add("brasAddress", "59df7586")
				.add("userIntranetAddress", IP);
		KeyValuePairs headers = KeyValuePairs.create()
				.add("Referer", HttpUtils.showUrl);
		
		final ProgressDialog progressDialog = ProgressDialog.show(activity, "Loading...", "正在登出"); 
		
		FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.logoutUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try
						{
                			int resultCode = jsonObj.getInt("resultCode");
                			if(resultCode == 0)
                			{
                				errorCode = NO_ERROR;
                				log = parseCode(resultCode);
                				ifConnected = false;
                				showToast(R.string.success_logout);
                				return;
                			}
                			errorCode = ERROR_LOGOUT;
                			log = parseCode(resultCode) + "," + jsonObj.getString("resultDescribe");
                			showToast(R.string.error_logout);
						}
                        catch (JSONException e)
						{
							e.printStackTrace();
							showToast(R.string.error_logout);
						}
                        finally
                        {
                        	if (progressDialog.isShowing() && progressDialog != null)
                            	progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
                    	if (progressDialog.isShowing() && progressDialog != null)
                        	progressDialog.dismiss();
						showToast(R.string.error_logout);
                    }
                });
        jsonRequest.setHeaders(headers.build());
        
        requestQueue.add(jsonRequest);
	}
	/**
	 * 重新登陆
	 * @param user
	 * @param password
	 * @param IP
	 */
	public void relogin(String user, String password, String IP)
	{
		this.RefreshSession();
		logout(IP);
		login(user, password, IP);
	}
	
	/**
	 * 获取IP地址
	 */
	public void getIP()
	{
		switch(this.ipAccessMethod)
		{
			case IP_FROM_SERVER: getIPFromServer();
			case IP_FROM_ROUTER: getIPFromRouter();
		}
	}
	/**
	 * 上传保存IP
	 * @param user
	 * @param IP
	 */
	public void saveIP(String user, String IP)
	{
		KeyValuePairs form = KeyValuePairs.create()
				.add("user", user)
				.add("ip", IP);
		
		FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.saveUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try
						{
                			int resultCode = jsonObj.getInt("code");
                			if(resultCode == 0)
                				errorCode = NO_ERROR;
                			else
                				log = jsonObj.getInt("code") + ":" + jsonObj.getString("msg");
						}
                        catch (JSONException e) {}
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {}
                });
        
        requestQueue.add(jsonRequest);
	}

	/**
	 * 获取上次登录IP
	 * @param user
	 */
	public void getLastIP(String user)
	{
		KeyValuePairs form = KeyValuePairs.create()
				.add("user", user);
		
		final ProgressDialog progressDialog = ProgressDialog.show(activity, "Loading...", "正在获取"); 
		
		FluentJsonRequest jsonRequest = new FluentJsonRequest(
                HttpUtils.getipUrl,
                form.build(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObj) {
                        try
						{
                			int resultCode = jsonObj.getInt("code");
                			if(resultCode == 0)
                			{
                				String ip = jsonObj.getString("ip");
//                				String time = jsonObj.getString("time");
                				activity.getEditIP().setText(ip);
                			}
                			else
                				log = jsonObj.getInt("code") + ":" + jsonObj.getString("msg");
						}
                        catch (JSONException e)
						{
							e.printStackTrace();
							showToast(R.string.error_ip);
						}
                        finally
                        {
                        	if (progressDialog.isShowing() && progressDialog != null)
                            	progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
                    	if (progressDialog.isShowing() && progressDialog != null)
                        	progressDialog.dismiss();
						showToast(R.string.error_ip);
                    }
                });
		
        requestQueue.add(jsonRequest);
	}
	/**
	 * 获取用户信息
	 */
	public void getInformation()
	{
		KeyValuePairs headers = KeyValuePairs.create()
				.add("Referer", HttpUtils.mainUrl)
				.add("Cookie", "JSESSIONID=" + this.session);
		
		FluentStringRequest request = new FluentStringRequest(
				Request.Method.GET,
				HttpUtils.showUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String content) {
                    	AccountInfo account = new AccountInfo(content);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
						showToast(R.string.error_account);
                    }
                });
		request.setHeaders(headers.build());
        requestQueue.add(request);
	}
	/**
	 * 从数字中南获取IP
	 */
	private void getIPFromServer()
	{
		FluentStringRequest request = new FluentStringRequest(
				Request.Method.GET,
				"http://wap.baidu.com",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String content) {
                    	if(content.indexOf("百度") > 0) // 当前在线
                		{
                			ifConnected = true;
                			if(routerURL.length() > 0)
                				getIPFromRouter();
                		}
                		
                		Pattern pattern = Pattern.compile("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
                		Matcher match = pattern.matcher(content);
                		if(match.find())
                			activity.getEditIP().setText(match.group(0));
                		errorCode = ERROR_NOIP_FROM_SERVER;
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
						showToast(R.string.error_ip);
                    }
                });

        requestQueue.add(request);
        
//				.connectTimeout(1000)
//                .socketTimeout(1000);
	}
	/**
	 * 根据配置从路由器web管理页面获取当前IP
	 */
	private void getIPFromRouter()
	{
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
                		if(match.find())
                			activity.getEditIP().setText(match.group(0));
                		errorCode = ERROR_NOIP_FROM_ROUTER;
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
						showToast(R.string.error_ip);
                    }
                });
		request.setHeaders(headers.build());
        requestQueue.add(request);
	}
	/**
	 * 获取整个过程session
	 */
	private void RefreshSession()
	{
		FluentStringRequest request = new FluentStringRequest(
				Request.Method.GET,
				HttpUtils.mainUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String headers) {
        				Pattern pattern = Pattern.compile("JSESSIONID=([^;]+);");
        				Matcher match = pattern.matcher(headers);
        				if(match.find())
        					session = match.group(1);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0)
                    {
						showToast(R.string.error_ip);
                    }
                });
        requestQueue.add(request);
	}
	
	/**
	 * 显示Toast
	 * @param resourceId
	 */
	private void showToast(int resourceId)
	{
		Toast.makeText(activity, resourceId, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 解析json中code含义
	 * @param code
	 * @return
	 */
	private String parseCode(int code)
	{
		switch(code)
		{
			case 0:return "成功";
			case 1:return "其他原因认证拒绝";
			case 2:return "用户连接已经存在";
			case 3:return "接入服器务繁忙，稍后重试";
			case 4:return "未知错误";
			case 6:return "认证响应超时";
			case 7:return "捕获用户网络地址错误";
			case 8:return "服务器网络连接异常";
			case 9:return "认证服务脚本执行异常";
			case 10:return "校验码错误";
			case 11:return "您的密码相对简单，帐号存在被盗风险，请及时修改成强度高的密码";
			case 12:return "无法获取您的网络地址,请输入任意其它网站从网关处导航至本认证页面";
			case 13:return "无法获取您接入点设备地址，请输入任意其它网站从网关处导航至本认证页面";
			case 14:return "无法获取您套餐信息";
			case 16:return "请输入任意其它网站导航至本认证页面,并按正常PORTAL正常流程认证";
			case 17:return "连接已失效，请输入任意其它网站从网关处导航至本认证页面";
			default:return "未知错误";
		}
	}
	/**
	 * 获取错误信息
	 * @return
	 */
	public String getError()
	{
		switch(this.errorCode)
		{
			case NO_ERROR:
				return "";
			case ERROR_NOIP_FROM_SERVER:
				return "无法从数字中南获取IP";
			case ERROR_NOIP_FROM_ROUTER:
				return "无法从路由器获取IP";
			case ERROR_NOIP:
				return "未知原因无法获取IP";
			case ERROR_IP_CONFIG:
				return "IP获取配置错误";
			case ERROR_LOGOUT:
				return "登出失败";
			case ERROR_LOGIN:
				return "登陆失败";
			default:
				return "未知错误";
		}
	}
	/**
	 * 获取登陆/登出日志信息
	 * @return
	 */
	public String getLog()
	{
		return log;
	}
	/**
	 * 获取当前状态(可能不准确)
	 * @return
	 */
	public boolean isIfConnected()
	{
		return ifConnected;
	}
	/**
	 * 获取默认获取IP方式
	 * @return
	 */
	public int getIpAccessMethod()
	{
		return ipAccessMethod;
	}
	/**
	 * 修改默认获取IP的方式
	 * @param ipAccessMethod
	 */
	public void setIpAccessMethod(int ipAccessMethod)
	{
		this.ipAccessMethod = ipAccessMethod;
	}
}
