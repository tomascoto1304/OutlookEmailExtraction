package com.mycompany;



import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Authenticator {
    // Azure application credentials
    private static final String TENANT_ID = "TENANT_ID";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CLIENT_SECRET = "CLIENT_SECRET";

    public static String getAccessToken() throws Exception {
        // URL to obtain the access token from Azure
        String tokenUrl = "https://login.microsoftonline.com/" + TENANT_ID + "/oauth2/v2.0/token";
       
        // Parameters to obtain the token
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(tokenUrl);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            // Parameters to obtain the token
            String body = "client_id=" + CLIENT_ID +
                          "&client_secret=" + CLIENT_SECRET +
                          "&scope=https://graph.microsoft.com/.default" +
                          "&grant_type=client_credentials";
            post.setEntity(new StringEntity(body));

            // Execute the request
            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Convert JSON response to object
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                // Return the access token
                return jsonNode.get("access_token").asText();
            }
        }
    }
}
