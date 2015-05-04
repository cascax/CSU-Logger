package xyz.codeme.loginer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
    public static String TAG = "LoginerMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        Fragment loginFragment = fragmentManager.findFragmentById(R.id.fragmentLogin);
        if(loginFragment == null) {
            loginFragment = new LoginFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentLogin, loginFragment)
                    .commit();
        }
    }

}
