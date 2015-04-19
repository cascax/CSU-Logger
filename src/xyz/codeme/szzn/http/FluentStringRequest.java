package xyz.codeme.szzn.http;

import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.StringRequest;

public class FluentStringRequest extends StringRequest
{
	private Map<String, String> headers = null;
	private Map<String, String> form = null;

	public FluentStringRequest(int method, String url,
			Listener<String> listener, ErrorListener errorListener)
	{
		super(method, url, listener, errorListener);
	}
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public void setForm(Map<String, String> form) {
		this.form = form;
	}
	
	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		if(headers == null)
			return super.getHeaders();
		return headers;
	}
	
	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		if(form == null)
			return super.getParams();
		return form;
	}

}
