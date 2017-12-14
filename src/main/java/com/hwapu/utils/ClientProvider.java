package com.hwapu.utils;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


import static java.lang.System.out;

public class ClientProvider {
	private static final Logger LOGGER = Logger.getLogger(ClientProvider.class);  
	 
    private static TransportClient client;
    
    private ClientProvider() {}
 
    static {
            try {
                Map<String, String> config = XmlProperties.loadFromXml("elasticsearch.xml");
                if (config == null) {
                    out.println("load xml err");
                    System.exit(0);;
                }
                Iterator<Map.Entry<String, String>> iterator = config.entrySet().iterator();
                Map<String, String> settingConfig = new HashMap<String, String>();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    if (!next.getKey().equals("transport.addresses")) {
                        settingConfig.put(next.getKey(), next.getValue());
                    }
                }
                Settings settings = Settings.builder().put(settingConfig).build();
                client = new PreBuiltTransportClient(settings);
 
                String[] addresses = config.get("transport.addresses").split(",");
                for (String address : addresses) {
                    String[] hostAndPort = address.split(":");
                    /**
                     * 6.0.0
                     */
                  //  client.addTransportAddresses(new TransportAddress(InetAddress.getByName(hostAndPort[0]), Integer.valueOf(hostAndPort[1])));
                    /**
                     * 5.6.3
                     */
                    
                    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostAndPort[0]), Integer.valueOf(hostAndPort[1])));

                }
            } catch (Exception e) {
                LOGGER.error(String.format("init search client err:=>msg:[%s]", e.getMessage()), e);
                if (client != null) {
                    client.close();
                }
            }
    }
    
    public static TransportClient get() {
        return client;
    }
 
    public static void close() {
    	 if (client != null) {
             client.close();
         }
    }
    

}
