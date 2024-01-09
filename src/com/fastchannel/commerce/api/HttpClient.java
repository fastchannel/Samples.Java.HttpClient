package com.fastchannel.commerce.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpClient {
    private String baseApiAddress;
    private String subscriptionKey;
    private String accessToken;

    public HttpClient(String baseApiAddress, String subscriptionKey) {
        this.baseApiAddress = baseApiAddress;
        this.subscriptionKey = subscriptionKey;
    }

    public void authenticate(String tokenEndpoint, String clientId, String clientSecret, String clientScope) {
        HttpURLConnection tokenConnection = null;

        try {
            String requestBody = "grant_type=client_credentials" +
                    "&scope=" + URLEncoder.encode(clientScope, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            URI tokenUri = new URI(tokenEndpoint);

            tokenConnection = (HttpURLConnection) tokenUri.toURL().openConnection();
            tokenConnection.setRequestMethod("POST");
            tokenConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            tokenConnection.setDoOutput(true);

            tokenConnection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

            StringBuilder tokenResponse = new StringBuilder();

            try(BufferedReader tokenReader = new BufferedReader(new InputStreamReader(tokenConnection.getInputStream()))){
                String tokenLine;
                while ((tokenLine = tokenReader.readLine()) != null) {
                    tokenResponse.append(tokenLine);
                }
            }

            accessToken = extractAccessToken(tokenResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (tokenConnection != null) {
                tokenConnection.disconnect();
            }
        }
    }

    public String get(String resourcePath) {
        return makeApiCall("GET", resourcePath, null);
    }

    public String post(String resourcePath, String requestBody) {
        return makeApiCall("POST", resourcePath, requestBody);
    }

    public String put(String resourcePath, String requestBody) {
        return makeApiCall("PUT", resourcePath, requestBody);
    }

    public String patch(String resourcePath, String requestBody) {
        return makeApiCall("PATCH", resourcePath, requestBody);
    }

    public String delete(String resourcePath) {
        return makeApiCall("DELETE", resourcePath, null);
    }

    private String makeApiCall(String httpMethod, String resourcePath, String requestBody) {
        HttpURLConnection apiConnection = null;

        try {
            URI apiResourceUri = new URI(baseApiAddress + resourcePath);

            apiConnection = (HttpURLConnection) apiResourceUri.toURL().openConnection();
            apiConnection.setRequestMethod(httpMethod);
            apiConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

            // Set the additional Subscription-Key header
            apiConnection.setRequestProperty("Subscription-Key", subscriptionKey);

            // Tells the server we want an application/json response
            apiConnection.setRequestProperty("Accept", "application/json");

            // Set the Content-Type header for requests with a request body
            if (requestBody != null) {
                apiConnection.setRequestProperty("Content-Type", "application/json");
                apiConnection.setDoOutput(true);

                try (OutputStream outputStream = apiConnection.getOutputStream()) {
                    outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            }

            StringBuilder apiResponse = new StringBuilder();
            try (BufferedReader apiReader = new BufferedReader(new InputStreamReader(apiConnection.getInputStream()))) {
                String apiLine;
                while ((apiLine = apiReader.readLine()) != null) {
                    apiResponse.append(apiLine);
                }
            }
            return apiResponse.toString();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (apiConnection != null) {
                apiConnection.disconnect();
            }
        }
    }

    // Helper method for extracting Access Token from JSON response
    private String extractAccessToken(String tokenResponse) {
        String accessTokenKey = "\"access_token\":\"";
        int startIndex = tokenResponse.indexOf(accessTokenKey) + accessTokenKey.length();
        int endIndex = tokenResponse.indexOf("\"", startIndex);
        return tokenResponse.substring(startIndex, endIndex);
    }

    public static void main(String[] args) {

        // OAuth 2.0 Client Credentials Grant (used for authentication)
        String clientId = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
        String clientSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        String clientScope = "api://xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/.default";
        String tokenEndpoint = "https://login.microsoftonline.com/fastchannel.com/oauth2/v2.0/token";

        // Fastchannel Web API base address and your personal API Subscription Key.
        String baseApiAddress = "https://api.commerce.fastchannel.com/";
        String subscriptionKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        // One single HTTP Client instance can make multiple authenticated HTTP requests.
        HttpClient httpClient = new HttpClient(baseApiAddress, subscriptionKey);

        // Authenticate only once, and then reuse the same Access Token on multiple API calls.
        httpClient.authenticate(tokenEndpoint, clientId, clientSecret, clientScope);

        // Simple API call: HTTP/GET
        // https://developers.commerce.fastchannel.com/api-details#api=Stock-v1&operation=getproductstock
        String getResourcePath = "stock-management/v1/stock/" + getStockResourceCode(0);
        String getResponse = httpClient.get(getResourcePath);
        System.out.println("[GET] HTTP Response for " + getResourcePath + ":");
        System.out.println(getResponse);
        System.out.println("");

        // Simple API call: HTTP/PUT
        // https://developers.commerce.fastchannel.com/api-details#api=Stock-v1&operation=setproductstock
        String putResourcePath = "stock-management/v1/stock/" + getStockResourceCode(0);
        String putRequestBody = "{\"StorageId\":18,\"Quantity\":0}";
        String putResponse = httpClient.put(putResourcePath, putRequestBody);
        System.out.println("[PUT] HTTP Response for " + putResourcePath + ":");
        System.out.println(putResponse);
        System.out.println("");

        // Multiple API calls: sequential HTTP/PUT requests
        // https://developers.commerce.fastchannel.com/api-details#api=Stock-v1&operation=setproductstock
        for (int i = 0; i < 100; i++) {
            String apiResourcePath = "stock-management/v1/stock/" + getStockResourceCode(i);
            String apiRequestBody = "{\"StorageId\":18,\"Quantity\":999}";
            String apiResponse = httpClient.put(apiResourcePath, apiRequestBody);
            System.out.println("[PUT] HTTP Response nº " + i + " for " + apiResourcePath + ":");
            System.out.println(apiResponse);
            System.out.println("");

            try {
                // Add a 500ms delay between each API Call
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    // Helper method for random Product Code definition, based on index iteration.
    private static String getStockResourceCode(int index) {
        switch (index % 10) {
            case 0:
                return "32004210";
            case 1:
                return "31232810";
            case 2:
                return "32004222";
            case 3:
                return "32003810";
            case 4:
                return "31236920";
            case 5:
                return "31239920";
            case 6:
                return "33140121";
            case 7:
                return "31012651";
            case 8:
                return "32004022";
            case 9:
                return "33008910";
        }

        return "00000000";
    }
}
