package xyz.codeme.loginer;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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

import xyz.codeme.loginer.utils.CommonUtils;
import xyz.codeme.szzn.http.AccountInfo;
import xyz.codeme.szzn.http.HttpUtils;
import xyz.codeme.szzn.rsa.RSAEncrypt;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginerLogin";

    private Spinner mSpinnerMethod;
    private EditText mEditIP;
    private EditText mEditAccount;
    private EditText mEditPassword;
    private TextView mTextRemained;
    private TextView mInfoAccount;
    private TextView mInfoTime;
    private TextView mInfoUsed;
    private TextView mInfoUsedRight;
    private TextView mInfoTotal;
    private TextView mInfoRemained;
    private TextView mInfoRemainedRight;
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
        initFirstOpen();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, parent, false);

        mEditIP             = (EditText) v.findViewById(R.id.edit_ip);
        mEditAccount        = (EditText) v.findViewById(R.id.edit_account);
        mEditPassword       = (EditText) v.findViewById(R.id.edit_password);
        mSpinnerMethod      = (Spinner) v.findViewById(R.id.spinner_method);
        mCheckSave          = (CheckBox) v.findViewById(R.id.check_save);
        mButtonRefreshIP    = (ImageButton) v.findViewById(R.id.btn_refresh_ip);
        mButtonSubmit       = (Button) v.findViewById(R.id.btn_submit);
        mLayoutAccountInfo  = (LinearLayout) v.findViewById(R.id.layout_account_info);
        mLayoutTimeout      = (LinearLayout) v.findViewById(R.id.layout_timeout);
        mLayoutAll          = (RelativeLayout) v.findViewById(R.id.layout_all);

        mTextRemained       = (TextView) v.findViewById(R.id.text_remained);
        mInfoAccount        = (TextView) v.findViewById(R.id.info_account);
        mInfoTime           = (TextView) v.findViewById(R.id.info_time);
        mInfoUsed           = (TextView) v.findViewById(R.id.info_used);
        mInfoUsedRight      = (TextView) v.findViewById(R.id.info_used_right);
        mInfoTotal          = (TextView) v.findViewById(R.id.info_total);
        mInfoRemained       = (TextView) v.findViewById(R.id.info_remained);
        mInfoRemainedRight  = (TextView) v.findViewById(R.id.info_remained_right);
        mInfoSchoolUsed     = (TextView) v.findViewById(R.id.info_schoolused);
        mInfoMoney          = (TextView) v.findViewById(R.id.info_money);
        mInfoOutTime        = (TextView) v.findViewById(R.id.info_timeout);

        initViewsListener();
        initFormPref();
        initRestOfTime();
        initAnimation();
        initRouter();
        mHttp.getIP();
        mHttp.isConnected();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHttp.setIfSaveIP(mPreferences.getBoolean("if_save_ip", true));
        if (mPreferences.getBoolean("if_show_timeout", true))
            mLayoutTimeout.setVisibility(View.VISIBLE);
        else
            mLayoutTimeout.setVisibility(View.GONE);
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

    private void initViewsListener() {
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

    private void initFormPref() {
        mEditAccount.setText(mPreferences.getString("user", ""));
        mEditPassword.setText(mPreferences.getString("password", ""));
    }

    /**
     * 获取路由器ip页,referer,cookie,ip匹配正则参数，并配置
     */
    private void initRouter() {
        if (! mPreferences.getBoolean("use_router", false))
            return;
        String routerURL, routerReferer, routerCookie, routerReg;
        routerReferer = mPreferences.getString("router_referer",
                getString(R.string.router_default_referer));
        routerURL = routerReferer + mPreferences.getString("router_url",
                getString(R.string.router_default_url));
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

    private void initFirstOpen()
    {
        int userID = mPreferences.getInt("userID", 0);
        if(userID == 0) {
            mHttp.register(CommonUtils.getVersionCode(getActivity()));
        } else {
            mHttp.setUserID(userID);
        }
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

    private void saveForm() {
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

    /**
     * 计算显示账户信息并显示
     * @param accountInfo 账户信息
     */
    private void showAccountInformation(AccountInfo accountInfo) {
        this.mAccountInfo = accountInfo;
        accountInfo.setFlowUnit(AccountInfo.USE_GB);
        // 超出显示超出流量，未超出显示剩余流量
        if(accountInfo.getPublicRemained() <= 0.001) {
            double beyondFlow = accountInfo.getPublicUsed() - accountInfo.getPublicTotal();
            showFlow(mInfoRemained, mInfoRemainedRight, beyondFlow);
            mTextRemained.setText(R.string.info_beyond);
        } else {
            showFlow(mInfoRemained, mInfoRemainedRight, accountInfo.getPublicRemained());
            mTextRemained.setText(R.string.info_remained);
        }

        showFlow(mInfoUsed, mInfoUsedRight, accountInfo.getPublicUsed());
        mInfoTotal.setText(Double.toString(accountInfo.getPublicTotal()) + " GB");
        mInfoAccount.setText(accountInfo.getUser());
        mInfoMoney.setText(Double.toString(accountInfo.getAccount()) + " 元");
        mInfoSchoolUsed.setText(Double.toString(accountInfo.getSchoolUsed()) + " GB");
        mInfoTime.setText(accountInfo.getTime());

        mLayoutAccountInfo.setVisibility(View.VISIBLE);
    }

    /**
     * 分两部分显示流量
     * @param left  正数部分View
     * @param right 小数部分View
     * @param flow  流量
     */
    private void showFlow(TextView left, TextView right, double flow) {
        left.setText(Integer.toString((int)flow));
        right.setText(
                "."
                + Integer.toString((int)(flow * 100) % 100)
                + " GB"
        );
    }

    private void showIP(String ip) {
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
                Intent settings = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settings);
                return true;
            case R.id.action_ip:
                mHttp.closeWifi();
                mHttp.getLastIP(mEditAccount.getText().toString());
                mSpinnerMethod.setSelection(2);
                return true;
            case R.id.action_increment:
                Intent increment = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getResources().getString(R.string.increment_url))
                );
                startActivity(increment);
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
                case HttpUtils.LONIN_FAILED:
                    mSpinnerMethod.setSelection(1);
                    break;
                case HttpUtils.GET_IP_SUCCESS:
                    showIP(msg.getData().getString("IP"));
                    break;
                case HttpUtils.GET_LAST_IP_SUCCESS:
                    showIP(msg.getData().getString("IP"));
                    mHttp.openWifi();
                    break;
                case HttpUtils.GET_ACCOUNT_SUCCESS:
                    showAccountInformation(new AccountInfo(msg.getData()));
                    break;
                case HttpUtils.REGISTER_SUCCESS:
                    int userID = msg.getData().getInt("userID");
                    mHttp.setUserID(userID);
                    mPreferences.edit().putInt("userID", userID).apply();
                    break;
            }
        }
    };
}
