package com.jeesuite.passport.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.jeesuite.springweb.interceptor.CorsEnableInterceptor;

@Configuration
public class CostomWebMvcConfigurerAdapter extends WebMvcConfigurerAdapter {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CorsEnableInterceptor()).addPathPatterns("/user/**","/sso/get_setcookie_list");
        super.addInterceptors(registry);
	}


}
