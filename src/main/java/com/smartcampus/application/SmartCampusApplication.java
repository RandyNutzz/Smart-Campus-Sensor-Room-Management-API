package com.smartcampus.application;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.exception.*;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.resource.DiscoveryResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application bootstrap class.
 *
 * Lifecycle Note: By default, JAX-RS creates a new instance of each
 * resource class per request (per-request scope). This means resource
 * classes are NOT singletons. To safely share in-memory data structures
 * (like our HashMaps), we use a singleton DataStore class that is
 * accessed statically, ensuring all request instances operate on the
 * same shared state without data loss between requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception Mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
