package xyz.codeme.loginer;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class LoginFragment extends Fragment {
    public static String TAG = "LoginerLogin";

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
    private ImageButton mButtonRefreshIP;
    private Button mButtonSubmit;

    private HttpUtils http;
    private SharedPreferences preferences;
    private long lastLoginTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preferences = getActivity().getSharedPreferences("setting", Activity.MODE_PRIVATE);
        http = new HttpUtils(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, parent, false);

        mEditIP = (EditText) v.findViewById(R.id.edit_ip);
        mEditAccount = (EditText) v.findViewById(R.id.edit_account);
        mEditPassword = (EditText) v.findViewById(R.id.edit_password);
        mSpinnerMethod = (Spinner) v.findViewById(R.id.spinner_method);
        mCheckSave = (CheckBox) v.findViewById(R.id.check_save);
        mButtonRefreshIP = (ImageButton) v.findViewById(R.id.btn_refresh_ip);
        mButtonSubmit = (Button) v.findViewById(R.id.btn_submit);

        mInfoAccount = (TextView) v.findViewById(R.id.info_account);
        mInfoTime = (TextView) v.findViewById(R.id.info_time);
        mInfoUsed = (TextView) v.findViewById(R.id.info_used);
        mInfoTotal = (TextView) v.findViewById(R.id.info_total);
        mInfoRemained = (TextView) v.findViewById(R.id.info_remained);
        mInfoSchoolUsed = (TextView) v.findViewById(R.id.info_schoolused);
        mInfoMoney = (TextView) v.findViewById(R.id.info_money);
        mInfoOutTime = (TextView) v.findViewById(R.id.info_outtime);

        initOnClickListener();
        initFormPref();
        initRouter();
        http.getIP();
        intiRestOfTime();

        return v;
    }

    public void initOnClickListener() {
        mButtonRefreshIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshIP();
            }
        });
        mButtonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    public void initFormPref() {
        mEditAccount.setText(preferences.getString("user", ""));
        mEditPassword.setText(preferences.getString("password", ""));
    }

    private void initRouter() {
        String routerURL = preferences.getString("routerURL", "http://192.168.5.1/userRpm/StatusRpm.htm");
        if(routerURL != null && routerURL.length() == 0) return;
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
            Toast.makeText(getActivity(), R.string.error_router, Toast.LENGTH_SHORT).show();
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
    public void refreshIP() {
        http.getIP();
    }

    /**
     * 提交
     */
    public void submit() {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
