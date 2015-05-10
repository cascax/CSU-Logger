package xyz.codeme.loginer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private static final String TAG = "LoginerLogin";

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
    private AccountInfo account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
        if (!preferences.getBoolean("if_save_ip", true))
            http.setIfSaveIP(false);
        http.getIP();
        initRestOfTime();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        restoreStateFromArguments();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveStateToArguments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveStateToArguments();
    }

    private void saveStateToArguments() {
        Bundle s = getArguments();
        Bundle state = new Bundle();
        if (account != null) {
            state.putString("User", account.getUser());
            state.putString("Time", account.getTime());
            state.putDoubleArray("Rate", new double[]{
                    account.getPublicTotal(),
                    account.getPublicUsed(),
                    account.getPublicRemained(),
                    account.getSchoolUsed(),
                    account.getAccount()
            });
            s.putBundle("accountState", state);
        }
    }

    private void restoreStateFromArguments() {
        Bundle s = getArguments();
        Bundle state = s.getBundle("accountState");
        if(state != null) {
            showAccountInformation(new AccountInfo(
                    state.getString("User"),
                    state.getString("Time"),
                    state.getDoubleArray("Rate")
            ));
        }
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

    /**
     * 获取路由器ip页,referer,cookie,ip匹配正则参数，并配置
     */
    private void initRouter() {
        if (!preferences.getBoolean("use_router", false))
            return;
        String routerURL, routerReferer, routerCookie, routerReg;
        routerURL = preferences.getString("router_url", getString(R.string.router_default_url));
        routerReferer = preferences.getString("router_referer",
                getString(R.string.router_default_referer));
        routerReg = preferences.getString("router_reg", getString(R.string.router_default_reg));

        // 装载cookie
        routerCookie = preferences.getString("router_admin", getString(R.string.router_default_admin))
                + ":"
                + preferences.getString("router_password", getString(R.string.router_default_password));
        routerCookie = Base64.encodeToString(routerCookie.getBytes(), Base64.DEFAULT);
        try {
            routerCookie = "Authorization=Basic%20"
                    + URLEncoder.encode(routerCookie.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "router端获取IP失败，cookie组合失败");
            Toast.makeText(getActivity(), R.string.error_router, Toast.LENGTH_SHORT).show();
            return;
        }

        http.routerConfigure(routerURL, routerReferer, routerCookie, routerReg);
    }

    private void initRestOfTime() {
        lastLoginTime = preferences.getLong("lastLogin", 0);
        if (lastLoginTime != 0) {
            showLogoutTime(lastLoginTime);
        }
    }

    private void showLogoutTime(long lastLogin) {
        lastLogin += 43200000;
        if (Calendar.getInstance().getTimeInMillis() > lastLogin)
            return;
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

        switch (selected) {
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
        if (mCheckSave.isChecked()) {
            editor.putString("user", mEditAccount.getText().toString());
            editor.putString("password", mEditPassword.getText().toString());
        }
        editor.apply();
        showLogoutTime(lastLoginTime);
    }

    public void showAccountInformation(AccountInfo account) {
        this.account = account;
        mInfoAccount.setText(account.getUser());
        mInfoRemained.setText(Double.toString(account.getPublicRemained()) + " MB");
        mInfoUsed.setText(Double.toString(account.getPublicUsed()) + " MB");
        mInfoTotal.setText(Double.toString(account.getPublicTotal()) + " MB");
        mInfoMoney.setText(Double.toString(account.getAccount()));
        mInfoSchoolUsed.setText(Double.toString(account.getSchoolUsed()) + " MB");
        mInfoTime.setText(account.getTime());
    }

    public void showIP(String ip) {
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
//                Intent i = new Intent(getActivity(), SettingsActivity.class);
//                startActivity(i);
                FragmentManager fragmentManager = getFragmentManager();
                Fragment settingsFragment = new SettingsFragment();
                fragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.fragmentContainer, settingsFragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.action_ip:
                http.getLastIP(mEditAccount.getText().toString());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
