package com.uit.api.common;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientHolder {
    private static RestClient restClient;

    private RestClientHolder(RestClient restClient) {
        RestClientHolder.restClient = restClient;
    }

    public static RestClient getRestClient() {
        return restClient;
    }
}
