package synet.controller.configuration;

import java.util.Map;

public interface Configuration {
	
	/**
	 * Get all the entries in a key/value pair
	 * 
	 * @return
	 */
	public Map<String, String> getAll();
	
	/**
	 * Add a property to the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, Object value);
	
	/**
	 * Remove all properties from the configuration
	 */
	public void clear();
	
	/**
	 * Remove a property from the configuration
	 * 
	 * @param key
	 */
	public void clearProperty(String key);
	
	/**
	 * Check if the configuration contains the specified key
	 * 
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key);
	
	/**
	 * Check if the configuration is empty
	 * 
	 * @return true if the configuration is empty
	 */
	public boolean isEmpty();
	
	/**
	 * Get a boolean associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key);
	
	/**
	 * Get a boolean associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public boolean getBoolean(String key, boolean defaultValue);
	
	/**
	 * Get a byte associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public byte getByte(String key);
	
	/**
	 * Get a byte associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public byte getByte(String key, byte defaultValue);
	
	/**
	 * Get a double associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public double getDouble(String key);
	
	/**
	 * Get a double associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public double getDouble(String key, double defaultValue);
	
	/**
	 * Get a int associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public int getInt(String key);
	
	/**
	 * Get a int associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public int getInt(String key, int defaultValue);
	
	/**
	 * Get a short associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public short getShort(String key);
	
	/**
	 * Get a short associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public short getShort(String key, short defaultValue);
	
	/**
	 * Get a long associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public long getLong(String key);
	
	/**
	 * Get a short associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public long getLong(String key, long defaultValue);
	
	/**
	 * Get a String associated with the given configuration key
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key);
	
	/**
	 * Get a String associated with the given configuration key.
	 * 
	 * @param key
	 * @param defaultValue if the key is not valid, the default value is used
	 * @return
	 */
	public String getString(String key, String defaultValue);
	
	/**
	 * Return a decorator Configuration containign every key from the current Configuration that starts with the specified prefix
	 * @param prefix
	 * @return
	 */
	public Configuration subset(String prefix);
}
