<configuration>
	<property>
		<name>dfs.replication</name>
		<value>1</value>
	</property>
	<property>
		<name>dfs.namenode.name.dir</name>
		<value>%INSTALL_DIR%/%NAMENODE_DIR%</value>
	</property>
	<property>
		<name>dfs.datanode.data.dir</name>
		<value>%INSTALL_DIR%/%DATANODE_DIR%</value>
	</property>
	<property>
		<name>dfs.namenode.http-address</name>
		<value>localhost:50070</value>
	</property>
</configuration>