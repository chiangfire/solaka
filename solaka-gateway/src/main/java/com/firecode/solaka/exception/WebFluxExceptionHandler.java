package com.firecode.solaka.exception;

import java.util.Date;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import com.firecode.kabouros.common.keygen.IPIdGenerator;
import com.firecode.kabouros.common.util.ErrorUtils;
import com.firecode.kabouros.webflux.ResponseHelper;
import com.firecode.solaka.domain.ErrorLog;

import reactor.core.publisher.Mono;

/**
 * WebFlux 错误处理
 * response.writeAndFlushWith(Mono.just(Mono.just(response.bufferFactory().wrap(ex.getMessage().getBytes(CharsetUtil.UTF_8)))))
 * @author JIANG
 */
public class WebFluxExceptionHandler extends AbstractErrorWebExceptionHandler {
	
	
	private static final Log logger = LogFactory.getLog(WebFluxExceptionHandler.class);
	private final ReactiveCassandraOperations reactiveCassandraTemplate;
	private final String applicationName;
	

	
	public WebFluxExceptionHandler(ErrorAttributes errorAttributes, 
			                       ResourceProperties resourceProperties,
			                       ApplicationContext applicationContext,
			                       String applicationName,
			                       ReactiveCassandraOperations reactiveCassandraTemplate) {
		super(errorAttributes, resourceProperties, applicationContext);
		Assert.hasLength(applicationName, "Application name must not be null");
		Assert.notNull(reactiveCassandraTemplate, "ReactiveCassandraOperations must not be null");
		this.applicationName = applicationName;
		this.reactiveCassandraTemplate = reactiveCassandraTemplate;
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		
		return RouterFunctions.route(RequestPredicates.all(), (request) -> this.renderErrorResponse(request,errorAttributes));
	}
	
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request,ErrorAttributes errorAttributes) {
		Throwable error = errorAttributes.getError(request);
		HttpStatus errorStatus = determineHttpStatus(error);
		return saveLog(request, error,errorStatus).flatMap(errorLog -> {
			String result = ResponseHelper.getResultStr(errorLog.getStatus(),errorLog.getError(),errorLog.getId().toString());
			return ServerResponse.status(errorLog.getStatus())
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(BodyInserters.fromObject(result))
					.doOnNext((resp) -> logError(request, errorStatus));
		});
	}
	
	private Mono<ErrorLog> saveLog(ServerRequest request,Throwable error,HttpStatus errorStatus){
		ErrorLog log = new ErrorLog();
		log.setStatus(errorStatus.value());
		log.setException(error.getClass().getName());
		log.setError(errorStatus.getReasonPhrase());
		log.setMessage(determineMessage(error));
		log.setTimestamp(new Date());
		log.setPath(request.uri().toString());
		if(!HttpStatus.NOT_FOUND.equals(errorStatus)){
			log.setTrace(ErrorUtils.getPrintStackTrace(error));
		}
		log.setId(Long.parseLong(IPIdGenerator.getInstance().generate().toString()));
		log.setMethodName(request.methodName());
		log.setApplicationName(applicationName);
		return reactiveCassandraTemplate.insert(log);
	}
	
	protected void logError(ServerRequest request, HttpStatus errorStatus) {
		Throwable ex = getError(request);
		log(request, ex, (errorStatus.is5xxServerError() ? logger::error : logger::warn));
	}

	private void log(ServerRequest request, Throwable ex,BiConsumer<Object, Throwable> logger) {
		if (ex instanceof ResponseStatusException) {
			logger.accept(buildMessage(request, ex), null);
		}
		else {
			logger.accept(buildMessage(request, null), ex);
		}
	}
	
	private String buildMessage(ServerRequest request, Throwable ex) {
		StringBuilder message = new StringBuilder("Failed to handle request [");
		message.append(request.methodName());
		message.append(" ");
		message.append(request.uri());
		message.append("]");
		if (ex != null) {
			message.append(": ");
			message.append(ex.getMessage());
		}
		return message.toString();
	}
	
	private HttpStatus determineHttpStatus(Throwable error) {
		if (error instanceof ResponseStatusException) {
			return ((ResponseStatusException) error).getStatus();
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
	
	private String determineMessage(Throwable error) {
		if (error instanceof WebExchangeBindException) {
			return error.getMessage();
		}
		if (error instanceof ResponseStatusException) {
			return ((ResponseStatusException) error).getReason();
		}
		return error.getMessage();
	}
	
}
