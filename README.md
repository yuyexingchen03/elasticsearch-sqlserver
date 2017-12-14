# elasticsearch-sqlserver
可以定时同步sqlserver数据到ElasticSearch搜索引擎

需要配合触发器使用

下载解压,需要配置以下配置文件
## c3p0-config.xml 数据可连接和驱动配置

>		<property name="driverClass">com.microsoft.sqlserver.jdbc.SQLServerDriver</property>
>		<property name="jdbcUrl">jdbc:sqlserver://192.168.100.51:1433</property>
>		<property name="user">sa</property>
>		<property name="password">123456</property>

------
注意jdbcUrl没有配置具体哪个数据库

## elasticsearch.xml配置ElasticSearch节点

> * cluster.name : 集群名字
> * transport.addresses : 节点列表,可以多个,逗号分隔
> * client.transport.sniff : In order to enable sniffing
> * client.transport.ignore_cluster_name :Set to true to ignore cluster name validation of connected nodes. (since 0.19.4)
> * client.transport.ping_timeout : The time to wait for a ping response from a node. Defaults to 5s.
> * client.transport.nodes_sampler_interval : How often to sample / ping the nodes listed and connected. Defaults to 5s.
   

------

## indexs.xml 索引配置
> * &lt;entry key="**索引名**"&gt;sql语句&lt;/entry&gt;
> * &lt;entry key="**索引名**_docType"&gt;索引的文档类型名&lt;/entry&gt;
> * &lt;entry key="**索引名**_docType_mapping"&gt;文档mapping配置明细&lt;/entry&gt;

例子:

| &lt;!-- 索引sql语句  key是索引名 --&gt;
    &lt;entry key="product_content"&gt;
		SELECT
			id,
			topicname,
			keyword,
			miaoshu,
			jianjie,
			biaoqian,
			updatetime
		FROM
			weiketi.dbo.product_content
		WHERE
			1 = 1
	&lt;/entry>
    &lt;!-- 索引文档类型  value是product_content索引的document类型--&gt;
    &lt;entry key="product_content_docType"&gt;product_content&lt;/entry&gt;
    &lt;!-- mapping 配置 --&gt;
    &lt;entry key="product_content_mapping"&gt;
		 {
		    "product_content": {
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
	&lt;/entry&gt;       | 
| --------   | 

