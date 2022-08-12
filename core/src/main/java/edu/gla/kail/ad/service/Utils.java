package edu.gla.kail.ad.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.gla.kail.ad.agents.ResponseIdGenerator;

public class Utils {

    /**
     * Method used to check whether a string is blank or not
     * @param s An input String
     * @return Returns true or false if the string is blank or not
     */
    public static boolean isBlank(String s){
        if(s != null){
            s = s.replace(" ", "");
            return s.length() == 0;
        }
        return true;
    }

    /**
     * Generic method used to convert any json object to hashmap. Notice that in this method
     * nested json objects will be converted to nested hashmaps mapped to a randomly
     * generated hash key. This ensures compatibility with Firebase 
     * @param jsonObj
     * @return HashMap representation of the jsonObject
     */
    public static HashMap<String, Object> jsonObjectToHashMap(JSONObject jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        Iterator<?> keys = jsonObj.keys();

        while(keys.hasNext()){
            String key = (String)keys.next();

            // Check if the object is an array. if it is, we need to generate
            // a unique id for every element in the list
            Object value = jsonObj.get(key); 
            if(value instanceof JSONArray){

                JSONArray obj = (JSONArray)value;

                // We keep track of the list of hashmaps for this inner object
                ArrayList<HashMap<String, HashMap<String, Object>>> tempArray = new ArrayList<HashMap<String, HashMap<String, Object>>>();
                for(int i = 0; i < obj.length(); i++){
                    String unique_id = ResponseIdGenerator.generate();
                    HashMap<String, HashMap<String, Object>> tempMap = new HashMap<String, HashMap<String, Object>>();

                    // This step is key: We recusrively pass down any inner object. This allows us to construct
                    // a tree structure of nested json objecs and convert them to a HashMap
                    HashMap<String, Object> resultMap = jsonObjectToHashMap(obj.getJSONObject(i));
                    tempMap.put(unique_id, resultMap);
                    tempArray.add(tempMap);

                }
                map.put(key, tempArray);

            }else{
                // If it's not an array we simply place the value
                map.put(key, value);
            }
            
        }

        return map;

    }

    /**
     * Generic method to convert a json string to hashmap. Notice that in this method
     * nested json objects will be converted to nested hashmaps mapped to a randomly
     * generated hash key. This ensures compatibility with Firebase
     * @param jsonString 
     * @return A Hashmap representation of the jsonString
     */
    public static HashMap<String, Object> jsonStringToHashMap(String jsonString){
        
        JSONObject jObject = new JSONObject(jsonString);
        HashMap<String, Object> map = jsonObjectToHashMap(jObject);
        return map;
    }

}


