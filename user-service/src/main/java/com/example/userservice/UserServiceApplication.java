package com.example.userservice;

import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedUsers(UserRepository repo) {
        return args -> {
            repo.save(User.builder().name("Alice Johnson").email("alice@example.com").phone("555-0101").build());
            repo.save(User.builder().name("Bob Smith").email("bob@example.com").phone("555-0102").build());
            repo.save(User.builder().name("Carol White").email("carol@example.com").phone("555-0103").build());
            log.info("Seeded 3 users into the database.");
        };
    }
}
