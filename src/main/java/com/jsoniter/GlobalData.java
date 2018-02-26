package com.jsoniter;

import java.util.HashMap;

public class GlobalData {
    public static HashMap<String,Boolean> dictionary;
    static {
        dictionary = new HashMap<String, Boolean>();
    }

    public static int readNumberBranches = 0;
}