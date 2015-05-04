package xyz.codeme.loginer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        Fragment settingsFragment = fragmentManager.findFragmentById(R.id.fragmentContainer);
        if(settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, settingsFragment)
                    .commit();
        }
    }
}
