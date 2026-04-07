package com.example.attendance_app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Configure OpenAPI metadata
    ----------------------------------------------------------------
    @parameter: -
    @Returnvalue: OpenAPI
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Bean
    OpenAPI attendanceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Attendance Tracking API")
                .version("v1")
                .description("API for recording and querying employee site attendance check-ins"));
    }
}
