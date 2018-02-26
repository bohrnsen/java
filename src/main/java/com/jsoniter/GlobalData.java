package com.jsoniter;

import java.util.HashMap;

public class GlobalData {
    public static HashMap<String, MethodData> dictionary;
    static {
        dictionary = new HashMap<String, MethodData>();
    }
}