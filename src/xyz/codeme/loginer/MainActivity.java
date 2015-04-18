package xyz.codeme.loginer;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.codeme.szzn.http.HttpUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private Button mButtonSubmit;
	private Button mButtonGetIP;
	private Spinner mSpinnerMethod;
	private EditText mEditIP;
	private EditText mEditAccount;
	private EditText mEditPassword;
	
	private String routerURL;
	private String routerReferer;
	private String routerCookie;
	private String routerReg;
	private HttpUtils http;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mEditIP = (EditText) findViewById(R.id.edit_ip);
		mEditAccount = (EditText) findViewById(R.id.edit_account);
		mEditPassword = (EditText) findViewById(R.id.edit_password);
		mSpinnerMethod = (Spinner) findViewById(R.id.spinner_method);
		
		routerURL = "http://192.168.5.1/userRpm/StatusRpm.htm";
		routerReferer = "http://192.168.5.1/";
		routerCookie = "Authorization=Basic%20YWRtaW46ODYyNjMzOQ%3D%3D";
		routerReg = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
		http = new HttpUtils(this);
		http.routerConfigure(routerURL, routerReferer, routerCookie, routerReg);
	}
	
	public void submit(View view)
	{
		
	}
	
	public void getIP(View view)
	{
		http.getLastIP("010909122723");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
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
}
