package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListToTask {
    public static void listToTask () throws IOException {
        File source = new File("spells.html");
        File target = new File("tasks.json");

        String content = Tools.readFile(source);
        JSONArray spells = new JSONArray();
        String regex = "<a href=\"(https://www.aonprd.com/SpellDisplay.aspx\\?ItemName=[^\"]*)\">(?:|<img src=\"[^\"]*\" title=\"[^\"]*\" style=\"[^\"]*\">)\\s(?:|<img src=\"[^\"]*\" title=\"[^\"]*\" style=\"[^\"]*\">)\\s*(\\S[^<>]*?\\S)\\s*</a>";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        while (matcher.find()) {
            JSONObject spell = new JSONObject();
            spell.put("url", matcher.group(1));
            spell.put("key", matcher.group(2));
            spells.add(spell);
        }
        Tools.writeFile(target, spells.toString(4));
    }
    public static void main(String[] args) throws Exception {
        listToTask();
    }
}
