package cyt.wuwadownloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class Json工具 {

    private static final Gson gson = new Gson();

    public static String 到Json(Object object) {
        return gson.toJson(object);
    }

    public static <T> T 到对象(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T 到对象(String json, Type type) {
        return gson.fromJson(json, type);
    }
}