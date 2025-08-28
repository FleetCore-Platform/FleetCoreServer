package io.levysworks.Managers.Database;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
public class DatabaseManager {
    @PostConstruct
    public void init() {}
}
