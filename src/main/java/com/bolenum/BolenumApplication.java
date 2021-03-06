package com.bolenum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bolenum.services.common.coin.Erc20TokenService;

import io.swagger.annotations.Api;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
@EnableAsync
public class BolenumApplication {

	/**
	 * This method is use for taskExecutor and return a TaskExecutor.
	 * @param Nothing.
	 * @return TaskExecutor.
	 */
	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setThreadNamePrefix("CustomeThread-");
		return executor;
	}

	/**
	 * This is main method of application.
	 * @param args.
	 * @return Nothing.
	 */
	public static void main(String[] args) {
		SpringApplication.run(BolenumApplication.class, args);
	}

	/**
	 * This method is use for get api Docket.
	 * @param Nothing.
	 * @return Docket.
	 */
	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.withClassAnnotation(Api.class)).paths(PathSelectors.any()).build()
				.pathMapping("/").apiInfo(apiInfo()).useDefaultResponseMessages(false);
	}

	/**
	 * This method is use for get apiInfo
	 * @param Nothing.
	 * @return ApiInfo.
	 */
	public ApiInfo apiInfo() {
		final ApiInfoBuilder builder = new ApiInfoBuilder();
		builder.title("Bolenum Exchange API").version("1.0").license("(C) Copyright Bolenum")
				.description("The API provides a platform to query build Bolenum exchange api").contact(new Contact(
						"Chandan", "http://oodlestechnologies.com", "chandan.kumar@oodlestechnologies.com"));
		return builder.build();
	}

	@Autowired
	private Erc20TokenService erc20TokenService;

	/**
	 * This method is use for fetchCoinPrice. 
	 * @param Nothing.
	 * @return Nothing.
	 */
	@Scheduled(fixedRate = 30 * 1000)
	public void fetchCoinPrice() {
		erc20TokenService.sendUserTokenToAdmin();
	}
}
