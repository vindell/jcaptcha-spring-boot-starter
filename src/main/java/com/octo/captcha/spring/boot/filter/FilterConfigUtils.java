package com.octo.captcha.spring.boot.filter;


import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

public class FilterConfigUtils {
    public FilterConfigUtils() {
    }

    public static String getStringInitParameter(FilterConfig theFilterConfig, String theInitParameterName, boolean isMandatory) throws ServletException {
        String returnedValue = theFilterConfig.getInitParameter(theInitParameterName);
        if (isMandatory && returnedValue == null) {
            throw new ServletException(theInitParameterName + " parameter must be declared for " + theFilterConfig.getFilterName() + " in web.xml");
        } else {
            return returnedValue;
        }
    }

    public static Integer getIntegerInitParameter(FilterConfig theFilterConfig, String theInitParameterName, boolean isMandatory, int theMinValue, int theMaxValue) throws ServletException {
        Integer returnedValue = null;
        String returnedValueAsString = theFilterConfig.getInitParameter(theInitParameterName);
        if (isMandatory && returnedValueAsString == null) {
            throw new ServletException(theInitParameterName + " parameter must be declared for " + theFilterConfig.getFilterName() + " in web.xml");
        } else {
            try {
                returnedValue = new Integer(returnedValueAsString);
            } catch (NumberFormatException var8) {
                throw new ServletException(theInitParameterName + " parameter must be an integer value " + theFilterConfig.getFilterName() + " in web.xml");
            }

            if (returnedValue >= theMinValue && returnedValue <= theMaxValue) {
                return returnedValue;
            } else {
                throw new ServletException(theInitParameterName + " parameter for " + theFilterConfig.getFilterName() + " in web.xml must be >= " + theMinValue + " and <= " + theMaxValue);
            }
        }
    }

    public static boolean getBooleanInitParameter(FilterConfig theFilterConfig, String theInitParameterName, boolean isMandatory) throws ServletException {
        String returnedValueAsString = theFilterConfig.getInitParameter(theInitParameterName);
        if (isMandatory && returnedValueAsString == null) {
            throw new ServletException(theInitParameterName + " parameter must be declared for " + theFilterConfig.getFilterName() + " in web.xml");
        } else {
            boolean returnedValue = false;
            if (returnedValueAsString != null) {
                returnedValue = new Boolean(returnedValueAsString);
            }

            return returnedValue;
        }
    }
}
