//package com.example.demo.aop;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Pointcut;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import javax.servlet.http.HttpServletRequest;
//import java.lang.reflect.Method;
//import java.util.concurrent.TimeUnit;
//
//@Component
//@Aspect
//@Order(100)
//public class RepeatSubmitInterceptor {
//
//    private static final String SUFFIX = "REQUEST_";
//
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    // 定义 注解 类型的切点
//    @Pointcut("@annotation(com.example.demo.aop.IsRepeatSubmit)")
//    public void arrPointcut() {}
//
//    // 实现过滤重复请求功能
//    @Around("arrPointcut()")
//    public Object arrBusiness(ProceedingJoinPoint joinPoint) {
//        // 获取 redis key，由 session ID 和 请求URI 构成
//        ServletRequestAttributes sra = (ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
//        HttpServletRequest request = sra.getRequest();
//        String key = SUFFIX + request.getSession().getId() + "_" + request.getRequestURI();
//
//        // 获取方法的 AvoidRepeatRequest 注解
//        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
//        IsRepeatSubmit arr = method.getAnnotation(IsRepeatSubmit.class);
//
//        // 判断是否是重复的请求
//        if (!redisTemplate.opsForValue().setIfAbsent(key, 1, arr.intervalTime(), TimeUnit.SECONDS)) {
//            // 已发起过请求
//            System.out.println("重复请求");
//            return arr.msg();
//        }
//
//        try {
//            // 非重复请求，执行业务代码
//            return joinPoint.proceed();
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//            return "error";
//        }
//    }
//}
