package org.darius.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
        org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "org.darius.userservice.repositories")
@EntityScan(basePackages = "org.darius.userservice.entities")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
