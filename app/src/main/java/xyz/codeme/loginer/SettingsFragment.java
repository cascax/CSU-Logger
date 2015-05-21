package xyz.codeme.loginer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import xyz.codeme.szzn.http.HttpUtils;

public class SettingsFragment extends PreferenceFragment {
    private Handler mHandle;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        setHasOptionsMenu(true);
        mHandle = new MessageHandler();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        switch(preference.getKey()) {
            case "check_update":
                new HttpUtils(this, mHandle).checkUpdate();
                return true;
            case "about_me":
                new AlertDialog.Builder(getActivity())
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(String.format(
                                getResources().getString(R.string.text_now_version),
                                getVersionName()
                        ))
                        .setMessage(R.string.text_about_me)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查是否需要更新
     * @param newVersion 从服务器获取的版本信息
     */
    private void checkUpdate(Bundle newVersion) {
        int versionCode = getVersionCode();
        if(versionCode != 0) {
            if(versionCode < newVersion.getInt("versionCode")) {
                String text = String.format(
                        getResources().getString(R.string.text_need_update),
                        newVersion.getString("versionName")
                );
                DialogInterface.OnClickListener okListener; // 确定按钮点击监听器
                okListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getResources().getString(R.string.update_url))
                        );
                        startActivity(i);
                    }
                };
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.text_title_update)
                        .setMessage(text)
                        .setPositiveButton(R.string.btn_ok, okListener)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            } else {
                Toast.makeText(getActivity(), R.string.text_latest_version, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * 获取版本号
     * @return int 版本号
     */
    private int getVersionCode() {
        try {
            PackageInfo info = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getVersionName() {
        try {
            PackageInfo info = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    private class MessageHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case HttpUtils.GET_VERSION:
                    checkUpdate(msg.getData());
                    break;
            }
        }
    };

}
