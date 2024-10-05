/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.octo.captcha.spring.boot.filter.image;

import com.octo.captcha.spring.boot.filter.FilterConfigUtils;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

/**
 * TODO
 * @author 		： <a href="https://github.com/hiwepy">hiwepy</a>
 */

import com.octo.captcha.module.jmx.JMXRegistrationHelper;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.ManageableCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ImageCaptchaFilter implements Filter {
	public static final String JMX_REGISTERING_NAME = "com.octo.captcha.module.servlet:object=ImageCaptchaFilter";
	public static final String CSV_DELIMITER = ";";
	public static final String CAPTCHA_ERROR_URL_PARAMETER = "CaptchaErrorURL";
	public static final String CAPTCHA_RENDERING_URL_PARAMETER = "CaptchaRenderingURL";
	public static final String CAPTCHA_VERIFICATION_URLS_PARAMETER = "CaptchaVerificationURLs";
	public static final String CAPTCHA_FAIL_URLS_PARAMETER = "CaptchaFailURLs";
	public static final String CAPTCHA_QUESTION_NAME_PARAMETER = "CaptchaQuestionParameterName";
	public static final String CAPTCHA_RESPONSE_PARAMETER_NAME_PARAMETER = "CaptchaChallengeResponseParameterName";
	public static final String CAPTCHA_SERVICE_CLASS_PARAMETER = "ImageCaptchaServiceClass";
	public static final String CAPTCHA_REGISTER_TO_MBEAN_SERVER_PARAMETER = "RegisterToMBeanServer";

	private boolean captchaRegisterToMBeanServer = false;
	private static String captchaQuestionParameterName = null;
	private ImageCaptchaService captchaService = null;
	private static String captchaRenderingURL = null;
	private static String captchaErrorURL = null;
	private static String captchaChallengeResponseParameterName = null;
	protected Hashtable verificationForwards = new Hashtable();
	protected String captchaServiceClassName;

	public ImageCaptchaFilter() {
	}

	public static String getCaptchaRenderingURL() {
		return captchaRenderingURL;
	}

	public static String getCaptchaQuestionParameterName() {
		return captchaQuestionParameterName;
	}

	public static String getCaptchaChallengeResponseParameterName() {
		return captchaChallengeResponseParameterName;
	}

	@Override
	public void init(FilterConfig theFilterConfig) throws ServletException {
		captchaRenderingURL = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaRenderingURL", true);
		captchaErrorURL = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaErrorURL", true);
		String captchaVerificationURLs = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaVerificationURLs", true);
		String captchaForwardErrorURLs = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaFailURLs", true);
		StringTokenizer verificationURLs = new StringTokenizer(captchaVerificationURLs, ";", false);
		StringTokenizer forwardErrorURLs = new StringTokenizer(captchaForwardErrorURLs, ";", false);
		if (verificationURLs.countTokens() != forwardErrorURLs.countTokens()) {
			throw new ServletException("CaptchaVerificationURLs and CaptchaFailURLs values are not consistant in web.xml : there should be exactly one forward error for each verification URL !");
		} else {
			while(verificationURLs.hasMoreTokens()) {
				this.verificationForwards.put(verificationURLs.nextToken(), forwardErrorURLs.nextToken());
			}

			captchaQuestionParameterName = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaQuestionParameterName", true);
			captchaChallengeResponseParameterName = FilterConfigUtils.getStringInitParameter(theFilterConfig, "CaptchaChallengeResponseParameterName", true);
			this.captchaServiceClassName = FilterConfigUtils.getStringInitParameter(theFilterConfig, "ImageCaptchaServiceClass", true);
			this.captchaRegisterToMBeanServer = FilterConfigUtils.getBooleanInitParameter(theFilterConfig, "RegisterToMBeanServer", false);

			try {
				this.captchaService = (ImageCaptchaService)Class.forName(this.captchaServiceClassName).newInstance();
			} catch (InstantiationException var7) {
				InstantiationException e = var7;
				throw new CaptchaServiceException(e);
			} catch (IllegalAccessException var8) {
				IllegalAccessException e = var8;
				throw new CaptchaServiceException(e);
			} catch (ClassNotFoundException var9) {
				ClassNotFoundException e = var9;
				throw new CaptchaServiceException(e);
			}

			if (this.captchaRegisterToMBeanServer && this.captchaService instanceof ManageableCaptchaService) {
				ManageableCaptchaService manageable = (ManageableCaptchaService)this.captchaService;
				JMXRegistrationHelper.registerToMBeanServer(manageable, "com.octo.captcha.module.servlet:object=ImageCaptchaFilter");
			}

		}
	}

	@Override
	public void doFilter(ServletRequest theRequest, ServletResponse theResponse, FilterChain theFilterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)theRequest;
		HttpServletResponse response = (HttpServletResponse)theResponse;
		StringBuffer servletPathBuff = (new StringBuffer()).append(request.getServletPath());
		if (request.getQueryString() != null) {
			servletPathBuff.append("?").append(request.getQueryString());
		}

		String servletPathInfo = servletPathBuff.toString();
		if (servletPathInfo.startsWith(captchaRenderingURL)) {
			try {
				this.generateAndRenderCaptcha(request, response);
			} catch (Throwable var9) {
				Throwable e = var9;
				response.sendRedirect(captchaErrorURL);
				e.printStackTrace();
			}
		} else if (this.verificationForwards.containsKey(servletPathInfo)) {
			this.verifyAnswerToACaptchaChallenge(request, response, servletPathInfo, theFilterChain);
		} else {
			this.addQuestionToRequest(request, response);
			theFilterChain.doFilter(theRequest, theResponse);
		}

	}

	@Override
	public void destroy() {
		if (this.captchaService instanceof ManageableCaptchaService && this.captchaRegisterToMBeanServer) {
			ManageableCaptchaService manageable = (ManageableCaptchaService)this.captchaService;
			JMXRegistrationHelper.unregisterFromMBeanServer("com.octo.captcha.module.servlet:object=ImageCaptchaFilter");
		}

	}

	private void addQuestionToRequest(HttpServletRequest theRequest, HttpServletResponse theResponse) {
		String captchaID = theRequest.getSession().getId();
		String question = this.captchaService.getQuestionForID(captchaID, theRequest.getLocale());
		theRequest.setAttribute(getCaptchaQuestionParameterName(), question);
	}

	private void generateAndRenderCaptcha(HttpServletRequest theRequest, HttpServletResponse theResponse) throws IOException {
		String captchaID = theRequest.getSession().getId();
		byte[] captchaChallengeAsJpeg = null;
		ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();

		try {
			BufferedImage challenge = this.captchaService.getImageChallengeForID(captchaID, theRequest.getLocale());
			JPEGImageEncoder jpegEncoder = JPEGCodec.createJPEGEncoder(jpegOutputStream);
			jpegEncoder.encode(challenge);
		} catch (IllegalArgumentException var8) {
			return;
		} catch (CaptchaServiceException var9) {
			theResponse.sendError(404);
			return;
		}

		captchaChallengeAsJpeg = jpegOutputStream.toByteArray();
		theResponse.setHeader("Cache-Control", "no-store");
		theResponse.setHeader("Pragma", "no-cache");
		theResponse.setDateHeader("Expires", 0L);
		theResponse.setContentType("image/jpeg");
		ServletOutputStream responseOutputStream = theResponse.getOutputStream();
		responseOutputStream.write(captchaChallengeAsJpeg);
	}

	private void verifyAnswerToACaptchaChallenge(HttpServletRequest theRequest, HttpServletResponse theResponse, String theVerificationURL, FilterChain theFilterChain) throws IOException, ServletException {
		String captchaID = theRequest.getSession().getId();
		String challengeResponse = theRequest.getParameter(captchaChallengeResponseParameterName);
		if (challengeResponse == null) {
			this.redirectError(theVerificationURL, theRequest, theResponse);
		} else {
			Boolean isResponseCorrect = Boolean.FALSE;

			try {
				isResponseCorrect = this.captchaService.validateResponseForID(captchaID, challengeResponse);
			} catch (CaptchaServiceException var9) {
			}

			if (isResponseCorrect) {
				this.forwardSuccess(theFilterChain, theRequest, theResponse);
			} else {
				this.redirectError(theVerificationURL, theRequest, theResponse);
			}

		}
	}

	private void redirectError(String theVerificationURL, HttpServletRequest theRequest, HttpServletResponse theResponse) throws ServletException {
		this.removeParametersFromRequest(theRequest);

		try {
			String forwardErrorURL = theRequest.getContextPath() + (String)this.verificationForwards.get(theVerificationURL);
			theResponse.sendRedirect(forwardErrorURL);
		} catch (IOException var5) {
			IOException e = var5;
			throw new ServletException(e);
		}
	}

	private void forwardSuccess(FilterChain theFilterChain, HttpServletRequest theRequest, HttpServletResponse theResponse) throws ServletException {
		this.removeParametersFromRequest(theRequest);

		try {
			theFilterChain.doFilter(theRequest, theResponse);
		} catch (IOException var5) {
			IOException e = var5;
			throw new ServletException(e);
		}
	}

	private void removeParametersFromRequest(HttpServletRequest theRequest) {
		theRequest.removeAttribute(getCaptchaChallengeResponseParameterName());
		theRequest.removeAttribute(getCaptchaQuestionParameterName());
	}
}
