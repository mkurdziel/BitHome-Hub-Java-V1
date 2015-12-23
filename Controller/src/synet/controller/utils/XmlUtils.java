package synet.controller.utils;

import java.io.File;
import java.io.IOException;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import synet.controller.Protocol.EsnDataTypes;

public class XmlUtils {
    public static final String TAG = "XmlUtils";
    public static final String C_STR_SUFFIX = ".xml";

    public static DateTime getXmlAttributeDateTime(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            return DateTime.parse(attr.getValue(), SysUtils.getDateTimeFormatter());
        }
        return null;
    }

    public static String getXmlAttributeString(
            Element p_element,
            String p_attrName,
            String p_default) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            return attr.getValue();
        }
        return p_default;
    }
    
    public static String getXmlAttributeString(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            return attr.getValue();
        }
        return null;
    }

    public static String getXmlElementString(Element p_element,
            String p_elementName) {
        Elements elements = p_element.getChildElements(p_elementName);
        if (elements != null && elements.size() > 0)
        {
            return elements.get(0).getValue();
        }
        return null;
    }

    public static Long getXmlAttributeLong(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            String strLong = StringUtils.leftPad(stripPrefix(attr.getValue()), 16, "0");
            Long msb = Long.parseLong(strLong.substring(0, 8), SysUtils.C_INT_BASE) << 32;
            Long lsb = Long.parseLong(strLong.substring(8, 16), SysUtils.C_INT_BASE);
            return msb | lsb;
        }
        return null;
    }

    public static String stripPrefix(String value)
    {
        if (value.startsWith("0x")) return value.substring(2);
        return value;
    }

    public static Integer getXmlElementInteger(Element p_element,
            String p_elementName) {
        Elements elements = p_element.getChildElements(p_elementName);
        if (elements != null && elements.size() > 0)
        {
            // Handle the negative value
            if (elements.get(0).getValue().equals("0xffffffff"))
            {
                return -1;
            }

            return Integer.parseInt(stripPrefix(elements.get(0).getValue()), SysUtils.C_INT_BASE);
        }
        return null;
    }

    public static Short getXmlAttributeShort(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            int in =  Integer.parseInt(stripPrefix(attr.getValue()), SysUtils.C_INT_BASE);
            return (short)in;
        }
        return null;
    }
    
    public static Short getXmlAttributeShort(Element p_element,
            String p_attrName, short defaultValue) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            int in =  Integer.parseInt(stripPrefix(attr.getValue()), SysUtils.C_INT_BASE);
            return (short)in;
        }
        return defaultValue;
    }

    public static Short getXmlAttributeShort10(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            int in =  Integer.parseInt(attr.getValue(), 10);
            return (short)in;
        }
        return null;
    }	

    public static EsnDataTypes getXmlAttributeDataType(Element p_element,
            String p_elementName) {
        Attribute attr = p_element.getAttribute(p_elementName);
        if (attr != null)
        {
            return EsnDataTypes.valueOf(attr.getValue());
        }
        return null;
    }

    public static Integer getXmlAttributeInteger(Element p_element,
            String p_attrName) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            // Handle the negative value
            if (attr.getValue().equals("0xffffffff"))
            {
                return -1;
            }
            return Integer.parseInt(stripPrefix(attr.getValue()), SysUtils.C_INT_BASE);
        }
        return null;
    }

    public static Integer getXmlAttributeInteger(Element p_element,
            String p_attrName, int p_base) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            return Integer.parseInt(stripPrefix(attr.getValue()), p_base);
        }
        return null;
    }

    public static Long getXmlAttributeLong(Element p_element,
            String p_attrName, int p_base) {
        Attribute attr = p_element.getAttribute(p_attrName);
        if (attr != null)
        {
            String strLong = StringUtils.leftPad(stripPrefix(attr.getValue()), 16, "0");
            Long msb = Long.parseLong(strLong.substring(0, 8), p_base) << 32;
            Long lsb = Long.parseLong(strLong.substring(8, 16), p_base);
            return msb | lsb;
        }
        return null;
    }
    

    /**
     * Read in XML file and return an XML document
     * 
     * @param p_fileName
     * @return
     */
    public static Document readXML(String p_fileName)
    {
        // Make sure we're handing an xml file here
        if (p_fileName.endsWith(C_STR_SUFFIX))
        {
            Builder parser = new Builder();
            File inFile = new File(p_fileName);
            if (inFile.exists())
            {
                try {
                    Document doc = parser.build(inFile);
                    return doc;
                } catch (ValidityException e) {
                    Logger.e(TAG, "validity exception while reading file: " + p_fileName, e);
                } catch (ParsingException e) {
                    Logger.e(TAG, "paring exception while reading file: " + p_fileName, e);
                } catch (IOException e) {
                    Logger.e(TAG, "IOException while reading file: " + p_fileName, e);
                }
            }
            else
            {
                Logger.e(TAG, "trying to read file that doesn't exist: " + p_fileName);
            }
        }   
        return null;
    }
}
