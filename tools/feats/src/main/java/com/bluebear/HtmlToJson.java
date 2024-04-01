package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HtmlToJson {

    private static String removeBr (String text) {
        if (text == null) return null;
        text = text.trim();
        while (text.startsWith("<br>")) {
            text = text.replaceAll("^<br>", "");
        }
        while (text.endsWith("<br>")) {
            text = text.replaceAll("<br>$", "");
        }
        return text;
    }

    public static void htmlToJson () throws IOException {
        File source = new File("feats/html");
        File target = new File("feats/json");
        Tools.clearPath(target);
        target.mkdirs();

        for (File featHtml : source.listFiles()) {
            String content = Tools.readFile(featHtml);

            String key = featHtml.getName().replaceAll(".html$", "");
            System.out.println("Serializing " + key);

            JSONObject feat = new JSONObject();
            feat.put("key", key);

            String name = Tools.reg(content, "<title>\\s*(\\S[^<>]*?\\S)\\s*- Feats - Archives of Nethys: Pathfinder RPG Database</title>");
            feat.put("name", Objects.requireNonNull(name));
            feat.put("url", "https://www.aonprd.com/FeatDisplay.aspx?ItemName=" + name.replaceAll(" ", "%20"));

            // remove all hyperlinks
            String spellPage = content;
            spellPage = spellPage.replaceAll("<a\\s*(?:[^>]*?)?href=[\"'](?:[^\"']*)[\"'][^>]*>(.*?)</a>", "$1");
            // capture br
            spellPage = spellPage.replaceAll("<\\s*br\\s*(|/)\\s*>", "<br>");

            // descriptors
            String descriptors = Tools.reg(spellPage, "<h1 class=\"title\">(?:|<img [^<>]*>) *(?:|<img [^<>]*>) *[^\\(\\)]* *\\(([^\\(\\)]*?)\\)</h1><b>Source</b>");
            if (descriptors != null) {
                if (!List.of(new String[]{
                        "BoA", "Cheliax", "DTT", "Dhampir", "Fetchling", "MC", "Taldor Variant", "UM", "UW", "Zon-Kuthon's Kiss"
                }).contains(descriptors)) {
                    feat.put("descriptors", descriptors.split(" *, *"));
                }
            }

            // separate combat stamina and mythic
            String staminaRegex = "<h2 class=\"title\"><u>Combat Trick</u> \\(from the <u>Combat Stamina</u> feat\\)</h2>(.*?)(?=<h2|</span>)";
            String staminaPart = Tools.reg(spellPage, staminaRegex);
            spellPage = spellPage.replaceAll(staminaRegex, "");
            String mythicRegex = "<h2 class=\"title\">Mythic [^<>]*</h2>(.*?)(?=<h2|</span>)";
            String mythicPart = Tools.reg(spellPage, mythicRegex);
            spellPage = spellPage.replaceAll(mythicRegex, "");

            // normal part
            {
                // sources
                String sourcesString = Tools.reg(spellPage, "<b>Source</b>\\s*(.*?)\\s*(?:<br>|<h)");
                feat.put("source", Tools.regAll(sourcesString, "<i>\\s*([^<>]*?)\\s*</i>"));

                // text
                int index = spellPage.indexOf("<b>Source</b>");
                int l = spellPage.indexOf("<br>", index) + "<br>".length();
                int r = spellPage.indexOf("<br>", l);
                if (r == -1) r = spellPage.length();
                feat.put("text", spellPage.substring(l, r));

                // benefit
                feat.put("prerequisites", removeBr(Tools.reg(spellPage, "<b>Prerequisites</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("benefit", removeBr(Tools.reg(spellPage, "<b>Benefit</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("normal", removeBr(Tools.reg(spellPage, "<b>Normal</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("special", removeBr(Tools.reg(spellPage, "<b>Special</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("goal", removeBr(Tools.reg(spellPage, "<b>Goal</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("completionBenefit", removeBr(Tools.reg(spellPage, "<b>Completion Benefit</b>: (.*?)(<b>|<h2|</span>|$)")));
            }

            // stamina part
            if (staminaPart != null) {
                // sources
                String sourcesString = Tools.reg(staminaPart, "<b>Source</b>\\s*(.*?)\\s*(?:<br>|<h)");
                feat.put("staminaSource", Tools.reg(sourcesString, "<i>\\s*([^<>]*?)\\s*</i>"));

                // text
                int index = staminaPart.indexOf("<b>Source</b>");
                int l = staminaPart.indexOf("<br>", index) + "<br>".length();
                int r = staminaPart.indexOf("<br>", l);
                if (r == -1) r = staminaPart.length();
                feat.put("staminaText", staminaPart.substring(l, r));
            }

            // mythic part
            if (mythicPart != null) {
                // sources
                String sourcesString = Tools.reg(mythicPart, "<b>Source</b>\\s*(.*?)\\s*(?:<br>|<h)");
                feat.put("mythicSource", Tools.reg(sourcesString, "<i>\\s*([^<>]*?)\\s*</i>"));

                // text
                int index = mythicPart.indexOf("<b>Source</b>");
                int l = mythicPart.indexOf("<br>", index) + "<br>".length();
                int r = mythicPart.indexOf("<br>", l);
                if (r == -1) r = mythicPart.length();
                feat.put("mythicText", mythicPart.substring(l, r));

                // benefit
                feat.put("mythicPrerequisites", removeBr(Tools.reg(mythicPart, "<b>Prerequisites</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("mythicBenefit", removeBr(Tools.reg(mythicPart, "<b>Benefit</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("mythicNormal", removeBr(Tools.reg(mythicPart, "<b>Normal</b>: (.*?)(<b>|<h2|</span>|$)")));
                feat.put("mythicSpecial", removeBr(Tools.reg(mythicPart, "<b>Special</b>: (.*?)(<b>|<h2|</span>|$)")));
            }

            Tools.writeFile(new File(target, key + ".json"), feat.toString(4));
        }
    }

    public static void buildTree () throws IOException {
        File source = new File("feats/json");
        List<String> featNames = new ArrayList<>();
        for (File featJson : source.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featJson));
            featNames.add(feat.getString("key"));
        }
        for (File featJson : source.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featJson));
            if (feat.containsKey("prerequisites")) {
                System.out.println("Building tree for " + feat.getString("key"));
                String prerequisites = feat.getString("prerequisites").toLowerCase();
                prerequisites = prerequisites.replaceAll("cannot have .*?(?=,|;|\\.)", "");
                JSONArray prerequisitesArray = new JSONArray();
                for (String featName : featNames) {
                    if (prerequisites.matches(".*(^|,|;| or ) *" + featName + " *(|\\([^()]*\\)) *(,|;| or |\\.| with ).*")) {
                        prerequisitesArray.add(featName);
                    }
                }
                if (!prerequisitesArray.isEmpty()) {
                    feat.put("prerequisiteKeys", prerequisitesArray);
                }
                Tools.writeFile(featJson, feat.toString(4));
            }
        }
    }
    public static void main(String[] args) throws Exception {
        htmlToJson();
        buildTree();
    }
}
