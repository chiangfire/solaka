package com.firecode.solaka.config;

import static com.firecode.solaka.config.SolakaCassandraConfiguration.BASE_PACKAGES_0;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.SocketOptions;
import com.firecode.kabouros.cassandra.BaseReactiveCassandraRepository;
import com.firecode.solaka.config.properties.SolakaCassandraProperties;

/**
 * 异步 cassandra 驱动配置 spring boot default config CassandraAutoConfiguration
 * 使用 Batch 可使 cassandra 实现原子性<Batch默认原子性>，类似于加了 @Transactional，详情如下：
 * Insert insert1 = CassandraTemplate.createInsertQuery("table1", value1, new WriteOptions(ConsistencyLevel.LOCAL_ONE, RetryPolicy.DEFAULT), cassandraConverter);
 * Insert insert2 = CassandraTemplate.createInsertQuery("table2", value2, new WriteOptions(ConsistencyLevel.LOCAL_ONE, RetryPolicy.DEFAULT), cassandraConverter);
 * Batch batch = QueryBuilder.batch(insert1,insert2);
 * cassandraOperations.execute(batch);
 * 
 * 还有轻量级原子性实例如下：
 * 数据插入：
 * INSERT INTO USERS (login, email, name, login_count)
 * values ('jbellis', 'jbellis@datastax.com', 'Jonathan Ellis', 1)
 * IF NOT EXISTS
 * 
 * 数据更新：
 * UPDATE users
 * SET reset_token = null, password = 'newpassword'
 * WHERE login = 'jbellis'
 * IF reset_token = 'some-generated-reset-token'
 * 
 * 轻量级原子性说明：
 * 1，更新的列不必与IF子句中的列相同。
 * 2，轻量级事务仅限于一个分区; 这是保持内部Paxos状态的粒度。作为推论，不同分区中的操作永远不会相互中断。
 * 3，如果操作由于现有值与预期值不匹配而失败，Cassandra将包含当前值，因此您可以决定是否重试或中止，而无需发出额外请求。
 * 4，已添加ConsistencyLevel.SERIAL以允许读取当前（可能未提交）的Paxos状态，而无需提出新的更新。如果SERIAL读取发现正在进行的未提交更新，它会将其作为读取的一部分进行提交。
 * 
 * @author JIANG
 *
 */
@Configuration
@EnableConfigurationProperties({SolakaCassandraProperties.class})
@EnableReactiveCassandraRepositories(basePackages = { BASE_PACKAGES_0 },repositoryBaseClass=BaseReactiveCassandraRepository.class)
public class SolakaCassandraConfiguration extends AbstractReactiveCassandraConfiguration {
	//repository 包路径
	public static final String BASE_PACKAGES_0 = "com.firecode.solaka.repository";
	//entity bean 包路径
	public static final String ENTITY_BASE_PACKAGES_0 = "com.firecode.solaka.domain";

	private final SolakaCassandraProperties cassandraProperties;
	
	public SolakaCassandraConfiguration(SolakaCassandraProperties centerCassandraProperties){
		
		this.cassandraProperties = centerCassandraProperties;
	}

	/**
	 * 应用启动时执行脚本
	 */
	@Override
	protected List<String> getStartupScripts() {

		return cassandraProperties.getStartupScripts();
	}

	/**
	 * cassandra集群连接点，用","分割
	 */
	@Override
	protected String getContactPoints() {

		return String.join(",", cassandraProperties.getContactPoints()
				.toArray(new String[cassandraProperties.getContactPoints().size()]));
	}
	
	/**
	 * 如果表不存在，创建表
	 * @return
	 */
	@Override
	public SchemaAction getSchemaAction() {
		
		return SchemaAction.CREATE_IF_NOT_EXISTS;
	}

	/**
	 * cassandra集群创建工厂
	 */
	@Bean
	@Override
	public CassandraClusterFactoryBean cluster() {
		CassandraClusterFactoryBean cluster = super.cluster();
		cluster.setUsername(cassandraProperties.getUsername());
		cluster.setPassword(cassandraProperties.getPassword());
		return cluster;
	}

	/**
	 * key空间
	 */
	@Override
	protected String getKeyspaceName() {

		return cassandraProperties.getKeyspaceName();
	}

	/**
	 * 连接基本配置
	 */
	@Override
	protected SocketOptions getSocketOptions() {
		PropertyMapper map = PropertyMapper.get();
		SocketOptions socketOptions = new SocketOptions();
		map.from(cassandraProperties::getConnectTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(socketOptions::setConnectTimeoutMillis);
		map.from(cassandraProperties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(socketOptions::setReadTimeoutMillis);
		return socketOptions;
	}

	/**
	 * 连接池配置
	 */
	@Override
	protected PoolingOptions getPoolingOptions() {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties.Pool poolProperties = cassandraProperties.getPool();
		PoolingOptions poolingOptions = new PoolingOptions();
		map.from(poolProperties::getIdleTimeout).whenNonNull().asInt(Duration::getSeconds)
				.to(poolingOptions::setIdleTimeoutSeconds);
		map.from(poolProperties::getPoolTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(poolingOptions::setPoolTimeoutMillis);
		map.from(poolProperties::getHeartbeatInterval).whenNonNull().asInt(Duration::getSeconds)
				.to(poolingOptions::setHeartbeatIntervalSeconds);
		map.from(poolProperties::getMaxQueueSize).to(poolingOptions::setMaxQueueSize);
		return poolingOptions;
	}

	/**
	 * entity bean 包路径
	 */
	@Override
	public String[] getEntityBasePackages() {
		
		return new String[] { ENTITY_BASE_PACKAGES_0 };
	}

	/**
	 * 端口
	 */
	@Override
	protected int getPort() {

		return cassandraProperties.getPort();
	}

}
