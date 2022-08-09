package edu.gla.kail.ad.service;

public class Utils {
    public static boolean isBlank(String s){
        if(s != null){
            s = s.replace(" ", "");
            return s.length() == 0;
        }
        return true;
    }
}
