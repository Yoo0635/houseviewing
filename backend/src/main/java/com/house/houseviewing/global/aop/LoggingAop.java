package com.house.houseviewing.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import java.util.Arrays;

@Aspect
@Slf4j
@Component
public class LoggingAop {

    private final String PACKAGE_NAME = "com.house.houseviewing";

    @Around("execution(* com.house.houseviewing.domain..*Controller.*(..))")
    public Object logging(ProceedingJoinPoint joinPoint) throws Throwable{
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("-----------------------------------------");
        log.info("[REQUEST] Method : {}", methodName);
        log.info("[PARAMS] : {}", Arrays.toString(args));

        StopWatch sw = new StopWatch();
        sw.start();

        try{
            Object result = joinPoint.proceed();
            sw.stop();
            long timeMs = sw.getTotalTimeMillis();

            if (result instanceof ResponseEntity){
                ResponseEntity<?> response = (ResponseEntity<?>) result;
                log.info("[RESPONSE] Status : {}", response.getStatusCode());
                log.info("[BODY] : {}", response.getBody());
            }
            else{
                log.info("[RESPONSE] Return : {}", result);
            }
            log.info("[TIME] : {}ms", timeMs);
            log.info("-----------------------------------------");

            return result;
        } catch (Throwable e){
            if(sw.isRunning())
                sw.stop();

            long timeMs = sw.getTotalTimeMillis();

            log.error("[ERROR] Method : {}",methodName);
            log.error("[TYPE] : {}", e.getClass().getSimpleName());
            log.error("[MESSAGE] : {}", e.getMessage() );
            log.error("[TIME] : {}ms", timeMs);
            log.info("-----------------------------------------");

            throw e;
        }
    }
}
