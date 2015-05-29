package xyz.codeme.szzn.http;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AccountInfo {
    public final static int USE_MB = 0x0506;
    public final static int USE_GB = 0x0625;

    private int flowUnit = USE_MB;

    private String user = "";           // 用户名
    private Calendar time = null;       // 截止日期
    private String timeStr = null;      // 截止日期字符串形式
    private double publicTotal = 0;     // 总流量(公网)
    private double publicUsed = 0;      // 已用流量(公网)
    private double publicRemained = 0;  // 剩余流量(公网)
    private double schoolUsed = 0;      // 已用流量(校园网)
    private double account = 0;         // 剩余金额

    public AccountInfo(JSONObject jsonObj) throws JSONException {
        user = jsonObj.getString("account");
        publicTotal = jsonObj.getDouble("totalflow");
        publicUsed = jsonObj.getDouble("usedflow");
        publicRemained = jsonObj.getDouble("surplusflow");
        schoolUsed = jsonObj.getDouble("userSchoolOctets");
        account = jsonObj.getDouble("surplusmoney");

        String date = jsonObj.getString("lastupdate");
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        time = Calendar.getInstance();
        time.setTime(dateFormat.parse(date, new ParsePosition(0)));
    }

    public AccountInfo(Bundle state) {
        this.user = state.getString("User");
        this.timeStr = state.getString("Time");
        double[] rate = state.getDoubleArray("Rate");
        this.publicTotal = rate[0];
        this.publicUsed = rate[1];
        this.publicRemained = rate[2];
        this.schoolUsed = rate[3];
        this.account = rate[4];
    }

    public Bundle parseBundle() {
        Bundle state = new Bundle();
        state.putString("User", user);
        state.putString("Time", getTime());
        state.putDoubleArray("Rate", new double[]{
                publicTotal,
                publicUsed,
                publicRemained,
                schoolUsed,
                account
        });
        return state;
    }

    public static String parseDate(Calendar time) {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        String format;
        if(time.get(Calendar.DAY_OF_YEAR) == today + 1)
            format = "明天HH:mm";
        else if(time.get(Calendar.DAY_OF_YEAR) == today)
            format = "今天HH:mm";
        else if(time.get(Calendar.DAY_OF_YEAR) == today - 1)
            format = "昨天HH:mm";
        else
            format = "yyyy-MM-dd HH:mm";

        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(time.getTime());
    }

    public String getUser() {
        return user;
    }

    public String getTime() {
        if(timeStr != null)
            return timeStr;
        if(time == null)
            return "";
        timeStr = AccountInfo.parseDate(this.time);
        return timeStr;
    }

    /**
     * 转换流量单位MB到GB 保留两位小数
     * @param flow 流量MB
     * @return 流量GB
     */
    private double translateMBtoGB(double flow) {
        return Math.round(flow / 10.24) / 100.0;
    }

    public double getPublicTotal() {
        if(flowUnit == USE_GB) {
            return translateMBtoGB(publicTotal);
        }
        return publicTotal;
    }

    public double getPublicUsed() {
        if(flowUnit == USE_GB) {
            return translateMBtoGB(publicUsed);
        }
        return publicUsed;
    }

    public double getPublicRemained() {
        if(flowUnit == USE_GB) {
            return translateMBtoGB(publicRemained);
        }
        return publicRemained;
    }

    public double getSchoolUsed() {
        if(flowUnit == USE_GB) {
            return translateMBtoGB(schoolUsed);
        }
        return schoolUsed;
    }

    public double getAccount() {
        return account;
    }

    public void setFlowUnit(int flowUnit) {
        this.flowUnit = flowUnit;
    }

    public int getFlowUnit() {
        return flowUnit;
    }
}
