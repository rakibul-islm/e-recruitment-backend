package com.bd.erecruitment.exception;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ExceptionLoggingAspect {

	private final ExceptionLogWriter exceptionLogWriter;

	@AfterThrowing(pointcut = "execution(public * com.bd.erecruitment.service.impl.MailServiceImpl.*(..))", throwing = "ex")
	public void logMailServiceFailure(JoinPoint joinPoint, Exception ex) {
		exceptionLogWriter.log(ex, 0, ex.getMessage(), joinPoint.getSignature().toShortString());
	}
}
