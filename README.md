# elasticsearch-sqlserver
可以定时同步sqlserver数据到ElasticSearch搜索引擎

需要配合触发器使用

下载解压,需要配置以下配置文件
## 应用相关配置
### c3p0-config.xml 数据可连接和驱动配置

>		<property name="driverClass">com.microsoft.sqlserver.jdbc.SQLServerDriver</property>
>		<property name="jdbcUrl">jdbc:sqlserver://192.168.100.51:1433</property>
>		<property name="user">sa</property>
>		<property name="password">123456</property>

------
注意jdbcUrl没有配置具体哪个数据库

### elasticsearch.xml配置ElasticSearch节点

> * cluster.name : 集群名字
> * transport.addresses : 节点列表,可以多个,逗号分隔
> * client.transport.sniff : In order to enable sniffing
> * client.transport.ignore_cluster_name :Set to true to ignore cluster name validation of connected nodes. (since 0.19.4)
> * client.transport.ping_timeout : The time to wait for a ping response from a node. Defaults to 5s.
> * client.transport.nodes_sampler_interval : How often to sample / ping the nodes listed and connected. Defaults to 5s.
   

------

### indexs.xml 索引配置
> * &lt;entry key="**索引名**"&gt;sql语句&lt;/entry&gt;
> * &lt;entry key="**索引名**_docType"&gt;索引的文档类型名&lt;/entry&gt;
> * &lt;entry key="**索引名**_docType_mapping"&gt;文档mapping配置明细&lt;/entry&gt;

例子:
```xml
 <!-- 索引sql语句  key是索引名 必须有id这列 做为索引的id -->
    <entry key="index1">
		SELECT
			id,
			topicname,
			keyword,
			miaoshu,
			jianjie,
			biaoqian,
			updatetime
		FROM
			databasename.dbo.tablename
		WHERE
			1 = 1
	</entry>
    <!-- 索引文档类型  value是product_content索引的document类型-->
    <entry key="index1_docType">docTypeName</entry>
    <!-- mapping 配置  可以参考ElasticSearch的文档 https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html-->
    <entry key="index1_mapping">
		 {
		    "docTypeName": {
		      "properties": {
		        "id": {
		          "type": "long",
		          "store":true,
		          "index":false
		        },
		        "topicname": {
		          "type": "text",
		          "store":true,
		          "index":true,
	           	  "analyzer": "ik_max_word",
		          "search_analyzer": "ik_max_word"
		        },
		        "keyword": {
		         "type": "text",
		          "store":true,
		          "index":true,
	           	  "analyzer": "ik_max_word",
		          "search_analyzer": "ik_max_word"
		        },
		         "miaoshu": {
		         "type": "text",
		          "index":true,
	           	  "analyzer": "ik_max_word",
		          "search_analyzer": "ik_max_word"
		        },
		         "jianjie": {
		         "type": "text",
		          "index":true,
	           	  "analyzer": "ik_max_word",
		          "search_analyzer": "ik_max_word"
		        },
		         "biaoqian": {
		         "type": "text",
		          "index":true,
	           	  "analyzer": "ik_max_word",
		          "search_analyzer": "ik_max_word"
		        },
		         "updatetime": {
		         "type": "date",
		         "store":true,
		          "index":true
		        }
		      }
		    }
		  }
	</entry>     
```

### sys_config.xml 配置

> how_long_syn :间隔多长时间同步一次数据,单位小时
> recordTableName : 触发器保存数据库操作记录的表   后面详细讲

例子:
```xml
   	<!-- 间隔多长时间同步一次数据 单位小时 -->
    <entry key="how_long_syn">24</entry>
    <!-- 触发器保存数据库操作记录的表  -->
    <entry key="recordTableName">数据库名.dbo.表名</entry>
```

## 数据库相关配置
