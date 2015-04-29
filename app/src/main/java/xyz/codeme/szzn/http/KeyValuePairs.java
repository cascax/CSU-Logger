package xyz.codeme.szzn.http;

import java.util.HashMap;
import java.util.Map;
/**
 * 构建表单
 * @author Msir
 *
 */
public class KeyValuePairs
{
	private final Map<String, String> params;
	
	public static KeyValuePairs create()
	{
		return new KeyValuePairs();
	}
	
	KeyValuePairs()
	{
		this.params = new HashMap<String, String>();
	}
	
	public KeyValuePairs add(String key, String value)
	{
		this.params.put(key, value);
		return this;
	}
	
	public Map<String, String> build()
	{
		return new HashMap<String, String>(this.params);
	}
}
