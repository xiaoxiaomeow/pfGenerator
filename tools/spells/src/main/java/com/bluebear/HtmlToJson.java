package com.bluebear;

import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class HtmlToJson {

    public static void htmlToJson () throws IOException {
        File source = new File("spells/html");
        File target = new File("spells/json");
        Tools.clearPath(target);
        target.mkdirs();

        for (File spellHtml : source.listFiles()) {
            String content = Tools.readFile(spellHtml);

            String key = spellHtml.getName().replaceAll(".html$", "");
            System.out.println("Serializing " + key);

            JSONObject spell = new JSONObject();
            spell.put("key", key);

            String name = Tools.reg(content, "<title>\\s*(\\S[^<>]*?\\S)\\s*- Spells - Archives of Nethys: Pathfinder RPG Database</title>");
            spell.put("name", Objects.requireNonNull(name));
            spell.put("url", "https://www.aonprd.com/SpellDisplay.aspx?ItemName=" + name.replaceAll(" ", "%20"));

            // remove other spells in the same page
            String spellPage = null;
            String[] pages = content.split("(?=<h1 class=\"title\">)");
            for (String page : pages) {
                String pageTitle = Tools.reg(page, "<h1 class=\"title\">(?:|<img src=\"[^\"]*\" title *=\"[^\"]*\" style=\"[^\"]*\">)\\s*(?:|<img src=\"[^\"]*\" title *=\"[^\"]*\" style=\"[^\"]*\">)\\s*(\\S[^<>]*\\S)\\s*</h1>");
                if (pageTitle == null) pageTitle = Tools.reg(page, "<h1 class=\"title\"><img src=\"[^\"]*\" title *=\"[^\"]*\" style=\"[^\"]*\">\\s*(\\S[^<>]*\\S)\\s*</h1>");
                if (Objects.equals(name, pageTitle)) {
                    spellPage = page;
                    break;
                }
            }
            if (spellPage == null) {
                System.err.println("Cannot find correct page in spell " + key);
                System.exit(0);
            }

            // remove all hyperlinks
            spellPage = spellPage.replaceAll("<a\\s*(?:[^>]*?)?href=[\"'](?:[^\"']*)[\"'][^>]*>(.*?)</a>", "$1");
            // capture br
            spellPage = spellPage.replaceAll("<\\s*br\\s*(|/)\\s*>", "<br>");

            // sources
            String sourcesString = Tools.reg(spellPage, "<b>Source</b>\\s*(.*?)\\s*(?:<br>|<h)");
            spell.put("source", Tools.regAll(sourcesString, "<i>\\s*([^<>]*?)\\s*</i>"));

            // title line
            String[] titleLine = Tools.reg(spellPage, "<b>School</b>\\s*(.*?)\\s*(?:<br>|<h)").split(";");
            // school
            spell.put("school", Tools.reg(titleLine[0], "^<u>\\s*(.*?)\\s*</u>"));
            // subSchools
            String subSchoolsString = Tools.reg(titleLine[0], "\\((.*?)\\)");
            if (subSchoolsString != null) {
                if (subSchoolsString.startsWith("see text") || subSchoolsString.startsWith("variable")) {
                    spell.put("subSchools", new ArrayList<>() {{add(subSchoolsString);}});
                } else {
                    spell.put("subSchools", Tools.regAll(subSchoolsString, "<u>\\s*(.*?)\\s*</u>"));
                    spell.put("subSchoolsOperator", Tools.reg(subSchoolsString, "\\s*(,|or) "));
                }
            }
            // descriptors
            String descriptorsString = Tools.reg(titleLine[0], "\\[(.*?)\\]");
            if (descriptorsString != null) {
                if (descriptorsString.startsWith("see text") || descriptorsString.startsWith("variable")) {
                    spell.put("descriptors", new ArrayList<>() {{add(descriptorsString);}});
                } else {
                    spell.put("descriptors", Tools.regAll(descriptorsString, "<u>\\s*(.*?)\\s*</u>"));
                    spell.put("descriptorsOperator", Tools.reg(descriptorsString, "\\s*(,|or) "));
                }
            }
            // levels
            if (titleLine.length > 1) {
                String race = Tools.reg(titleLine[1], "<b>Level</b>\\s*(?:.*?)\\s*\\(([^\\(\\)]*?)\\)\\s*$");
                String levelsString;
                if (race == null) {
                    levelsString = Tools.reg(titleLine[1], "<b>Level</b>\\s*(.*?)\\s*$");
                } else {
                    levelsString = Tools.reg(titleLine[1], "<b>Level</b>\\s*(.*?)\\s*\\((?:[^\\(\\)]*?)\\)\\s*$");
                }
                spell.put("race", race);
                String[] levels = levelsString.split("\\s*,\\s*");
                JSONObject levelsMap = new JSONObject();
                for (String level : levels) {
                    try {
                        int index = level.lastIndexOf(" ");
                        String clazz = level.substring(0, index);
                        int spellLevel = Integer.parseInt(level.substring(index + 1, level.length()));
                        levelsMap.put(clazz, spellLevel);
                    } catch (Exception e) {
                        System.out.println(race);
                        System.out.println(levelsString);
                        System.out.println(level);
                        throw e;
                    }
                }
                spell.put("levels", levelsMap);
            } else {
                System.err.println("Cannot find level in spell " + key);
            }
            // other info
            spell.put("castingTime", Tools.reg(spellPage, "<b>Casting Time</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("components", Tools.reg(spellPage, "<b>Components</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            String range = Tools.reg(spellPage, "<b>Range</b>\\s*(.*?)\\s*(?:<br>|<h)");
            if (range != null && !range.isEmpty()) {
                spell.put("range", range);
            }
            spell.put("effect", Tools.reg(spellPage, "<b>Effect</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("target", Tools.reg(spellPage, "<b>Target</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("area", Tools.reg(spellPage, "<b>Area</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("targetOrArea", Tools.reg(spellPage, "<b>Target or Area</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("duration", Tools.reg(spellPage, "<b>Duration</b>\\s*(.*?)\\s*(?:<br>|<h)"));
            spell.put("savingThrow", Tools.reg(spellPage, "<b>Saving Throw</b>\\s*(.*?)\\s*(?:<br>|<h|;)"));
            spell.put("spellResistance", Tools.reg(spellPage, "<b>Spell Resistance</b>\\s*(.*?)\\s*(?:<br>|<h)"));

            spell.put("text", Objects.requireNonNull(Tools.reg(spellPage, "<h3 class=\"framing\">Description</h3>(.*?)(?:</span>|$|<h)")));

            if (spell.get("effect") != null && spell.getString("text").contains("<b>Effect</b> " + spell.getString("effect"))) {
                spell.remove("effect");
            }

            String mythicPart = Tools.reg(spellPage, "<h2 class=\"title\">Mythic.*?</h2>(.*?)(?:</span>|$)");
            if (mythicPart != null) {
                String mythicSourcesString = Tools.reg(mythicPart, "<b>Source</b>\\s*(.*?)\\s*<br>");
                spell.put("mythicSource", Tools.regAll(mythicSourcesString, "<i>\\s*([^<>]*?)\\s*</i>"));
                spell.put("mythicText", Tools.reg(mythicPart, "<b>Source</b>\\s*.*?\\s*<br>(.*?)$"));
            }

            Tools.writeFile(new File(target, key + ".json"), spell.toString(4));
        }
    }
    public static void main(String[] args) throws Exception {
        htmlToJson();
    }
}
