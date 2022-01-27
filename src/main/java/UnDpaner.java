import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.net.ssl.*;


public class UnDpaner {
// Connect to BD

    public static Connection getConnection() throws SQLException{
        Properties props = new Properties();

        String url = props.getProperty("jdbc:oracle:thin:@server:port:schema");
        String username = props.getProperty("username");
        String password = props.getProperty("password");

        return DriverManager.getConnection(url, username, password);
    }

    static class HHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOExcepton {

            ArrayList<String> array = new ArrayList<>();
            String line;
            File cardBase = new File("./file.csv");
            FileReader fr = new FileReader(cardBase);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null){
                array.add(line);
            }


            fr.close();
            br.close();

            Random rand = new Random();

            int rand1 = rand.nextInt(array.size());
            int rand2 = rand.nextInt(array.size());

            String request = "";
            InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
            BufferedReader ibr = new BufferedReader(isr);
            int i;
            StringBuilder strBuild = new StringBuilder(512);
            while ((i = ibr.read()) != -1) {
                strBuild.append((char) i);
            }

            ibr.close();
            isr.close();

            String uid = "";
            String panID1 = "";
            String panID2 = "";
            request = strBuild.toString();

//            System.out.println(request);

            Pattern pattern = Pattern.compile("regex_uid");
            Matcher matcher = pattern.matcher(request);
            while (matcher.find()) {
                uid = request.substring(matcher.start(), matcher.end());
                }
            pattern = Pattern.compile("regex_panID1");
            matcher = pattern.matcher(request);
            while (matcher.find()) {
                panID1 = request.substring(matcher.start(), matcher.end());
            }
            pattern = Pattern.compile("regex_panID2");
            matcher = pattern.matcher(request);
            while (matcher.find()) {
                panID2 = request.substring(matcher.start(), matcher.end());
            }

            String card1 = array[rand1];
            String card2 = array[rand2];

            String response = uid + "some text" + card1 + "text" + panID1 + "some text" + card2 + "text" + panID2;

            t.getResponseHeaders().set("text for headers");
            t.sendResponseHeaders(200,response.length());

            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            t.close();
        }
    }

    public static void main(String[] args) {
        try{


            String sqlCommand = "SELECT * FROM TABLE_NAME";
            String PAN = "";

            PrintWriter writer = new PrintWriter("bdFile.csv");
            Class.forNmae("oracle.jdbc.driver.OracleDriver");

            try (Connection conn = DriverManager.getConnection()) {
                System.out.println("DB connection successful");
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuerry(sqlCommand);
                while (rs.next()) {
                    PAN = rs.getString("COLUMN_NAME");
                    writer.println(PAN);
                }
                writer.close();

            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }

            int port = Integer.parseInt(args[0]);
            HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 5);
            server.createContext("/", HHandler());

            try {

                char[] password = "password".toCharArray();
                Keystore ks = KeyStore.getInstance("JKS");
                FileInputStream fis = new FileInputStream("./keystore.jks");
                ks.load(fis, password);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, password);

                KeyStore ks2 = KeyStore.getInstance("JKS");
                FileInputStream fis2 = new FileInputStream("./trusted.jks");
                ks2.load(fis2, password);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks2);

                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    @Override
                    public void configure(HttpsParameters params) {
                        try{
                            SSLContext c = getSSLContext();
                            SSLEngine engine = c.createSSLEngine();

                            params.setNeedClientAuth(false);
                            params.setSSLParameters(c.getDefaultSSLParameters());
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());

                            SSLParameters sslParameters = c.getSupportedSSLParameters();
                            params.setSSLParameters(sslParameters);

                        } catch (Exception ex) {
                            System.err.println("Failed to create HTTP port");
                            System.err.println(ex.getMessage());
                        }

                    }
                });

            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
            ExecutorService exe = Executors.newFixedThreadPool(1);
            server.setExecutor(exe);
            server.start();
            System.out.println("Server started, awaiting requests");

            exe.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
