package xyz.codeme.szzn.http;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

public class FluentJsonRequest extends Request<JSONObject>
{
	private Map<String, String> headers = null;
	private Map<String, String> form = null;
    private Listener<JSONObject> listener;
	
	public FluentJsonRequest(String url, Map<String, String> form,
			Listener<JSONObject> listener, ErrorListener errorListener) {
		super(Request.Method.POST, url, errorListener);
		this.listener = listener;
		this.form = form;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public void setForm(Map<String, String> form) {
		this.form = form;
	}
	
	@Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,HttpHeaderParser.parseCharset(response.headers));
                 
            return Response.success(new JSONObject(jsonString),HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
	
	@Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
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
