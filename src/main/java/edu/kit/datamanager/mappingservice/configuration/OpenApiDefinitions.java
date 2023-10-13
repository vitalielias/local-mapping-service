/*
 * Copyright 2022 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.mappingservice.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class provides the OpenAPI definition for the mapping-service.
 *
 * @author maximilianiKIT
 */
@Configuration
public class OpenApiDefinitions {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Mapping-Service - RESTful API").
                        description("This webpage describes the RESTful API of the KIT Data Manager Mapping-Service.").
                        version("0.1").
                        contact(
                                new Contact().
                                        name("KIT Data Manager Support").
                                        url("https://github.com/kit-data-manager").
                                        email("support@datamanager.kit.edu")).
                        license(
                                new License().
                                        name("Apache 2.0").
                                        url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                );
    }

}