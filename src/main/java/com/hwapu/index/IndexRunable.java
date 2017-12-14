package com.hwapu.index;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hwapu.utils.XmlProperties;
public class IndexRunable implements Runnable{
	
	private static final Logger LOGGER = Logger.getLogger(IndexRunable.class);
	
	public IndexRunable() { }
	
	public void run() {
		LOGGER.info("开始定时同步数据到ES搜索引擎");
		try {
			System.out.println(new Date()+":开始同步");
			Map<String,String> indexConfigMap = XmlProperties.loadFromXml("indexs.xml");
			for(Map.Entry<String, String> entry : indexConfigMap.entrySet()) {
				String indexName = entry.getKey();
				if(!(indexName.endsWith("_docType")||indexName.endsWith("_mapping"))) {
					IndexSyncMain.syncIndexMain(indexConfigMap, indexName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}


}
