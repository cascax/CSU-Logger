package xyz.codeme.loginer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "LoginerMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        Fragment loginFragment = fragmentManager.findFragmentById(R.id.fragmentContainer);
        if(loginFragment == null) {
            loginFragment = new LoginFragment();
            loginFragment.setArguments(new Bundle());
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, loginFragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

}
