package xyz.codeme.loginer;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        setHasOptionsMenu(true);
        Log.d(MainActivity.TAG, "onCreate!");
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        ActionBar bar = getActivity().getActionBar();
//        if(bar != null) {
//            bar.setTitle(R.string.setting_title);
//            bar.setDisplayHomeAsUpEnabled(true);
//            Log.d(MainActivity.TAG, "bar!");
//        }
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                if(NavUtils.getParentActivityName(getActivity()) != null) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
