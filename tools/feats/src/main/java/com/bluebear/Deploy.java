package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Deploy {


    public static void generateIndex () throws IOException {
        File source = new File("feats/translated");
        File target = new File("../../pathfinder/featsIndex.js");

        JSONObject feats = new JSONObject();
        List<JSONObject> featsArray = new ArrayList<>();
        for (File featFile : source.listFiles()) {
            JSONObject feat = JSONObject.fromObject(Tools.readFile(featFile));
            JSONObject index = new JSONObject();

            String[] keys = new String[] {"key", "name", "name_zh", "descriptors", "source", "prerequisiteKeys"};
            for (String key : keys) {
                index.put(key, feat.get(key));
            }

            feats.put(index.getString("key"), index);
            featsArray.add(index);
        }

        // build tree
        Node base = new Node();
        Map<String, Node> nodes = new HashMap<>();
        int left = 0;
        while (!featsArray.isEmpty()) {
            System.out.println("Scanning, left = " + featsArray.size());
            if (left == featsArray.size()) {
                System.out.println(left + " = " + featsArray.size());
                System.out.println(JSONArray.fromObject(featsArray));
                System.exit(0);
            }
            left = featsArray.size();
            Set<JSONObject> toRemove = new HashSet<>();
            for (JSONObject feat : featsArray) {
                // check if all prerequisite are met
                boolean valid = true;
                if (feat.containsKey("prerequisiteKeys")) {
                    for (Object o : feat.getJSONArray("prerequisiteKeys")) {
                        valid &= nodes.containsKey(o.toString());
                    }
                }
                if (valid) {
                    Node node = new Node();
                    node.key = feat.getString("key");
                    nodes.put(node.key, node);
                    toRemove.add(feat);
                    if (feat.containsKey("prerequisiteKeys")) {
                        for (Object o : feat.getJSONArray("prerequisiteKeys")) {
                            String key = o.toString();
                            // top
                            boolean onTop = true;
                            for (Object o2 : feat.getJSONArray("prerequisiteKeys")) {
                                JSONObject possibleChild = feats.getJSONObject(o2.toString());
                                onTop &= !(possibleChild.containsKey("prerequisiteKeys") && possibleChild.getJSONArray("prerequisiteKeys").contains(key));
                            }
                            if (onTop) {
                                nodes.get(key).children.add(node);
                            }
                        }
                    } else {
                        base.children.add(node);
                    }
                }
            }
            featsArray.removeAll(toRemove);
        }

        Tools.writeFile(target, "var featDict = " + feats.toString(4) + ";\nvar featTree = " + base.toJsonArray().toString(4) + ";");
    }

    public static void copyFiles () throws IOException {
        File source = new File("feats/translated");
        File target = new File("../../pathfinder/feats");
        Tools.clearPath(target);
        target.mkdirs();

        for (File file : source.listFiles()) {
            Tools.writeFile(new File(target, file.getName()), Tools.readFile(file));
        }
    }

    public static void main(String[] args) throws IOException {
        generateIndex();
        copyFiles();
    }
}

class Node {
    String key;
    List<Node> children = new ArrayList<>();
    JSONArray toJsonArray () {
        JSONArray list = new JSONArray();
        for (Node node : children) {
            JSONObject child = new JSONObject();
            child.put("key", node.key);
            child.put("children", node.toJsonArray());
            list.add(child);
        }
        return list;
    }
}