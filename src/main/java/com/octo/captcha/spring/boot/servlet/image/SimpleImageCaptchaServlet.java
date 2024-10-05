package com.octo.captcha.spring.boot.servlet.image;


import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import jakarta.servlet.Servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SimpleImageCaptchaServlet extends HttpServlet implements Servlet {

    public static ImageCaptchaService service = new DefaultManageableImageCaptchaService();

    public SimpleImageCaptchaServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.setDateHeader("Expires", 0L);
        httpServletResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        httpServletResponse.addHeader("Cache-Control", "post-check=0, pre-check=0");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setContentType("image/jpeg");
        BufferedImage bi = service.getImageChallengeForID(httpServletRequest.getSession(true).getId());
        ServletOutputStream out = httpServletResponse.getOutputStream();
        ImageIO.write(bi, "jpg", out);

        try {
            out.flush();
        } finally {
            out.close();
        }

    }

    public static boolean validateResponse(HttpServletRequest request, String userCaptchaResponse) {
        if (request.getSession(false) == null) {
            return false;
        } else {
            boolean validated = false;

            try {
                validated = service.validateResponseForID(request.getSession().getId(), userCaptchaResponse);
            } catch (CaptchaServiceException var4) {
                CaptchaServiceException e = var4;
                e.printStackTrace();
            }

            return validated;
        }
    }
}
