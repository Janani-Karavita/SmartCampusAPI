package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Minimal request/response logging filter for debugging purposes.
 */

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_NANOS = "smartcampus.startNanos";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_NANOS, System.nanoTime());
        System.out.println("--> " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        long start = 0L;
        Object prop = requestContext.getProperty(START_NANOS);
        if (prop instanceof Long) {
            start = (Long) prop;
        }
        long elapsedMs = start == 0L ? -1L : (System.nanoTime() - start) / 1_000_000L;

        String path = requestContext.getUriInfo().getRequestUri().toString();
        System.out.println("<-- " + requestContext.getMethod() + " " + path + " " + responseContext.getStatus() + " (" + elapsedMs + "ms)");
    }
}