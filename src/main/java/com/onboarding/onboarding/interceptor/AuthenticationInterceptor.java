package com.onboarding.onboarding.interceptor;

import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.onboarding.onboarding.exception.ConnectionRefusedException;
import com.onboarding.onboarding.exception.GlobalException;
import com.onboarding.onboarding.exception.InvalidTokenException;
import com.onboarding.onboarding.exception.ServerErrorException;
import com.onboarding.onboarding.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 12:36 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private WalletService walletService;

    private static String username;

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        AuthenticationInterceptor.username = username;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("Prehandle");
        if(shouldSkipUrl(request)){
            return true;
        }
        String token = request.getHeader("Authorization");
        if(token!=null && token.startsWith("Bearer")){
            try {
                token = token.substring(7);
                walletService.validateToken(token)
                        .map(apiResponse -> {
                            if(apiResponse.getMessage().equalsIgnoreCase("valid token") && apiResponse.isSuccess()){
                                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                                System.out.println(apiResponse);
                                setUsername(apiResponse.getUsername());
                                return true;
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                return false;
                            }
                        })
                        .onErrorResume(error ->{
                            if(error.getMessage().equals("Invalid token")){
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                            else if(error.getMessage().equals("Server error")){
                                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }
                            return Mono.just(false);
                        })
                        .block();
            }
            catch (InvalidTokenException | ServerErrorException | ConnectionRefusedException ex){
                throw ex;
            }
        }

        if(token==null){
            throw  new InvalidTokenException("Authorization token is empty");
        }
        if(response.getStatus()==401){
            throw new InvalidTokenException("Token is invalid or expired");
        }
        else if(response.getStatus()==500){
            throw new ServerErrorException("Internal server error");
        }
        else if(response.getStatus()==503){
            throw new ConnectionRefusedException("Connection refused to the server");
        }
        else if(response.getStatus()==202){
            return true;
        }
        else {
            throw new GlobalException("Global Exception");
        }
    }

    public boolean shouldSkipUrl(HttpServletRequest request){
        String requestUrl = request.getRequestURI();
        return requestUrl.contains("/swagger") || requestUrl.contains("/v3/api-docs");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
