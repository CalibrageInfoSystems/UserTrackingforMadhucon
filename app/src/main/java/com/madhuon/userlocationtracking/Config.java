package com.madhuon.userlocationtracking;

public class Config {

    public static final boolean DEVELOPER_MODE = false;
    public static String live_url = "http://182.18.157.215/3FSmartPalm_Nursery/API/api";

    public static void initialize() {
        if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("release")) {
            live_url = "http://182.18.157.215/3FSmartPalm_Nursery/API/api";

        } else {

            live_url = "http://182.18.157.215/3FSmartPalm_Nursery/API/api";
        }
    }
}
