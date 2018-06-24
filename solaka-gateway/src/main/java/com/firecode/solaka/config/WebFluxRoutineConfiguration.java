package com.firecode.solaka.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;

import com.firecode.kabouros.webflux.support.CustomCharSequenceEncoder;
import com.firecode.kabouros.webflux.support.CustomEncoderHttpMessageWriter;
import com.firecode.kabouros.webflux.support.CustomJackson2JsonEncoder;

@Configuration
public class WebFluxRoutineConfiguration extends DelegatingWebFluxConfiguration {
	
	
	@Override
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		
		configurer.defaultCodecs().jackson2JsonEncoder(new CustomJackson2JsonEncoder());
	}
	
	@Bean
	@Override
	public ResponseBodyResultHandler responseBodyResultHandler() {
		List<HttpMessageWriter<?>> writers = serverCodecConfigurer().getWriters().stream().map(writer ->{
			if(writer.getWritableMediaTypes().toString().indexOf(MediaType.TEXT_PLAIN_VALUE) != -1){
				if(writer.getWritableMediaTypes().contains(MediaType.ALL)){
					return new CustomEncoderHttpMessageWriter<CharSequence>(CustomCharSequenceEncoder.allMimeTypes());
				}
				return new CustomEncoderHttpMessageWriter<CharSequence>(CustomCharSequenceEncoder.textPlainOnly());
			}
			return writer;
		}).collect(Collectors.toList());
		return new ResponseBodyResultHandler(writers,webFluxContentTypeResolver(), webFluxAdapterRegistry());
	}

}
