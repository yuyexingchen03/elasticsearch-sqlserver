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


>     <!-- 是否启用嗅探 还可以配置其他一些配置 -->
>     <entry key="client.transport.sniff">true</entry>
>    <!-- 集群名称 -->
>    <entry key="cluster.name">EsApplication</entry>
>    <entry key="transport.addresses">192.168.100.155:9201</entry>
>
------
其他一些参数
参数 | 值和说明
---|---
client.transport.ignore_cluster_name | Set to true to ignore cluster name validation of connected nodes. (since 0.19.4)
client.transport.ping_timeout | The time to wait for a ping response from a node. Defaults to 5s.
client.transport.nodes_sampler_interval | How often to sample / ping the nodes listed and connected. Defaults to 5s.
