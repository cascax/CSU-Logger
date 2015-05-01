package xyz.codeme.loginer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import xyz.codeme.szzn.http.AccountInfo;
import xyz.codeme.szzn.http.HttpUtils;
import xyz.codeme.szzn.rsa.RSAEncrypt;


public class MainActivity extends ActionBarActivity {
    public static String TAG = "LoginerMain";

    private Spinner mSpinnerMethod;
    private EditText mEditIP;
    private EditText mEditAccount;
    private EditText mEditPassword;
    private TextView mInfoAccount;
    private TextView mInfoTime;
    private TextView mInfoUsed;
    private TextView mInfoTotal;
    private TextView mInfoRemained;
    private TextView mInfoSchoolUsed;
    private TextView mInfoMoney;
    private TextView mInfoOutTime;
    private CheckBox mCheckSave;

    private HttpUtils http;
    private SharedPreferences preferences;
    private long lastLoginTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditIP = (EditText) findViewById(R.id.edit_ip);
        mEditAccount = (EditText) findViewById(R.id.edit_account);
        mEditPassword = (EditText) findViewById(R.id.edit_password);
        mSpinnerMethod = (Spinner) findViewById(R.id.spinner_method);
        mCheckSave = (CheckBox) findViewById(R.id.check_save);

        mInfoAccount = (TextView) findViewById(R.id.info_account);
        mInfoTime = (TextView) findViewById(R.id.info_time);
        mInfoUsed = (TextView) findViewById(R.id.info_used);
        mInfoTotal = (TextView) findViewById(R.id.info_total);
        mInfoRemained = (TextView) findViewById(R.id.info_remained);
        mInfoSchoolUsed = (TextView) findViewById(R.id.info_schoolused);
        mInfoMoney = (TextView) findViewById(R.id.info_money);
        mInfoOutTime = (TextView) findViewById(R.id.info_outtime);

        preferences = this.getBaseContext().getSharedPreferences("setting", MODE_PRIVATE);

        mEditAccount.setText(preferences.getString("user", ""));
        mEditPassword.setText(preferences.getString("password", ""));

        http = new HttpUtils(this);
        initRouter();
        http.getIP();
        intiRestOfTime();
    }

    private void initRouter() {
        String routerURL = preferences.getString("routerURL", "http://192.168.5.1/userRpm/StatusRpm.htm");
        if(routerURL.length() == 0) return;
        String routerReferer = preferences.getString("routerReferer", "http://192.168.5.1/");

        String routerCookie;
        routerCookie= preferences.getString("routerAdmin", "admin")
                + ":"
                + preferences.getString("routerPassword", "admin");
        routerCookie = Base64.encodeToString(routerCookie.getBytes(), Base64.DEFAULT);
        try {
            routerCookie = "Authorization=Basic%20"
                    + URLEncoder.encode(routerCookie.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "router端获取IP失败，cookie组合失败");
            Toast.makeText(this, R.string.error_router, Toast.LENGTH_SHORT).show();
            return;
        }
        String routerReg = preferences.getString("routerReg", "10\\.96\\.[1-9]\\d{0,2}\\.\\d{1,3}");

        http.routerConfigure(routerURL, routerReferer, routerCookie, routerReg);
    }

    private void intiRestOfTime() {
        lastLoginTime = preferences.getLong("lastLogin", 0);
        if(lastLoginTime != 0) {
            showLogoutTime(lastLoginTime);
        }
    }

    private void showLogoutTime(long lastLogin) {
        lastLogin += 43200000;
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(lastLogin);
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        mInfoOutTime.setText(dateFormat.format(time.getTime()));
    }

    /**
     * 刷新重新获取IP
     */
    public void refreshIP(View view) {
        http.getIP();
    }

    /**
     * 提交
     */
    public void submit(View view) {
        int selected = (int) mSpinnerMethod.getSelectedItemId();
        String account, password, ip;
        account = mEditAccount.getText().toString();
        password = mEditPassword.getText().toString();
        password = RSAEncrypt.newInstance().encryptedString(password);
        ip = mEditIP.getText().toString();

        switch(selected) {
            case 0:
                http.relogin(account, password, ip);
                break;
            case 1:
                http.login(account, password, ip);
                break;
            case 2:
                http.logout(ip);
                break;
        }
    }

    public void saveForm() {
        SharedPreferences.Editor editor = preferences.edit();
        lastLoginTime = Calendar.getInstance().getTimeInMillis();
        editor.putLong("lastLogin", lastLoginTime);
        if(mCheckSave.isChecked()) {
            editor.putString("user", mEditAccount.getText().toString());
            editor.putString("password", mEditPassword.getText().toString());
        }
        editor.apply();
        showLogoutTime(lastLoginTime);
    }

    public void showAccountInformation(AccountInfo account) {
        mInfoAccount.setText(account.getUser());
        mInfoRemained.setText(Double.toString(account.getPublicRemained()) + " MB");
        mInfoUsed.setText(Double.toString(account.getPublicUsed()) + " MB");
        mInfoTotal.setText(Double.toString(account.getPublicTotal()) + " MB");
        mInfoMoney.setText(Double.toString(account.getAccount()));
        mInfoSchoolUsed.setText(Double.toString(account.getSchoolUsed()) + " MB");
        mInfoTime.setText(account.getTime());
    }

    public void showIP(String ip)
    {
        mEditIP.setText(ip);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_ip:
                http.getLastIP(mEditAccount.getText().toString());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
