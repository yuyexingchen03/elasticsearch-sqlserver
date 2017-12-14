# elasticsearch-sqlserver
可以定时同步sqlserver数据到ElasticSearch搜索引擎

需要配合数据库触发器使用

下载解压,需要配置以下配置文件
## 应用相关配置
### c3p0-config.xml 数据可连接和驱动配置

>		<property name="driverClass">com.microsoft.sqlserver.jdbc.SQLServerDriver</property>
>		<property name="jdbcUrl">jdbc:sqlserver://192.168.100.51:1433</property>
>		<property name="user">sa</property>
>		<property name="password">123456</property>


注意jdbcUrl没有配置具体哪个数据库

------

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
    <!-- mapping 配置  
        可以参考ElasticSearch的文档
        https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html-->
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
------

### sys_config.xml 配置

> how_long_syn :间隔多长时间同步一次数据,单位小时
> recordTableName : 触发器保存数据库操作记录的表   后面详细讲

例子:
```xml
   	<!-- 间隔多长时间同步一次数据 单位小时 -->
    <entry key="how_long_syn">24</entry>
    <!-- 触发器保存数据库操作记录的表  -->
    <entry key="recordTableName">数据库名.dbo.操作记录表名</entry>
```
------

## 数据库相关配置

### 建立用于保存数据库插入(insert),更新(update),删除(delete)的表
```sql
DROP TABLE [dbo].[操作记录表名]
GO
CREATE TABLE [dbo].[操作记录表名] (
[id] int NOT NULL IDENTITY(1,1) ,
[table_name] varchar(255) NULL ,
[option_type] varchar(255) NULL ,
[delete_flag] int NULL ,
[index_name] varchar(255) NULL ,
[doc_type] varchar(255) NULL ,
[update_time] datetime2(7) NULL ,
[record_id] varchar(255) NULL ,
[record_id_column] varchar(255) NULL DEFAULT ('id') 
)


GO
DBCC CHECKIDENT(N'[dbo].[操作记录表名]', RESEED, 4025)
GO
IF ((SELECT COUNT(*) from fn_listextendedproperty('MS_Description', 
'SCHEMA', N'dbo', 
'TABLE', N'操作记录表名', 
'COLUMN', N'record_id_column')) > 0) 
EXEC sp_updateextendedproperty @name = N'MS_Description', @value = N'对应为主键用的id的名称,不填写的话默认id'
, @level0type = 'SCHEMA', @level0name = N'dbo'
, @level1type = 'TABLE', @level1name = N'操作记录表名'
, @level2type = 'COLUMN', @level2name = N'record_id_column'
ELSE
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'对应为主键用的id的名称,不填写的话默认id'
, @level0type = 'SCHEMA', @level0name = N'dbo'
, @level1type = 'TABLE', @level1name = N'操作记录表名'
, @level2type = 'COLUMN', @level2name = N'record_id_column'
GO

ALTER TABLE [dbo].[操作记录表名] ADD PRIMARY KEY ([id])
GO
```
注意:
>操作记录表名 对应sys_config.xml中recordTableName属性

------

### 建立数据库触发器
假如建立索引的sql语句如下,索引名称为`indexName1`,索引的`docType`名称为`docType1` ,存放操作记录的表是`database_xx.dbo.recordTable`
```sql
SELECT
			b.GUID as id,
			b.GUID AS CatalogId,
			b.CatalogName,
			a.Barcode,
			a.BigPic,
			a.BookName,
			a.FileAbsoluteUrl AS FileRemoteUrl,
			a.FileSize,
			d.GUID AS GradeId,
			a.SubjectID,
			a.SmallPic,
			b.BookId,
			a.PressID,
			b.ParentID,
			b.Sort
		FROM
			databaseName1.dbo.table1 a
		RIGHT OUTER JOIN databaseName1.dbo.table2 b ON a.GUID = b.BookID
		RIGHT OUTER JOIN databaseName1.dbo.table3 c ON a.GradeID = c.GradeID
		JOIN databaseName1.dbo.table4 d ON c.GradeClassID = d.GUID
		WHERE
			a.State = 1
```
在table1上建立三个触发器
insert触发器
```sql
BEGIN
  declare @GUID varchar(255);
  /* 此处为什么取GUID的值呢? 根据上面sql语句 ON a.GUID = b.BookID 结合查找获取的列(结果集列中)中出现了推断b.BookId, */
  select @GUID =GUID from inserted;
INSERT INTO database_xx.dbo.recordTable(record_id,record_id_column,table_name,option_type,update_time,delete_flag,index_name,doc_type) 
/* 'BookID'此处为什么取这个值? 因为 ON a.GUID = b.BookID 同事a.GUID没有出现在查询结果集列中,而b.BookID缺出现在结果集列中 */
  VALUES  (@GUID ,'BookID','databaseName1.dbo.table1','insert',GETDATE(),0,'indexName1','docType1');
END

```
update触发器
```sql
BEGIN
  declare @GUID varchar(255);
  /* 此处为什么取GUID的值呢? 根据上面sql语句 ON a.GUID = b.BookID 结合查找获取的列(结果集列中)中出现了推断b.BookId, */
  select @GUID =GUID from inserted;
INSERT INTO database_xx.dbo.recordTable(record_id,record_id_column,table_name,option_type,update_time,delete_flag,index_name,doc_type) 
/* 'BookID'此处为什么取这个值? 因为 ON a.GUID = b.BookID 同事a.GUID没有出现在查询结果集列中,而b.BookID缺出现在结果集列中 */
  VALUES  (@GUID ,'BookID','databaseName1.dbo.table1','update',GETDATE(),0,'indexName1','docType1');
END
```
delete触发器
```sql
BEGIN
  declare @GUID varchar(255);
  /* 此处为什么取GUID的值呢? 根据上面sql语句 ON a.GUID = b.BookID 结合查找获取的列(结果集列中)中出现了推断b.BookId, */
  select @GUID =GUID from deleted;
INSERT INTO database_xx.dbo.recordTable(record_id,record_id_column,table_name,option_type,update_time,delete_flag,index_name,doc_type) 
/* 'BookID'此处为什么取这个值? 因为 ON a.GUID = b.BookID 同事a.GUID没有出现在查询结果集列中,而b.BookID缺出现在结果集列中 */
  VALUES  (@GUID ,'BookID','databaseName1.dbo.table1','delete',GETDATE(),0,'indexName1','docType1');
END
```
