package com.example.grocery_billing.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(HttpServletRequest request, Exception ex) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String errorLog = "Error occurred at: " + LocalDateTime.now() + "\n" +
                              "URL: " + request.getRequestURL() + "\n" +
                              "Exception: " + ex.getMessage() + "\n" +
                              sw.toString() + "\n-------------------------------------------------\n";
            Files.write(Paths.get("error.txt"), errorLog.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        
        // Let Spring handle it normally so the user still sees the white label error page
        // But we get the stack trace logged!
        throw new RuntimeException(ex);
    }
}
