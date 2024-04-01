package com.bluebear;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Single {
    public static void main(String[] args) throws IOException {
        String key = "Water Shield";
        String urlString = "https://www.aonprd.com/SpellDisplay.aspx?ItemName=Water%20Shield";

        System.out.println("Fetching " + key);

        URL url = new URL(urlString);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(proxy);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept", "*/*");
        connection.connect();
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();

        key = key.toLowerCase().replaceAll("/", "-");
        Tools.writeFile(new File(new File("spells/html"), key + ".html"), builder.toString());
    }
}
