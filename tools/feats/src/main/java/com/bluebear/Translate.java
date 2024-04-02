package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Translate {
    private static final String[] cats = new String[]{
            "source",
            "descriptors",
            "staminaSource",
            "mythicSource"
    };
    private static final Map<String, SortedSet<String>> tags = new HashMap();

    private static void register(String cat, String tag) {
        if (tag == null) {
            return;
        }
        if (!tags.containsKey(cat)) {
            tags.put(cat, new TreeSet<>());
        }
        tags.get(cat).add(tag);
    }

    private static void registerAll(String cat, JSONObject spell) {
        Object object = spell.get(cat);
        if (object == null) {
            return;
        }
        if (object instanceof String) {
            register(cat, object.toString());
        }
        if (object instanceof JSONArray) {
            for (Object child : (JSONArray) object) {
                if (cat.toLowerCase().contains("source")) {
                    register(cat, child.toString().split(" pg\\.")[0]);
                } else {
                    register(cat, child.toString());
                }
            }
        }
    }

    public static void exportTags() throws IOException {
        File source = new File("feats/json");
        File target = new File("tags.json");

        for (File featFile : source.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));

            for (String cat : cats) {
                registerAll(cat, feat);
            }

            JSONObject levels = (JSONObject) feat.get("levels");
            if (levels != null) {
                for (Object key : levels.keySet()) {
                    register("clazz", key.toString());
                }
            }
        }

        JSONObject tagsObject = new JSONObject();
        for (Map.Entry<String, SortedSet<String>> entry : tags.entrySet()) {
            JSONObject catTags = new JSONObject();
            for (String tag : entry.getValue()) {
                catTags.put(tag, tag);
            }
            tagsObject.put(entry.getKey() + "Translations", catTags);
        }

        Tools.writeFile(target, tagsObject.toString(4));
    }

    private static String addBr(String text) {
        if (text == null) return null;
        text = text.trim();
        text = text.replaceAll("\n", "</p><p>");
        return text;
    }

    private static void translateFeat(JSONObject feat, String chmText) {
        chmText = chmText.trim();
        if (chmText.isEmpty()) return;

        feat.put("name_zh", Objects.requireNonNull(Tools.reg(chmText, "^(.*?)( \\(|（)")));
        int l = chmText.indexOf("\n");
        int r = chmText.indexOf("\n", l + 1);
        feat.put("text_zh", chmText.substring(l + 1, r));

        String endRegex = "(?:\n(?:先决条件|专长效果|通常状况|特殊说明|即时收益|专长目标|完成收益)|$)";

        feat.put("prerequisites_zh", addBr(Tools.reg(chmText, "先决条件(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("benefit_zh", addBr(Objects.requireNonNull(Tools.reg(chmText, "(?:专长效果|即时收益)(?:：|:) *((.|\n)*?) *" + endRegex))));
        feat.put("normal_zh", addBr(Tools.reg(chmText, "通常状况(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("special_zh", addBr(Tools.reg(chmText, "特殊说明(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("goal_zh", addBr(Tools.reg(chmText, "专长目标(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("completionBenefit_zh", addBr(Tools.reg(chmText, "完成收益(?:：|:) *((.|\n)*?) *" + endRegex)));
    }

    private static void mythicFeat(JSONObject feat, String chmText) {
        chmText = chmText.trim();
        if (chmText.isEmpty()) return;

        int l = chmText.indexOf("\n");
        int r = chmText.indexOf("\n", l + 1);
        feat.put("mythicText_zh", chmText.substring(l + 1, r));

        String endRegex = "(?:\n(?:先决条件|专长效果|通常状况|特殊说明|即时收益|专长目标|完成收益)|$)";

        feat.put("mythicPrerequisites_zh", addBr(Tools.reg(chmText, "先决条件(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("mythicBenefit_zh", addBr(Objects.requireNonNull(Tools.reg(chmText, "(?:专长效果|即时收益)(?:：|:) *((.|\n)*?) *" + endRegex))));
        feat.put("mythicNormal_zh", addBr(Tools.reg(chmText, "通常状况(?:：|:) *((.|\n)*?) *" + endRegex)));
        feat.put("mythicSpecial_zh", addBr(Tools.reg(chmText, "特殊说明(?:：|:) *((.|\n)*?) *" + endRegex)));
    }

    public static void translateFeats() throws IOException {
        File ref = new File("feats/json");
        File source = new File("feats/chm");
        File target = new File("feats/translated");
        Tools.clearPath(target);
        target.mkdirs();

        File[] files = source.listFiles();
        ArrayList<File> sort = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().contains("-")) {
                sort.add(file);
            }
        }
        for (File file : files) {
            if (file.getName().contains("-")) {
                sort.add(file);
            }
        }
        for (File chmFile : sort) {
            String content = Tools.readFile(chmFile).trim();
            String[] chmTexts = content.split("\n\n(?=[^\n\\(\\)（）]*? *[\\(（].*[\\)）])");

            for (String chmText : chmTexts) {
                chmText = chmText.trim();
                String key = Tools.reg(chmText, "^[^\n\\(\\)（）]* *[\\(（](.*)[\\)）]");
                if (key == null || key.isEmpty()) {
                    System.out.println("Can't find key in feat: ");
                    System.out.println(chmText);
                    System.exit(0);
                }
                System.out.println("Translating feat " + key);
                key = key.toLowerCase().replaceAll("/", "-");
                File spellFile = new File(ref, key + ".json");
                if (!spellFile.exists()) {
                    System.out.println("Can't find feat " + key);
                    System.exit(0);
                }
                JSONObject feat = JSONObject.fromObject(Tools.readFile(spellFile));
                try {
                    translateFeat(feat, chmText);
                } catch (Exception e) {
                    System.out.println(chmText);
                    StringSelection stringSelection = new StringSelection(chmText);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                    e.printStackTrace();
                    System.exit(0);
                }
                File spellTarget = new File(target, key + ".json");
                if (spellTarget.exists()) {
                    System.err.println("Duplicate Spell " + key);
                } else {
                    Tools.writeFile(spellTarget, feat.toString(4));
                }
            }
        }
    }


    public static void mythicFeats() throws IOException {
        File source = new File("feats/mythic.txt");
        File target = new File("feats/translated");

        String content = Tools.readFile(source).trim();
        String[] chmTexts = content.split("\n\n(?=[^\n\\(\\)（）]*? *[\\(（].*[\\)）])");

        for (String chmText : chmTexts) {
            chmText = chmText.trim();
            String key = Tools.reg(chmText, "^[^\n\\(\\)（）]* *[\\(（](.*)[\\)）]");
            if (key == null || key.isEmpty()) {
                System.out.println("Can't find key in feat: ");
                System.out.println(chmText);
                System.exit(0);
            }
            System.out.println("Mythic feat " + key);
            key = key.toLowerCase().replaceAll("/", "-");
            File featFile = new File(target, key + ".json");
            if (!featFile.exists()) {
                System.out.println("Can't find feat " + key);
                System.exit(0);
            }
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));
            try {
                mythicFeat(feat, chmText);
            } catch (Exception e) {
                System.out.println(chmText);
                StringSelection stringSelection = new StringSelection(chmText);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                e.printStackTrace();
                System.exit(0);
            }
            Tools.writeFile(featFile, feat.toString(4));
        }
    }

    public static void staminaFeats() throws IOException {
        File source = new File("feats/stamina.txt");
        File target = new File("feats/translated");

        String content = Tools.readFile(source).trim();
        String[] chmTexts = content.split("\n");

        Map<String, List<File>> nameMap = new HashMap<>();
        for (File featFile : target.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));
            String name = (String) feat.get("name_zh");
            if (!nameMap.containsKey(name)) {
                nameMap.put(name, new ArrayList<>());
            }
            nameMap.get(name).add(featFile);
        }

        for (String chmText : chmTexts) {
            chmText = chmText.trim();
            int index = chmText.indexOf("：");
            String name = chmText.substring(0, index);
            String text = chmText.substring(index + 1);
            // System.out.println("Mythic feat " + name);

            if (!nameMap.containsKey(name)) {
                System.err.println("Cannot find feat " + name);
            } else {
                for (File featFile : nameMap.get(name)) {
                    JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));
                    feat.put("staminaText_zh", text);
                    Tools.writeFile(featFile, feat.toString(4));
                }
            }
        }
    }

    public static void copyRawFeats() throws IOException {
        File ref = new File("feats/json");
        File target = new File("feats/translated");

        for (File featFile : ref.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));
            String key = feat.getString("key");
            File spellTarget = new File(target, key + ".json");
            if (!spellTarget.exists()) {
                translateFeat(feat, "");
                Tools.writeFile(spellTarget, feat.toString(4));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // exportTags();

        translateFeats();
        copyRawFeats();

        mythicFeats();
        staminaFeats();

        Deploy.main(null);
    }
}
