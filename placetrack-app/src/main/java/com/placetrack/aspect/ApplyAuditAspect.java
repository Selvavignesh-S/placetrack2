package com.placetrack.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP audit log: records every apply attempt (who, which drive, outcome, time taken)
 * without a single logging line inside ApplicationService.
 *
 * Aspect   = this class.
 * Pointcut = the execution(...) expression: which methods to intercept.
 * Advice   = the audit() method: what to do around them.
 * Join point = one actual call of apply(...).
 * Spring weaves it in by wrapping ApplicationService in a proxy object.
 */
@Aspect
@Component
public class ApplyAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(ApplyAuditAspect.class);

    @Around("execution(* com.placetrack.service.ApplicationService.apply(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs(); // apply(studentId, driveId)
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed(); // run the real apply()
            log.info("AUDIT apply student={} drive={} outcome=SUCCESS time={}ms",
                    args[0], args[1], System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            log.info("AUDIT apply student={} drive={} outcome=BLOCKED reason=\"{}\" time={}ms",
                    args[0], args[1], e.getMessage(), System.currentTimeMillis() - start);
            throw e; // the caller still gets the exception
        }
    }
}
