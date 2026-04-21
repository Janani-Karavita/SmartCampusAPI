package com.smartcampus.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 *
 * The @ApplicationPath annotation establishes the versioned root of the API.
 * All resource paths declared with @Path are relative to this base URI,
 * resulting in endpoints such as GET /api/v1/rooms.
 *
 * When using Jersey with Grizzly (embedded server), this class is registered
 * in Main.java – no web.xml deployment descriptor is required.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-scans the classpath for @Provider and @Path classes when
    // the Application subclass returns empty sets from getClasses() /
    // getSingletons(), which is the default behaviour of the base class.
}
