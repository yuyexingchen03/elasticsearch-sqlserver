package com.hwapu.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.log4j.Logger;



public class XmlProperties {
	private static final Logger LOGGER = Logger.getLogger(XmlProperties.class);  
    private static Map<String,Map<String, String>> xmlMap =new HashMap<String, Map<String,String>>();
 
    private XmlProperties() {
    }
 
    public static Map<String, String> loadFromXml(String xmlPropertiesPath) {
    	Map<String, String> resMap = null;
    	resMap = xmlMap.get(xmlPropertiesPath);
    	if(resMap == null) {
    		try {
                Object in = XmlProperties.class.getClassLoader().getResourceAsStream(xmlPropertiesPath);
                if(in != null) {
                	LOGGER.info("Found the xml properties ["+xmlPropertiesPath+"] in class path,use it");
                	
                    //LOGGER.info("Found the xml properties [{}] in class path,use it", xmlPropertiesPath);
                    Map e1 = loadFromXml((InputStream)in);
                    return e1;
                }
                
     
                File e = new File(xmlPropertiesPath);
                if(!e.isFile()) {
                    return resMap;
                }
     
                LOGGER.info("Found the xml properties ["+xmlPropertiesPath+"] in file path,use it");
                in = new FileInputStream(new File(xmlPropertiesPath));
                resMap = loadFromXml((InputStream)in);
                ((InputStream) in).close();
                
                xmlMap.put(xmlPropertiesPath, resMap);
            } catch (Exception var7) {
                LOGGER.error("Load xml properties [" + xmlPropertiesPath + "] error.", var7);
            }
    	}
        return resMap;
    }
 
    public static Map<String, String> loadFromXml(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.loadFromXML(in);
        HashMap map = new HashMap();
        Set entries = properties.entrySet();
        Iterator iter = entries.iterator();
 
        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
 
        return map;
    }
}
