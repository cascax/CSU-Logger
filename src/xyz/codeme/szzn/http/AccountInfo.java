package xyz.codeme.szzn.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountInfo
{
	private String user = ""; // 用户名
	private String time = ""; // 截止至
	private double publicTotal = 0; // 总流量(公网)
	private double publicUsed = 0; // 已用流量(公网)
	private double publicRemained = 0; // 剩余流量(公网)
	private double schoolUsed = 0; // 已用流量(校园网)
	private double account = 0; // 剩余金额

	AccountInfo(String content)
	{
		Pattern pattern = Pattern
				.compile("尊敬的([\\w]+)用户，您本月截止至(.+)为止[\\s\\S]+总流量\\(公网\\):([\\d\\.]+)MB[\\s\\S]+用流量\\(公网\\):([\\d\\.]+)MB[\\s\\S]+余流量\\(公网\\):([\\d\\.]+)MB[\\s\\S]+用流量（校园网\\):([\\d\\.]+)MB[\\s\\S]+金额:([\\d\\.]+)元");
		Matcher match = pattern.matcher(content);
		if (match.find())
		{
			user = match.group(1);
			time = match.group(2);
			publicTotal = new Double(match.group(3));
			publicUsed = new Double(match.group(4));
			publicRemained = new Double(match.group(5));
			schoolUsed = new Double(match.group(6));
			account = new Double(match.group(7));
		}
	}

	public String getUser()
	{
		return user;
	}

	public String getTime()
	{
		return time;
	}

	public double getPublicTotal()
	{
		return publicTotal;
	}

	public double getPublicUsed()
	{
		return publicUsed;
	}

	public double getPublicRemained()
	{
		return publicRemained;
	}

	public double getSchoolUsed()
	{
		return schoolUsed;
	}

	public double getAccount()
	{
		return account;
	}
}
