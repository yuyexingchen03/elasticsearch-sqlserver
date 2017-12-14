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
