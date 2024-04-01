package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TaskToHtml {
    public static void taskToHtml () throws IOException {
        File source = new File("tasks.json");
        File target = new File("spells/html");
        // Tools.clearPath(target);
        target.mkdirs();

        JSONArray spells = JSONArray.fromObject(Tools.readFile(source));
        for (Object object : spells) {
            JSONObject spell = (JSONObject) object;
            String key = spell.getString("key");
            File targetFile = new File(target, key.toLowerCase().replaceAll("/", "-") + ".html");
            if (targetFile.exists()) continue;

            String urlString = spell.getString("url");
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

            Tools.writeFile(targetFile, builder.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        taskToHtml();
    }
}
