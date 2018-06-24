package com.firecode.solaka.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import com.firecode.kabouros.cassandra.BaseCassandraProperties;

@Primary
@Configuration
@PropertySource("classpath:cassandra.properties")
@ConfigurationProperties(prefix = "solaka.data.cassandra")
public class SolakaCassandraProperties extends BaseCassandraProperties {

}
