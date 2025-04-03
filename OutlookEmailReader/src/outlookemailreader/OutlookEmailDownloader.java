package outlookemailreader;

import com.mycompany.auth.Authenticator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class OutlookEmailDownloader {

    private static final String USER_EMAIL = "USER_EMAIL"; // Account email
    private static final String DOWNLOAD_DIR = "attachments/"; // Download directory

    public static void fetchEmails() throws Exception {
        System.out.println("Starting email download...");

        // Get the access token
        String accessToken = Authenticator.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            System.out.println("ERROR: Unable to obtain access token.");
            return;
        }
        System.out.println("Token obtained successfully.");

        // URL to fetch emails from Microsoft Graph API
        String url = "https://graph.microsoft.com/v1.0/users/" + USER_EMAIL + "/messages?$select=subject,from,receivedDateTime,hasAttachments";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(get)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Convert JSON response to object
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                if (jsonNode.has("value")) {
                    for (JsonNode email : jsonNode.get("value")) {
                        System.out.println("--------------------------------------------------");
                        System.out.println("Subject: " + email.get("subject").asText());
                        System.out.println("Sender: " + email.get("from").get("emailAddress").get("address").asText());
                        System.out.println("Date: " + email.get("receivedDateTime").asText());

                        // If the email has attachments, download them
                        if (email.has("hasAttachments") && email.get("hasAttachments").asBoolean()) {
                            downloadAttachments(accessToken, email.get("id").asText());
                        }
                    }
                }
            }
        }
    }

    public static void downloadAttachments(String accessToken, String messageId) throws Exception {
        String url = "https://graph.microsoft.com/v1.0/users/" + USER_EMAIL + "/messages/" + messageId + "/attachments";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(get)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                Files.createDirectories(Paths.get(DOWNLOAD_DIR));
                System.out.println("Download folder created at: " + new File(DOWNLOAD_DIR).getAbsolutePath());

                for (JsonNode attachment : jsonNode.get("value")) {
                    String fileName = attachment.get("name").asText();
                    byte[] fileData = Base64.getDecoder().decode(attachment.get("contentBytes").asText());

                    try (FileOutputStream fos = new FileOutputStream(new File(DOWNLOAD_DIR + fileName))) {
                        fos.write(fileData);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        fetchEmails();
    }
}
