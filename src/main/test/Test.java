import com.fgsqw.httpserver.HttpServer;

public class Test {
    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer(8888);
        server.addPath("/css/bootstrap.min.css", (request, response) -> response.writeString("Hello World"));
        server.addPath("/hello", (request, response) -> response.writeString("Hello World"));
        server.start();
    }
}
