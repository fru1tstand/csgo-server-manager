package me.fru1t.csgo_server_manager;

import com.google.common.collect.ImmutableList;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.StandardTemplateModeHandlers;

import java.util.List;
import java.util.Map;

/**
 * Entrypoint to the CSGO Server Manager application. This main method starts up the web server.
 */
@SpringBootApplication
@Controller
@Configuration
public class CsgoServerManagerApplication {
    private static final String DEBUG_ARG = "-debug";

    private static ApplicationContext context;
    private static boolean isDebugging;

    public static void main(String[] args) {
        context = SpringApplication.run(CsgoServerManagerApplication.class, args);

        // Parse params
        List<String> argList = ImmutableList.copyOf(args);

        // Debugging?
        isDebugging = argList.contains(DEBUG_ARG);
    }

    @RequestMapping(value = "/")
    public String index(Map<String, Object> model) {
        return "index";
    }
}
