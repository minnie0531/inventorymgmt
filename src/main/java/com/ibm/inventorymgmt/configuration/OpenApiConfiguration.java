package com.ibm.inventorymgmt.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfiguration {
	
	  @Bean
	  public OpenAPI openAPI() {
	    Info info = new Info().title("Inventory check service").version("v1")
	            .description("Inventory check service")
	            .termsOfService("http://tech.garage.korea.com")
	            .contact(new Contact().name("Technology Korea Garage").email("korea.garage@ibm.com"))
	            .license(new License().name("Apache License Version 2.0").url("http://www.apache.org/licenses/LICENSE-2.0"));

	    return new OpenAPI()
	            .components(new Components())
                    .addServersItem(new Server().url("http://minnie.garagekr.com/api/"))
	            .info(info);
	  }

}
