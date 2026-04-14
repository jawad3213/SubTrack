import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestGemini {
    public static void main(String[] args) {
        String apiKey = "AIzaSyCk9jqIRPhvnIm3a8m_AQtSD9P-wd1dc34";
        String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
        
        try {
            System.out.println("Testing Gemini API Key...");
            URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"Respond with exactly: OK\"}]}]}";
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("HTTP Status Code: " + responseCode);
            
            if (responseCode == 200) {
                System.out.println("Success! The key is working.");
            } else {
                System.out.println("Error from Google:");
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
