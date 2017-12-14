package com.hwapu.run;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.hwapu.index.IndexRunable;
import com.hwapu.utils.XmlProperties;


/**
 * 
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	 try {
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			IndexRunable runable = new IndexRunable();
			// 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间  
			Map<String, String> confiMap = XmlProperties.loadFromXml("sys_config.xml");
			String howLongSyn = confiMap.get("how_long_syn");
			if (howLongSyn == null) {
				howLongSyn = "24";
			}
			service.scheduleAtFixedRate(runable, 0, Integer.valueOf(howLongSyn.trim()), TimeUnit.SECONDS);
		} finally {
		}  
    }
}
