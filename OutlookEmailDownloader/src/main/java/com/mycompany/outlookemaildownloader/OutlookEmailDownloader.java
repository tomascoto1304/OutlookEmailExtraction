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

    private static final String USER_EMAIL = "jyu@bdconsultores.com";
    private static final String DOWNLOAD_DIR = "attachments/"; // Carpeta de descargas

    public static void fetchEmails() throws Exception {
        System.out.println("Iniciando la descarga de correos...");

        // Obtener el token de acceso
        String accessToken = Authenticator.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            System.out.println("ERROR: No se pudo obtener el token de acceso.");
            return;
        }
        System.out.println("Token obtenido correctamente.");

        // URL de la API de Microsoft Graph con $select para obtener información relevante
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

                // Parsear la respuesta JSON
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                if (jsonNode.has("value")) {
                    System.out.println("Correos recibidos:");
                    for (JsonNode email : jsonNode.get("value")) {
                        System.out.println("--------------------------------------------------");
                        System.out.println("Asunto: " + email.get("subject").asText());
                        System.out.println("Remitente: " + email.get("from").get("emailAddress").get("address").asText());
                        System.out.println("Fecha: " + email.get("receivedDateTime").asText());

                        // Verificar si el correo tiene adjuntos
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
        String url = "https://graph.microsoft.com/v1.0/users/" + USER_EMAIL + "/messages/" + messageId + "/attachments";
        System.out.println("URL de adjuntos: " + url);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    System.out.println("ERROR al obtener adjuntos: Estado " + statusCode);
                    System.out.println(responseBody);
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);
                if (jsonNode.has("value")) {
                    Files.createDirectories(Paths.get(DOWNLOAD_DIR)); // Crear carpeta si no existe

                    for (JsonNode attachment : jsonNode.get("value")) {
                        if (attachment.has("contentBytes")) {
                            String fileName = attachment.get("name").asText();
                            String fileContent = attachment.get("contentBytes").asText();
                            byte[] fileData = java.util.Base64.getDecoder().decode(fileContent);

                            File file = new File(DOWNLOAD_DIR + fileName);
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                fos.write(fileData);
                                System.out.println("Archivo descargado: " + fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al descargar adjuntos:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Ejecutando aplicacion...");

        try {
            OutlookEmailDownloader.fetchEmails();
        } catch (Exception e) {
            System.out.println("Error al ejecutar fetchEmails:");
            e.printStackTrace();
        }
    }
}
