package xyz.codeme.loginer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;

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
    private LinearLayout mLayoutAccountInfo;
    private LinearLayout mLayoutTimeout;
    private RelativeLayout mLayoutAll;

    private HttpUtils mHttp;
    private SharedPreferences mPreferences;
    private long mLastLoginTime;
    private AccountInfo mAccountInfo;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mHandler = new MessageHandler();
        mHttp = new HttpUtils(this, mHandler);
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
        mLayoutAccountInfo = (LinearLayout) v.findViewById(R.id.layout_account_info);
        mLayoutTimeout = (LinearLayout) v.findViewById(R.id.layout_timeout);
        mLayoutAll  = (RelativeLayout) v.findViewById(R.id.layout_all);

        mInfoAccount = (TextView) v.findViewById(R.id.info_account);
        mInfoTime = (TextView) v.findViewById(R.id.info_time);
        mInfoUsed = (TextView) v.findViewById(R.id.info_used);
        mInfoTotal = (TextView) v.findViewById(R.id.info_total);
        mInfoRemained = (TextView) v.findViewById(R.id.info_remained);
        mInfoSchoolUsed = (TextView) v.findViewById(R.id.info_schoolused);
        mInfoMoney = (TextView) v.findViewById(R.id.info_money);
        mInfoOutTime = (TextView) v.findViewById(R.id.info_timeout);

        initOnClickListener();
        initFormPref();
        initRouter();
        if (! mPreferences.getBoolean("if_save_ip", true))
            mHttp.setIfSaveIP(false);
        if (! mPreferences.getBoolean("if_show_timeout", true))
            mLayoutTimeout.setVisibility(View.GONE);
        mHttp.getIP();
        initRestOfTime();
        initAnimation();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        restoreStateFromArguments();
        Toolbar toolbar = ((MainActivity)getActivity()).getToolbar();
        toolbar.setTitle(R.string.app_name);
        toolbar.setNavigationIcon(null);
        mHttp.isConnected();
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
        if (mAccountInfo != null) {
            Bundle state = mAccountInfo.parseBundle();
            s.putBundle("accountState", state);
        }
    }

    private void restoreStateFromArguments() {
        Bundle s = getArguments();
        Bundle state = s.getBundle("accountState");
        if(state != null) {
            showAccountInformation(new AccountInfo(state));
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
        mEditAccount.setText(mPreferences.getString("user", ""));
        mEditPassword.setText(mPreferences.getString("password", ""));
    }

    /**
     * 获取路由器ip页,referer,cookie,ip匹配正则参数，并配置
     */
    private void initRouter() {
        if (!mPreferences.getBoolean("use_router", false))
            return;
        String routerURL, routerReferer, routerCookie, routerReg;
        routerURL = mPreferences.getString("router_url", getString(R.string.router_default_url));
        routerReferer = mPreferences.getString("router_referer",
                getString(R.string.router_default_referer));
        routerReg = mPreferences.getString("router_reg", getString(R.string.router_default_reg));

        // 装载cookie
        routerCookie = mPreferences.getString("router_admin", getString(R.string.router_default_admin))
                + ":"
                + mPreferences.getString("router_password", getString(R.string.router_default_password));
        routerCookie = Base64.encodeToString(routerCookie.getBytes(), Base64.DEFAULT);
        try {
            routerCookie = "Authorization=Basic%20"
                    + URLEncoder.encode(routerCookie.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(MainActivity.TAG, "router端获取IP失败，cookie组合失败");
            Toast.makeText(getActivity(), R.string.error_router, Toast.LENGTH_SHORT).show();
            return;
        }

        mHttp.routerConfigure(routerURL, routerReferer, routerCookie, routerReg);
    }

    private void initRestOfTime() {
        mLastLoginTime = mPreferences.getLong("lastLogin", 0);
        if (mLastLoginTime != 0) {
            showLogoutTime(mLastLoginTime);
        }
    }

    private void initAnimation() {
        // 用户信息面板进入动画
        mLayoutAccountInfo.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int infoHeight = mLayoutAccountInfo.getHeight();
                        int allHeight = mLayoutAll.getHeight();
                        float rate = infoHeight * -1.0f / allHeight;
                        TranslateAnimation translateAnimation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0f,
                                Animation.RELATIVE_TO_SELF, 0f,
                                Animation.RELATIVE_TO_SELF, rate,
                                Animation.ABSOLUTE, 0f
                        );
                        translateAnimation.setDuration(600);
                        mLayoutAll.setAnimation(translateAnimation);
                        if(mLayoutAccountInfo.getVisibility() != View.GONE)
                            mLayoutAccountInfo.getViewTreeObserver()
                                    .removeGlobalOnLayoutListener(this);
                    }
                });
    }

    private void showLogoutTime(long lastLogin) {
        if(mLayoutTimeout.getVisibility() != View.VISIBLE)
            return;
        lastLogin += 43200000;
        if (Calendar.getInstance().getTimeInMillis() > lastLogin)
            return;
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(lastLogin);
        mInfoOutTime.setText(AccountInfo.parseDate(time));
    }

    /**
     * 刷新重新获取IP
     */
    public void refreshIP() {
        mHttp.getIP();
    }

    /**
     * 提交
     */
    public void submit() {
        int selected = (int) mSpinnerMethod.getSelectedItemId();
        String user, password, ip;
        user = mEditAccount.getText().toString();
        password = mEditPassword.getText().toString();
        password = RSAEncrypt.newInstance().encryptedString(password);
        ip = mEditIP.getText().toString();

        switch (selected) {
            case 0:
                mHttp.relogin(user, password, ip);
                break;
            case 1:
                mHttp.login(user, password, ip);
                break;
            case 2:
                mHttp.logout(ip);
                break;
        }
    }

    public void saveForm() {
        SharedPreferences.Editor editor = mPreferences.edit();
        mLastLoginTime = Calendar.getInstance().getTimeInMillis();
        editor.putLong("lastLogin", mLastLoginTime);
        if (mCheckSave.isChecked()) {
            editor.putString("user", mEditAccount.getText().toString());
            editor.putString("password", mEditPassword.getText().toString());
        }
        editor.apply();
        showLogoutTime(mLastLoginTime);
    }

    public void showAccountInformation(AccountInfo accountInfo) {
        this.mAccountInfo = accountInfo;
        mInfoAccount.setText(accountInfo.getUser());
        mInfoRemained.setText(Double.toString(
                Math.round(accountInfo.getPublicRemained() / 10.24) / 100.0));
        mInfoUsed.setText(Double.toString(
                Math.round(accountInfo.getPublicUsed() / 10.24) / 100.0));
        mInfoTotal.setText(Double.toString(
                Math.round(accountInfo.getPublicTotal() / 10.24) / 100.0) + " GB");
        mInfoMoney.setText(Double.toString(accountInfo.getAccount()));
        mInfoSchoolUsed.setText(Double.toString(accountInfo.getSchoolUsed()) + " MB");
        mInfoTime.setText(accountInfo.getTime());

        mLayoutAccountInfo.setVisibility(View.VISIBLE);
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
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.fragmentContainer, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.action_ip:
                mHttp.getLastIP(mEditAccount.getText().toString());
                mSpinnerMethod.setSelection(2);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MessageHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case HttpUtils.CONNECTED_TRUE:
                    mSpinnerMethod.setSelection(0);
                    break;
                case HttpUtils.CONNECTED_FALSE:
                    mSpinnerMethod.setSelection(1);
                    if (mPreferences.getBoolean("if_auto_login", false)
                            && mEditIP.getText().toString().length()  > 0 ) {
                        submit();
                    }
                    break;
                case HttpUtils.LONIN_SUCCESS:
                    saveForm();
                    break;
                case HttpUtils.GET_IP_SUCCESS:
                    showIP(msg.getData().getString("IP"));
                    break;
                case HttpUtils.GET_ACCOUNT_SUCCESS:
                    showAccountInformation(new AccountInfo(msg.getData()));
                    break;
            }
        }
    };
}
