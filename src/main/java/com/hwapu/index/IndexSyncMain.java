package com.hwapu.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.hwapu.utils.C3p0Utils;
import com.hwapu.utils.ClientProvider;
import com.hwapu.utils.XmlProperties;

public class IndexSyncMain {
	private static final Logger LOGGER = Logger.getLogger(IndexSyncMain.class);
	private static final Connection connection = C3p0Utils.getConnection();
	private static final String recordTableName ;
	static {
		Map<String, String> confiMap = XmlProperties.loadFromXml("sys_config.xml");
		 recordTableName = confiMap.get("recordTableName");
	}
	
	public static void syncIndexMain(Map<String, String> indexConfigMap, String indexName) throws Exception {
		if(indexExits(indexName)) {
			PreparedStatement ps =null;
			ResultSet rs= null;
			try {
				String sql = "select * from "+recordTableName+" where delete_flag = ? and index_name =?";
				ps =connection.prepareStatement(sql);
				ps.setInt(1, 0);
				ps.setString(2, indexName);
				rs = ps.executeQuery();
				while(rs.next()) {
					Long id = rs.getLong("id");
					String recordId =rs.getString("record_id") ;
					String optionType = rs.getString("option_type");
					String docType = rs.getString("doc_type");
					String recordIdColumn = rs.getString("record_id_column");
					
					String indexName2 = rs.getString("index_name");
					String tableName = rs.getString("table_name");
					Date updateTime  = rs.getDate("update_time");
					System.out.println("id:"+id+
							"\trecord_id:"+recordId+
							"\trecord_id_column:"+recordIdColumn+
							"\ttable_name:"+tableName+
							"\toption_type:"+optionType+
							"\tdoc_type:"+docType+
							"\tindex_name:"+indexName2+
							"\tupdate_time:"+updateTime);
					
					cudIndex(id, recordId,recordIdColumn!=null?recordIdColumn:"id" ,optionType ,indexName2,indexConfigMap);
					
				}
				
			}catch (Exception e) {
				throw e;
			}finally {
				if(rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						rs=null;
						e.printStackTrace();
					}
				}
				if(ps != null) {
					try {
						ps.close();
					} catch (SQLException e) {
						ps=null;
						e.printStackTrace();
						
					}
				}
			}
		}else {
			//不存在就全量新增索引
			try {
				fullIndex(indexName,indexConfigMap);
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
	}
	/**
	 * 同步(更新 新增 删除)索引
	 * @param id 记录表id
	 * @param recordId 记录id
	 * @param optionType 操作类型
	 * @param indexName 索引名
	 * @param indexConfigMap 索引配置文件项
	 * @throws Exception 
	 */
	private static void cudIndex(Long id,String recordId,String recordIdColumn,String optionType,String indexName,Map<String,String> indexConfigMap) throws Exception {
		//如果索引文件存在就更新
		if("insert" .equals(optionType)) {
			addInex(id,recordId,recordIdColumn,indexName,indexConfigMap);
		}else if("update".equals(optionType)) {
			updateIndex(id,recordId,recordIdColumn,indexName,indexConfigMap);
		}else if("delete" .equals(optionType)) {
			deleteIndex(id,recordId,recordIdColumn,indexName,indexConfigMap);
		}
	}
	/**
	 * 全量索引数据表
	 * @param indexName
	 * @param indexConfigMap
	 * @throws Exception 
	 */
	private static void fullIndex(String indexName, Map<String,String> indexConfigMap) throws Exception {
		LOGGER.info("全量同步数据到ES搜索引擎,索引名["+indexName+"]");
		/**
		 * 创建索引
		 */
		createIndex(indexName,indexConfigMap);
		/**
		 * 遍历索引文档
		 */
		String docType = indexConfigMap.get(indexName+"_docType");
		
		String sql = indexConfigMap.get(indexName).toLowerCase();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			
			String sqlCount ="select count(1) from ("+sql+") a";
			ps = connection.prepareStatement(sqlCount);
			rs = ps.executeQuery();
			rs.next();
			int count =rs.getInt(1) ;
			rs.close();
			ps.close();
			int pageSize =1000;
			if(count>1000) {//超过1000条记录就分页 分页处理只能针对单表  多表有待改进
				int pageNum =count/pageSize;
				if(count%pageSize != 0) {
					pageNum += 1;
				}
				for(int j=1;j<=pageNum;j++) {
					LOGGER.info("第"+j+"页数据,每页500条");
					int topNum = j*pageSize;//分页 每页500条记录
					String sqlPage= "select * from ( select ROW_NUMBER()  OVER (ORDER BY id) as rowNum,* from ("+sql+") a ) b where rowNum between "+(topNum-pageSize+1)+" and "+topNum;
					ps = connection.prepareStatement(sqlPage);
					rs = ps.executeQuery();
					forEachAddIndex(indexName, docType, rs);
					rs.close();
					ps.close();
				}
				
			}else {
				ps = connection.prepareStatement(sql);
				rs = ps.executeQuery();
				forEachAddIndex(indexName, docType, rs);

			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}finally {
			C3p0Utils.close(ps, rs);
		}
	}
	/**
	 * 循环遍历结果集 创建索引
	 * @param indexName
	 * @param docType
	 * @param rs
	 * @throws SQLException
	 */
	private static void forEachAddIndex(String indexName, String docType, ResultSet rs)
			throws SQLException {
		BulkRequestBuilder bulkRequest =  ClientProvider.get().prepareBulk();
		String[] columnNames = getColumnNames(rs);
		String[] colunmTypes = getColumnTypes(rs);
		while(rs.next()) {
			LOGGER.info("[indexName]:"+indexName+"[recordId]:"+rs.getObject("id"));
			Map<String, Object> json = new HashMap<String, Object>();
			for(int i =0;i<columnNames.length;i++) {
				if(columnNames[i].equals("rowNum")) {
					continue;
				}
				if("java.lang.String".equals(colunmTypes[i])) {
					json.put(columnNames[i],rs.getString(columnNames[i]));
				}else if("java.lang.Integer".equals(colunmTypes[i])) {
					json.put(columnNames[i],rs.getInt(columnNames[i]));
				}else if("java.lang.Long".equals(colunmTypes[i])) {
					json.put(columnNames[i],rs.getLong(columnNames[i]));
				}else if("java.sql.Timestamp".equals(colunmTypes[i])) {
					json.put(columnNames[i],rs.getDate(columnNames[i]).getTime());
				}else if("java.sql.Date".equals(colunmTypes[i])) {
					json.put(columnNames[i],rs.getDate(columnNames[i]).getTime());
				}else {
					json.put(columnNames[i],rs.getObject(columnNames[i]));
				}
				
			}
			bulkRequest.add(ClientProvider.get().prepareIndex(indexName, docType, rs.getObject("id").toString()).setSource(json));
		}
		BulkResponse bulkResponse = bulkRequest.get();
		if (bulkResponse.hasFailures()) {
			Iterator<BulkItemResponse> iterator= bulkResponse.iterator();
			while (iterator.hasNext()) {
				BulkItemResponse response =  iterator.next();
				LOGGER.error(response.getFailureMessage());
				
			}
		}
	}
	/**
	 * 创建索引
	 * @param indexName
	 * @param docType
	 * @param indexConfigMap
	 * @throws Exception 
	 */
	private static void createIndex(String indexName, Map<String, String> indexConfigMap) throws Exception {
		LOGGER.info("新建索引,索引名["+indexName+"]");
		try {
			String mappingConfig = indexConfigMap.get(indexName+"_mapping");
			String indexDocType = indexConfigMap.get(indexName+"_docType");
			//XContentBuilder xContentBuilder = new x
			ClientProvider.get().admin().indices().prepareCreate(indexName).addMapping(indexDocType, mappingConfig,XContentType.JSON).get();
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 删除索引
	 * @param id
	 * @param recordId
	 * @param indexName
	 * @param indexConfigMap
	 * @throws Exception 
	 */
	private static void deleteIndex(Long id, String recordId,String recordIdColumn,String indexName,Map<String,String> indexConfigMap) throws Exception {
		LOGGER.info("开始删除索引文档["+indexName+"]");
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = indexConfigMap.get(indexName);
			String docType =indexConfigMap.get(indexName+"_docType");
			
			//老版本
			//sql+=" and "+recordIdColumn+" = '" + recordId+"'";
			
			//新版本
			sql = "select * from ("+sql+") a where " + recordIdColumn +"= '" + recordId +"'" ;
			
			
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();
			BulkRequestBuilder bulkRequest = ClientProvider.get().prepareBulk();
			boolean needUpdate = rs.next();
			while(needUpdate) {
				LOGGER.info("[删除索引:]"+indexName+"\t[索引id:]"+rs.getObject("id"));
				bulkRequest.add(ClientProvider.get().prepareDelete(indexName, docType, rs.getObject("id").toString()));
			}
			if(needUpdate) {
				BulkResponse bulkResponse = bulkRequest.get();
				if (bulkResponse.hasFailures()) {
					Iterator<BulkItemResponse> iterator= bulkResponse.iterator();
					while (iterator.hasNext()) {
						BulkItemResponse response =  iterator.next();
						LOGGER.error(response.getFailureMessage());
						
					}
				}
			}
			
			C3p0Utils.close(ps, rs);
			sql = "update "+recordTableName+" set delete_flag =? ,update_time=? where id =?";
			ps = connection.prepareStatement(sql);
			ps.setInt(1, 1);
			ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			ps.setLong(3, id);
			ps.executeUpdate();
			LOGGER.info("删除更新索引完成!");
			
		} catch (SQLException e) {
			throw e;
		}finally {
			C3p0Utils.close(ps, rs);
		}
		
	}

	/**
	 * 更新索引
	 * @param id
	 * @param recordId
	 * @param indexName
	 * @param indexConfigMap
	 * @throws Exception 
	 */
	private static void updateIndex(Long id,String recordId,String recordIdColumn, String indexName,Map<String,String> indexConfigMap) throws Exception {
		addInex(id, recordId,recordIdColumn, indexName,indexConfigMap);
		
	}

	/**
	 *  新增索引
	 * @param id
	 * @param recordId
	 * @param indexName
	 * @param indexConfigMap
	 * @throws Exception 
	 */
	private static void addInex(Long id, String recordId,String recordIdColumn, String indexName,Map<String,String> indexConfigMap) throws Exception {
		LOGGER.info("开始新增/更新索引,索引名称["+indexName+"]");
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = indexConfigMap.get(indexName);
			String docType =indexConfigMap.get(indexName+"_docType");
			//老版本
			//sql+=" and "+recordIdColumn+" = '" + recordId+"'";
			//新版本
			sql = "select * from ("+sql+") a where " + recordIdColumn +"= '" + recordId +"'" ;
			
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();
			String[] columnNames = getColumnNames(rs);
			String[] colunmTypes = getColumnTypes(rs);
			BulkRequestBuilder bulkRequest = ClientProvider.get().prepareBulk();
			boolean needUpdate = rs.next();
			while(needUpdate) {
				LOGGER.info("[新增/更新-索引:]"+indexName+"\t[索引id:]"+rs.getObject("id"));
				Map<String, Object> json = reslut2JsonMap(rs, columnNames, colunmTypes);
				bulkRequest.add(ClientProvider.get().prepareIndex(indexName, docType,rs.getObject("id").toString())
				        .setSource(json));
			/*	IndexResponse response = ClientProvider.get().prepareIndex(indexName, docType,rs.getObject("id").toString())
				        .setSource(json).get();*/
			}
			BulkResponse bulkResponse ;
			if(needUpdate) {
				bulkResponse = bulkRequest.get();
				if (bulkResponse.hasFailures()) {
					Iterator<BulkItemResponse> iterator= bulkResponse.iterator();
					while (iterator.hasNext()) {
						BulkItemResponse response =  iterator.next();
						LOGGER.error(response.getFailureMessage());
					}
				}
			}
			C3p0Utils.close( ps, rs);
			
			sql = "update "+recordTableName+" set delete_flag =?,update_time=? where id =?";
			ps = connection.prepareStatement(sql);
			ps.setInt(1, 1);
			ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			ps.setLong(3, id);
			ps.executeUpdate();
			LOGGER.info("新增或更新操作记录表成功,新增或更新");
		} catch (SQLException e) {
			throw e;
		}finally {
			C3p0Utils.close( ps, rs);
		}
	}

	private static Map<String, Object> reslut2JsonMap(ResultSet rs, String[] columnNames, String[] colunmTypes)
			throws SQLException {
		Map<String, Object> json = new HashMap<String, Object>();
		for(int i =0;i<columnNames.length;i++) {
			if("java.lang.String".equals(colunmTypes[i])) {
				json.put(columnNames[i],rs.getString(columnNames[i]));
			}else if("java.lang.Integer".equals(colunmTypes[i])) {
				json.put(columnNames[i],rs.getInt(columnNames[i]));
			}else if("java.lang.Long".equals(colunmTypes[i])) {
				json.put(columnNames[i],rs.getLong(columnNames[i]));
			}else if("java.sql.Timestamp".equals(colunmTypes[i])) {
				json.put(columnNames[i],rs.getDate(columnNames[i]).getTime());
			}else if("java.sql.Date".equals(colunmTypes[i])) {
				json.put(columnNames[i],rs.getDate(columnNames[i]).getTime());
			}else {
				json.put(columnNames[i],rs.getObject(columnNames[i]));
			}
			
		}
		return json;
	}
	/**
	 * 获取数据库查询结果集每列名称
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static String[] getColumnNames(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int count=rsmd.getColumnCount();

		String[] name=new String[count];
		for(int i=0;i<count;i++) {
			name[i]=rsmd.getColumnName(i+1);
		}
		return name;
	}
	
	/**
	 * 获取数据库查询结果集每列类型
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static String[] getColumnTypes(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int count=rsmd.getColumnCount();

		String[] types=new String[count];
		for(int i=0;i<count;i++) {
			types[i]=rsmd.getColumnClassName(i+1);
		}
		return types;
	}

	/**
	 * 判断索引是否存在
	 * @param indexName
	 * @return
	 */
	private static boolean indexExits(String indexName) {
		TransportClient client =ClientProvider.get();
		ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get(); 
		return healths.getIndices().get(indexName)!=null;
	}
}
