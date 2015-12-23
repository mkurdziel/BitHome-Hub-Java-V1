/**
 * 
 */
package synet.controller.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import synet.controller.utils.Logger;
import synet.controller.utils.SysUtils;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

/**
 * @author kur57360
 *
 */
public class XMLConfiguration implements Configuration {

	private static final String C_XML_LASTUPDATED = "lastUpdated";

	private static final String C_XML_SEPARATOR = ".";

	private static final String C_XML_ROOT = "controller";

	private final static String TAG = "XMLConfiguration";
	private Document m_xmlDoc = null;
	private Element m_root = null;
	private String m_fileName;

	/**
	 * Initialize the XMLConfiguration with a file name. 
	 * 
	 * @param filename
	 */
	public XMLConfiguration(String filename)
	{
		File xmlFile = new File(filename);
		m_fileName = filename;
		if (xmlFile.exists())
		{
			Builder xmlBuilder = new Builder();
			try {
				m_xmlDoc = xmlBuilder.build(xmlFile);
				m_root = m_xmlDoc.getRootElement();
			} catch (ValidityException e) {
				Logger.e(TAG, "configuration file is invalid", e);
			} catch (ParsingException e) {
				Logger.e(TAG, "configuration file is unparsable", e);
			} catch (IOException e) {
				Logger.e(TAG, "configuration file cannot be read", e);
			}
		}

		if (m_root == null)
		{
			Logger.w(TAG, "could not open config file " + filename + ". Creating new config file.");
			m_root = new Element(C_XML_ROOT);
			m_xmlDoc = new Document(m_root);
			writeConfiguration();
		}
	}

	/**
	 * Initialize the XMLConfiguration with an XML Element. 
	 * 
	 * @param filename
	 * @throws Exception 
	 */
	public XMLConfiguration(Element element)
	{
		if (element != null)
		{
			m_root = element;
		}
		else
		{
			Logger.e(TAG, "Creating config with NULL Element!");
		}
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#addProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void addProperty(String key, Object value) {

		Logger.v(TAG, "adding property " + key + " = " + String.valueOf(value));
		String[] keyPath = splitPrefix(key);
		Element ele = getElementAndCreate(m_root, keyPath, 0);
		ele.removeChildren();
		ele.appendChild(String.valueOf(value));
	}

	/**
	 * Recursive function for getting an element from the path whilst creating it
	 * 
	 * @param element
	 * @param keyPath
	 * @param startIndex
	 * @return
	 */
	private Element getElementAndCreate(Element element, String[] keyPath, int startIndex) {
		if (startIndex < keyPath.length)
		{
			Element nextEl = element.getFirstChildElement(keyPath[startIndex]);
			// If the element was not found, create it
			if (nextEl == null)
			{
				Logger.v(TAG, keyPath[startIndex] + " is not found. Creating it");
				nextEl = new Element(keyPath[startIndex]);
				element.appendChild(nextEl);
			}
			return getElementAndCreate(nextEl, keyPath, ++startIndex);
		}
		return element;
	}

