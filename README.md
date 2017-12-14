# elasticsearch-sqlserver
可以定时同步sqlserver数据到ElasticSearch搜索引擎

需要配合触发器使用

下载解压,需要配置以下配置文件
## c3p0-config.xml 数据可连接和驱动配置

>		<property name="driverClass">com.microsoft.sqlserver.jdbc.SQLServerDriver</property>
>		<property name="jdbcUrl">jdbc:sqlserver://192.168.100.51:1433</property>
>		<property name="user">sa</property>
>		<property name="password">123456</property>
>
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


