package com.mycompany.outlookemaildownloader;

import com.mycompany.Authenticator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OutlookEmailDownloader {

    private static final String USER_EMAIL = "USER_EMAIL"; // Account email
    private static final String DOWNLOAD_DIR = "attachments/"; // Download directory


    public static void fetchEmails() throws Exception {
        System.out.println("Iniciando la descarga de correos...");

       // Get the access token
        String accessToken = Authenticator.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            System.out.println("ERROR: No se pudo obtener el token de acceso.");
            return;
        }
        System.out.println("Token obtenido correctamente.");

        // URL to fetch emails from Microsoft Graph API
        String url = "https://graph.microsoft.com/v1.0/users/" + USER_EMAIL + "/messages?$select=subject,from,receivedDateTime,hasAttachments";
        System.out.println("URL de solicitud: " + url);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Accept", "application/json");

            System.out.println("Enviando solicitud a Microsoft Graph...");

            try (CloseableHttpResponse response = client.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    System.out.println("ERROR: La API respondió con estado " + statusCode);
                    System.out.println("Respuesta completa:");
                    System.out.println(responseBody);
                    return;
                }

                // Convert JSON response to object
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                if (jsonNode.has("value")) {
                    System.out.println("Correos recibidos:");
                    for (JsonNode email : jsonNode.get("value")) {
                        System.out.println("--------------------------------------------------");
                        System.out.println("Asunto: " + email.get("subject").asText());
                        System.out.println("Remitente: " + email.get("from").get("emailAddress").get("address").asText());
                        System.out.println("Fecha: " + email.get("receivedDateTime").asText());

                       // If the email has attachments, download them
                        if (email.has("hasAttachments") && email.get("hasAttachments").asBoolean()) {
                            String messageId = email.get("id").asText();
                            System.out.println("Este correo tiene archivos adjuntos. Descargando...");
                            downloadAttachments(accessToken, messageId);
                        }
                        System.out.println("--------------------------------------------------");
                    }
                } else {
                    System.out.println("No se encontraron correos.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error al obtener correos:");
            e.printStackTrace();
        }
    }

    private static void downloadAttachments(String accessToken, String messageId) {
    // Construct the URL to fetch email attachments
    String url = "https://graph.microsoft.com/v1.0/users/" + USER_EMAIL + "/messages/" + messageId + "/attachments";
    System.out.println("Attachments URL: " + url);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + accessToken);
        get.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = client.execute(get)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            // Check if the request was successful
            if (statusCode != 200) {
                System.out.println("ERROR fetching attachments: Status " + statusCode);
                System.out.println(responseBody);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            if (jsonNode.has("value")) {
                Files.createDirectories(Paths.get(DOWNLOAD_DIR)); // Create directory if it does not exist

                for (JsonNode attachment : jsonNode.get("value")) {
                    // Check if the attachment contains contentBytes (file data)
                    if (attachment.has("contentBytes")) {
                        String fileName = attachment.get("name").asText();
                        String fileContent = attachment.get("contentBytes").asText();
                        byte[] fileData = java.util.Base64.getDecoder().decode(fileContent);

                        // Save the file in the designated download directory
                        File file = new File(DOWNLOAD_DIR + fileName);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(fileData);
                            System.out.println("File downloaded: " + fileName);
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.out.println("Error downloading attachments:");
        e.printStackTrace();
    }
}

public static void main(String[] args) {
    System.out.println("Running application...");

    try {
        // Fetch emails from Outlook and download attachments if available
        OutlookEmailDownloader.fetchEmails();
    } catch (Exception e) {
        System.out.println("Error executing fetchEmails:");
        e.printStackTrace();
    }
}