	/**
	 * Recursive function for getting an element from the path without creating
	 * 
	 * @param element
	 * @param keyPath
	 * @param startIndex
	 * @return
	 */
	private Element getElement(Element element, String[] keyPath, int startIndex) {
		if (startIndex < keyPath.length)
		{
			String key = keyPath[startIndex];

			Element nextEl = element.getFirstChildElement(key);
			// If the element was not found, create it
			if (nextEl == null)
			{
				return null;
			}
			return getElement(nextEl, keyPath, ++startIndex);
		}
		else if (keyPath.length > 0)
		{
			return (element.getLocalName().compareTo(keyPath[startIndex-1]) == 0) ?
					element : null;
		}
		return null;
	}
	/**
	 * Get the element 
	 * 
	 * @param element
	 * @param keyPath
	 * @return
	 */
	private Element getElement(String keyPath ) {
		return getElement(m_root, splitPrefix(keyPath), 0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#clear()
	 */
	@Override
	public void clear() {
		Logger.e(TAG, "clear not implemented");
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#clearProperty(java.lang.String)
	 */
	@Override
	public void clearProperty(String key) {
		Element ele = getElement(key);
		if (ele != null)
		{
			ele.getParent().removeChild(ele);
		}
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#containsKey(java.lang.String)
	 */
	@Override
	public boolean containsKey(String key) {
		return getElement(key) != null;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getBoolean(java.lang.String)
	 */
	@Override
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getBoolean(java.lang.String, boolean)
	 */
	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Boolean.parseBoolean(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getBoolean: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getByte(java.lang.String)
	 */
	@Override
	public byte getByte(String key) {
		return getByte(key, (byte)0x0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getByte(java.lang.String, byte)
	 */
	@Override
	public byte getByte(String key, byte defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Byte.parseByte(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getByte: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getDouble(java.lang.String)
	 */
	@Override
	public double getDouble(String key) {
		return getDouble(key, 0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getDouble(java.lang.String, double)
	 */
	@Override
	public double getDouble(String key, double defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Double.parseDouble(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getDouble: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getInt(java.lang.String)
	 */
	@Override
	public int getInt(String key) {
		return getInt(key, 0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getInt(java.lang.String, int)
	 */
	@Override
	public int getInt(String key, int defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Integer.parseInt(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getInt: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getLong(java.lang.String)
	 */
	@Override
	public long getLong(String key) {
		return getLong(key, 0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getLong(java.lang.String, long)
	 */
	@Override
	public long getLong(String key, long defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Long.parseLong(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getLong: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getShort(java.lang.String)
	 */
	@Override
	public short getShort(String key) {
		return getShort(key, (short)0);
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getShort(java.lang.String, short)
	 */
	@Override
	public short getShort(String key, short defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			try
			{
				return Short.parseShort(el.getValue());
			}
			catch(NumberFormatException e)
			{
				Logger.w(TAG, "getShort: " + key, e);
			}
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key) {
		return getString(key, "");
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getString(java.lang.String, java.lang.String)
	 */
	@Override
	public String getString(String key, String defaultValue) {
		Element el = getElement(key);

		if (el != null)
		{
			return el.getValue();
		}
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return m_root.getChildCount() == 0;
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#subset(java.lang.String)
	 */
	@Override
	public Configuration subset(String prefix) {
		Element subsetEle = getElementAndCreate(m_root, splitPrefix(prefix), 0);
		return new XMLConfiguration(subsetEle);
	}

	/**
	 * Split the prefix based on the separator
	 * 
	 * @param prefix
	 * @return
	 */
	private String[] splitPrefix(String prefix) {
		return prefix.split(Pattern.quote(C_XML_SEPARATOR));
	}

	/**
	 * Write the configuration back out to the file
	 */
	public void writeConfiguration()
	{
		if (m_xmlDoc != null)
		{
			updateLastUpdated();
			
			try {
				FileOutputStream fos = new FileOutputStream(m_fileName);
				Serializer output = new Serializer(fos, "ISO-8859-1");
				output.setIndent(2);
				output.write(m_xmlDoc);
				fos.close();

			} catch (FileNotFoundException e) {
				Logger.e(TAG, "Could not open file for writing", e);
			} catch (UnsupportedEncodingException e) {
				Logger.e(TAG, "Unsupported encoding for writing file", e);
			} catch (IOException e) {
				Logger.e(TAG, "IO exception writing file", e);
			}
		}
	}

	/**
	 * @return the root Element of the XMLConfiguration
	 */
	public Element getRoot()
	{
		return m_root;
	}
	
	/**
	 * Update the lastUpdated attribute on the root node
	 */
	private void updateLastUpdated()
	{
		m_root.addAttribute(new Attribute(C_XML_LASTUPDATED, DateTime.now().toString(SysUtils.getDateTimeFormatter())));
	}

	/* (non-Javadoc)
	 * @see synet.controller.configuration.Configuration#getAll()
	 */
	@Override
	public Map<String, String> getAll() {
		HashMap<String, String> map = new HashMap<String, String>();
		
		addChildElements(m_root.getChildElements(), map);
		
		return map;
	}

	/**
	 * Recursive function to add element pairs to a map
	 * 
	 * @param p_elements
	 * @param map
	 */
	private void addChildElements(Elements p_elements, HashMap<String, String> map) {
		for(int i=0; i<p_elements.size(); ++i)
		{
			Element e = p_elements.get(i);
			
			// If there are no child elements, this is a value
			if (e.getChildElements().size() == 0)
			{
				map.put(getFullKey(e), e.getValue());
			}
			else
			{
				addChildElements(e.getChildElements(), map);
			}
		}
	}
	
	/**
	 * Return the full key for an element (ie. nodeManager.nodes.node1)
	 * @param p_element
	 * @return
	 */
	private String getFullKey(Element p_element)
	{
		StringBuilder sb = new StringBuilder();
		Element nextEle = p_element;
		while(nextEle != null)
		{
			if (sb.length() != 0)
			{
				sb.insert(0, C_XML_SEPARATOR);
			}
			sb.insert(0, nextEle.getLocalName());
			
			if (nextEle.getParent() instanceof Element)
			{
				nextEle = (Element)nextEle.getParent();
			}
			else
			{
				nextEle = null;
			}
		}
		return sb.toString();
	}
}
