package com.bluebear;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {
    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        return builder.toString();
    }

    public static void writeFile(File file, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        writer.write(content);
        writer.flush();
        writer.close();
    }
    public static void clearPath (File path) {
        if (!path.exists()) {
            return;
        }
        if (path.isFile()) {
            path.delete();
        } else {
            for (File sub : path.listFiles()) {
                sub.delete();
            }
            path.delete();
        }
    }
    public static String reg(String content, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    public static List<String> regAll(String content, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        ArrayList<String> strings = new ArrayList<>();
        while (matcher.find()) {
            strings.add(matcher.group(1));
        }
        return strings;
    }
}
