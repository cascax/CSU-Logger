//package xyz.codeme.szzn;
//
//import xyz.codeme.szzn.http.AccountInfo;
//import xyz.codeme.szzn.http.HttpUtils;
//import xyz.codeme.szzn.rsa.RSAEncrypt;
//
//public class FluentUse implements Runnable
//{
//	private String routerURL;
//	private String routerReferer;
//	private String routerCookie;
//	private String routerReg;
//	private HttpUtils http;
//	
//	public FluentUse()
//	{
//		routerURL = "http://192.168.5.1/userRpm/StatusRpm.htm";
//		routerReferer = "http://192.168.5.1/";
//		routerCookie = "Authorization=Basic%20YWRtaW46ODYyNjMzOQ%3D%3D";
//		routerReg = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
//		http = new HttpUtils(routerURL, routerReferer, routerCookie, routerReg);
////		http = new HttpUtils();
//	}
//
//	public void login(String user, String password)
//	{
//		password = RSAEncrypt.newInstance().encryptedString(password);
//		String IP = http.getIP();
//		
//		// 登陆
//		if(http.login(user, password, IP))
//		{
//			System.out.println("登陆成功");
//			AccountInfo account = http.getInformation();
//			System.out.println("用户名" + account.getUser());
//			System.out.println("截止至" + account.getTime());
//			System.out.println("总流量(公网)" + account.getPublicTotal());
//			System.out.println("已用流量(公网)" + account.getPublicUsed());
//			System.out.println("剩余流量(公网)" + account.getPublicRemained());
//			System.out.println("已用流量(校园网)" + account.getSchoolUsed());
//			System.out.println("金额" + account.getAccount());
//			if(! http.saveIP(user, IP))
//				System.out.println(http.getLog());
//		}
//		else
//		{
//			System.out.println(http.getError() + ":" + http.getLog());
//		}
//	}
//	
//	public boolean logout()
//	{
//		String IP = http.getIP();
//		
//		// 登出
//		if(http.logout(IP))
//		{
//			System.out.println("登出成功");
//			return true;
//		}
//		else
//		{
//			System.out.println(http.getError() + ":" + http.getLog());
//			return false;
//		}
//	}
//	
//	public void relogin(String user, String password)
//	{
//		password = RSAEncrypt.newInstance().encryptedString(password);
//		String IP = http.getIP();
//		
//		// 重登
//		if(http.relogin(user, password, IP))
//		{
//			System.out.println("重登成功");
//			AccountInfo account = http.getInformation();
//			System.out.println("用户名" + account.getUser());
//			System.out.println("截止至" + account.getTime());
//			System.out.println("总流量(公网)" + account.getPublicTotal());
//			System.out.println("已用流量(公网)" + account.getPublicUsed());
//			System.out.println("剩余流量(公网)" + account.getPublicRemained());
//			System.out.println("已用流量(校园网)" + account.getSchoolUsed());
//			System.out.println("金额" + account.getAccount());
//			if(! http.saveIP(user, IP))
//				System.out.println(http.getLog());
//		}
//		else
//		{
//			System.out.println(http.getError() + ":" + http.getLog());
//		}
//	}
//
//	@Override
//	public void run()
//	{
//		
//		
//	}
//}
