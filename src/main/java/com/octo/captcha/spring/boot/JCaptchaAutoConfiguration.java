package com.octo.captcha.spring.boot;

import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.spring.boot.filter.image.ImageCaptchaFilter;
import com.octo.captcha.spring.boot.servlet.image.SimpleImageCaptchaServlet;
import jakarta.servlet.ServletException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ ImageCaptchaService.class })
@EnableConfigurationProperties(JCaptchaProperties.class)
public class JCaptchaAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ImageCaptchaService.class)
	public ImageCaptchaService captchaService(JCaptchaProperties properties) {
		
		ImageCaptchaService captchaService = new DefaultManageableImageCaptchaService(); 
		
		return captchaService;
	}

	@Bean
	@ConditionalOnMissingBean(name = "jcaptchaServlet")
	@ConditionalOnProperty(prefix = JCaptchaProperties.PREFIX, value = "type", havingValue = "servlet")
	public ServletRegistrationBean<SimpleImageCaptchaServlet> servletRegistrationBean(JCaptchaProperties properties, ImageCaptchaService imageCaptchaService) throws ServletException {

		ServletRegistrationBean<SimpleImageCaptchaServlet> registrationBean = new ServletRegistrationBean<SimpleImageCaptchaServlet>();
		
		SimpleImageCaptchaServlet captchaServlet = new SimpleImageCaptchaServlet();

		SimpleImageCaptchaServlet.service = imageCaptchaService;
		
		registrationBean.setServlet(captchaServlet);
		registrationBean.addUrlMappings(properties.getCaptchaServletPattern());

		return registrationBean;
	}
	
	@Bean
	@ConditionalOnMissingBean(name = "jcaptchaFilter")
	@ConditionalOnProperty(prefix = JCaptchaProperties.PREFIX, value = "type", havingValue = "filter")
	public FilterRegistrationBean<ImageCaptchaFilter> imageCaptchaFilter(JCaptchaProperties properties,
																		 ImageCaptchaService captchaService){
		
		FilterRegistrationBean<ImageCaptchaFilter> registrationBean = new FilterRegistrationBean<ImageCaptchaFilter>();
		registrationBean.setFilter(new ImageCaptchaFilter());

		// 默认参数
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_ERROR_URL_PARAMETER, properties.getCaptchaErrorURL());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_RENDERING_URL_PARAMETER, properties.getCaptchaErrorURL());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_VERIFICATION_URLS_PARAMETER, properties.getCaptchaVerificationURLs());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_FAIL_URLS_PARAMETER, properties.getCaptchaForwardErrorURLs());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_QUESTION_NAME_PARAMETER, properties.getCaptchaQuestionParameterName());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_RESPONSE_PARAMETER_NAME_PARAMETER, properties.getCaptchaChallengeResponseParameterName());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_SERVICE_CLASS_PARAMETER, captchaService.getClass().getName());
		registrationBean.addInitParameter(ImageCaptchaFilter.CAPTCHA_REGISTER_TO_MBEAN_SERVER_PARAMETER, Boolean.toString(properties.isCaptchaRegisterToMBeanServer()));
		
		registrationBean.addUrlPatterns(properties.getCaptchaFilterPattern());
		registrationBean.setEnabled(true); 
	    return registrationBean;
	}
	
	
}
