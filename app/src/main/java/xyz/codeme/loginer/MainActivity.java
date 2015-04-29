package xyz.codeme.loginer;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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

    private HttpUtils http;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditIP = (EditText) findViewById(R.id.edit_ip);
        mEditAccount = (EditText) findViewById(R.id.edit_account);
        mEditPassword = (EditText) findViewById(R.id.edit_password);
        mSpinnerMethod = (Spinner) findViewById(R.id.spinner_method);

        mInfoAccount = (TextView) findViewById(R.id.info_account);
        mInfoTime = (TextView) findViewById(R.id.info_time);
        mInfoUsed = (TextView) findViewById(R.id.info_used);
        mInfoTotal = (TextView) findViewById(R.id.info_total);
        mInfoRemained = (TextView) findViewById(R.id.info_remained);
        mInfoSchoolUsed = (TextView) findViewById(R.id.info_schoolused);
        mInfoMoney = (TextView) findViewById(R.id.info_money);

        preferences = this.getBaseContext().getSharedPreferences("setting", MODE_PRIVATE);

        mEditAccount.setText(preferences.getString("user", ""));
        mEditPassword.setText(preferences.getString("password", ""));

        String routerURL = preferences.getString("routerURL", ""); //"http://192.168.5.1/userRpm/StatusRpm.htm";
        String routerReferer = preferences.getString("routerReferer", ""); //"http://192.168.5.1/";
        String routerCookie = preferences.getString("routerCookie", ""); //"Authorization=Basic%20YWRtaW46ODYyNjMzOQ%3D%3D";
        String routerReg = preferences.getString("routerReg", ""); //"10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
        http = new HttpUtils(this);
        if(routerURL.length() > 0)
            http.routerConfigure(routerURL, routerReferer, routerCookie, routerReg);
        http.getIP();
    }

    public void refreshIP(View view)
    {
        http.getIP();
    }

    public void submit(View view)
    {
        int selected = (int) mSpinnerMethod.getSelectedItemId();
        String account, password, ip;
        account = mEditAccount.getText().toString();
        password = mEditPassword.getText().toString();
        password = RSAEncrypt.newInstance().encryptedString(password);
        ip = mEditIP.getText().toString();

        switch(selected)
        {
            case 0:
                http.logout(ip);
                http.login(account, password, ip);
                break;
            case 1:
                http.login(account, password, ip);
                break;
            case 2:
                http.logout(ip);
                break;
        }
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

        switch (id)
        {
            case R.id.action_settings:
                return true;
            case R.id.action_ip:
                http.getLastIP("010909122723");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public EditText getEditIP()
    {
        return mEditIP;
    }

    public EditText getEditAccount()
    {
        return mEditAccount;
    }

    public EditText getEditPassword()
    {
        return mEditPassword;
    }

    public TextView getInfoAccount()
    {
        return mInfoAccount;
    }

    public TextView getInfoTime()
    {
        return mInfoTime;
    }

    public TextView getInfoUsed()
    {
        return mInfoUsed;
    }

    public TextView getInfoTotal()
    {
        return mInfoTotal;
    }

    public TextView getInfoRemained()
    {
        return mInfoRemained;
    }

    public TextView getInfoSchoolUsed()
    {
        return mInfoSchoolUsed;
    }

    public TextView getInfoMoney()
    {
        return mInfoMoney;
    }

    public SharedPreferences getPreferences()
    {
        return preferences;
    }
}
