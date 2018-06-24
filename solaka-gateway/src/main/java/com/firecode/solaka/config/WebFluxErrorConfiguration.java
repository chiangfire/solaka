package com.firecode.solaka.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import com.firecode.solaka.exception.WebFluxExceptionHandler;

@Configuration
@AutoConfigureAfter(ErrorWebFluxAutoConfiguration.class)
public class WebFluxErrorConfiguration {
	
	private final ApplicationContext applicationContext;

	private final ResourceProperties resourceProperties;

	private final List<ViewResolver> viewResolvers;

	private final ServerCodecConfigurer serverCodecConfigurer;
	
	@Autowired
	private ReactiveCassandraOperations reactiveCassandraTemplate;

	public WebFluxErrorConfiguration(ResourceProperties resourceProperties,
			                        ObjectProvider<List<ViewResolver>> viewResolversProvider,
			                        ServerCodecConfigurer serverCodecConfigurer,
			                        ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.resourceProperties = resourceProperties;
		this.viewResolvers = viewResolversProvider.getIfAvailable(() -> Collections.emptyList());
		this.serverCodecConfigurer = serverCodecConfigurer;
	}

	/**
	 * 错误处理
	 * @param errorAttributes
	 * @return
	 */
	@Bean
	@Order(-1)
	public ErrorWebExceptionHandler errorWebExceptionHandler(Environment env,ErrorAttributes errorAttributes) {
		WebFluxExceptionHandler exceptionHandler = new WebFluxExceptionHandler(errorAttributes,resourceProperties,
				                                                               applicationContext,env.getProperty("spring.application.name"),
				                                                               reactiveCassandraTemplate);
		exceptionHandler.setViewResolvers(this.viewResolvers);
		exceptionHandler.setMessageWriters(this.serverCodecConfigurer.getWriters());
		exceptionHandler.setMessageReaders(this.serverCodecConfigurer.getReaders());
		return exceptionHandler;
	}

}
