package com.audit.transaction_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import io.r2dbc.spi.ConnectionFactory;

@Configuration
public class DatabaseConfig {

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        // Instantiates Spring's reactive database initializer manager
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();

        //  Injects H2 reactive ConnectionFactory so it knows which database to connect to
        initializer.setConnectionFactory(connectionFactory);

        // Creates a container to hold and execute multiple SQL scripts
        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();

        // Adds schema.sql file to the execution queue
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        // Attaches the ordered script queue to the initializer manager
        initializer.setDatabasePopulator(populator);

        // Returns the fully configured bean so Spring executes it automatically right before opening network traffic ports
        return initializer;
    }
}