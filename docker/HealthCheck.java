import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sonda de healthcheck para a imagem distroless (sem shell, wget ou curl).
 * Compilada no estágio de build do Dockerfile (onde há JDK 21) e executada
 * pelo JRE da imagem final, via healthcheck do docker-compose.yml.
 * Sai com 0 se o Actuator responder 200 com status "UP"; caso contrário, 1.
 */
public final class HealthCheck {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://127.0.0.1:8080/actuator/health";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || !response.body().contains("\"UP\"")) {
            System.err.println("unhealthy: HTTP " + response.statusCode() + " " + response.body());
            System.exit(1);
        }
        System.out.println("healthy: " + response.body());
    }
}
