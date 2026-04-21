package com.smartcampus.mapper;

import com.smartcampus.api.ApiError;
import com.smartcampus.exception.ApiException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class APIExceptionMapper implements ExceptionMapper<ApiException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ApiException exception) {
        int status = mapStatus(exception);
        String path = uriInfo != null && uriInfo.getRequestUri() != null ? uriInfo.getRequestUri().getPath() : null;

        ApiError body = new ApiError(exception.getCode(), exception.getMessage(), status, path);
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private int mapStatus(ApiException e) {
        String code = e.getCode();
        if ("BAD_REQUEST".equals(code)) return 400;
        if ("FORBIDDEN".equals(code)) return 403;
        if ("NOT_FOUND".equals(code)) return 404;
        if ("CONFLICT".equals(code)) return 409;
        if ("UNPROCESSABLE_ENTITY".equals(code)) return 422;
        if("ROOM_NOT_EMPTY".equals(code)) return 409;
        if("LINKED_RESOURCE_NOT_FOUND".equals(code)) return 422;
        return 400;
    }
}