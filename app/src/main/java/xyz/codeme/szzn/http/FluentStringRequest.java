package xyz.codeme.szzn.http;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

public class FluentStringRequest extends StringRequest
{
	private Map<String, String> headers = null;
	private Map<String, String> form = null;
	private boolean ifGetCookie;

	public FluentStringRequest(int method, String url,
			Listener<String> listener, ErrorListener errorListener)
	{
		super(method, url, listener, errorListener);
		this.ifGetCookie = false;
	}

	public FluentStringRequest(int method, String url, boolean ifGetCookie,
			Listener<String> listener, ErrorListener errorListener)
	{
		super(method, url, listener, errorListener);
		this.ifGetCookie = ifGetCookie;
	}
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public void setForm(Map<String, String> form) {
		this.form = form;
	}
	
	@Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        String cookie = "";
        if(ifGetCookie)
        	cookie = "[" + response.headers.get("Set-Cookie") + "]";
        return Response.success(cookie + parsed, HttpHeaderParser.parseCacheHeaders(response));
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
