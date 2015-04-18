package xyz.codeme.szzn.http;

import java.util.Map;

import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;

public class FluentJsonRequest extends JsonObjectRequest
{
	private Map<String, String> headers = null;
	private Map<String, String> form = null;
	
	public FluentJsonRequest(int method, String url, JSONObject jsonRequest,
			Listener<JSONObject> listener, ErrorListener errorListener)
	{
		super(method, url, jsonRequest, listener, errorListener);
	}

	public void setHeaders(Map<String, String> headers)
	{
		this.headers = headers;
	}
	
	public void setForm(Map<String, String> form)
	{
		this.form = form;
	}
	
	@Override
	public Map<String, String> getHeaders() throws AuthFailureError
	{
		if(headers == null)
			return super.getHeaders();
		return headers;
	}
	
	@Override
	protected Map<String, String> getParams() throws AuthFailureError
	{
		if(form == null)
			return super.getParams();
		return form;
	}
}
