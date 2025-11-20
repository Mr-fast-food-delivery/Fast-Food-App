package com.phegon.FoodApp.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    private int statusCode; // e.g., "200", "404"
    private String message; // Additional information about the response
    private T data; // The actual data payload
    private Map<String, Serializable> meta;
    public static Object error(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'error'");
    }
    public static <T> Response<T> error(String message, int statusCode) {
    return Response.<T>builder()
            .statusCode(statusCode)
            .message(message)
            .build();
}

}
