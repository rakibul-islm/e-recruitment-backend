package com.bd.erecruitment.exception;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Auto-persists exceptions that escape genuinely infrastructural methods (external API calls, IO)
 * via {@link ExceptionLogWriter}, so those call sites don't need a manual log call.
 * <p>
 * Pointcuts here are scoped per-class deliberately, NOT blanket over all service methods:
 * a blanket pointcut would also catch intentional business/validation exceptions
 * ({@code ApiException}, {@code ServiceException}, {@code UsernameNotFoundException}, ...),
 * which GlobalExceptionHandler already handles without persisting by design, and would
 * double-log any truly-uncaught exception that GlobalExceptionHandler also persists at the
 * controller boundary. Catch blocks that swallow an exception and continue (never letting it
 * leave the method) are invisible to AOP and still need an explicit ExceptionLogWriter call.
 */
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
