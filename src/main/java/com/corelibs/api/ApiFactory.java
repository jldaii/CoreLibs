package com.corelibs.api;

import android.text.TextUtils;

import com.corelibs.common.Configuration;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 用于retrofit创建网络请求接口的Api实现，通过设置{@link Configuration#enableLoggingNetworkParams()}来启用网络请求
 * 参数与相应结果.
 * <br/>
 * 通过{@link #add(String)}或{@link #add(String, String)}来添加不同的BaseUrl，并通过create方法创建Api实现。
 * <br/><br/>
 * <pre>
 * ApiFactory.getFactory().add(baseUrl);
 * ApiFactory.getFactory().create(ProductApi.class);
 *
 * ApiFactory.getFactory().add("dev", baseUrl);
 * ApiFactory.getFactory().create("dev", ApProductApi.class);
 * ApiFactory.getFactory().create(1, ApProductApi.class);
 * </pre>
 * Created by Ryan on 2015/12/30.
 */
public class ApiFactory {

    private static ApiFactory factory;
    private HashMap<String, Retrofit> retrofitMap = new HashMap<>();

    public static ApiFactory getFactory() {
        if (factory == null) {
            synchronized (ApiFactory.class) {
                if (factory == null)
                    factory = new ApiFactory();
            }
        }

        return factory;
    }

    /**
     * 新增一个retrofit实例，可以通过{@link #create(Class)}或{@link #create(int, Class)}方法获取Api实现。<br/>
     * key默认自增长。
     */
    public void add(String baseUrl) {
        retrofitMap.put(retrofitMap.size() + "", createRetrofit(baseUrl));
    }

    /**
     * 新增一个retrofit实例，可以通过{@link #create(String, Class)}方法获取Api实现。<br/>
     */
    public void add(String key, String baseUrl) {
        retrofitMap.put(key, createRetrofit(baseUrl));
    }

    /**
     * 获取Api实现，默认通第一个retrofit实例创建。
     */
    public <T> T create(Class<T> clz) {
        checkRetrofitMap();
        return retrofitMap.get("0").create(clz);
    }

    /**
     * 根据key值获取Api实现
     */
    public <T> T create(int key, Class<T> clz) {
        checkRetrofitMap();
        return retrofitMap.get(key + "").create(clz);
    }

    /**
     * 根据key值获取Api实现
     */
    public <T> T create(String key, Class<T> clz) {
        checkRetrofitMap();
        return retrofitMap.get(key).create(clz);
    }

    private void checkRetrofitMap() {
        if (retrofitMap.size() <= 0)
            throw new IllegalStateException("Please add a Retrofit instance");
    }

    private Retrofit createRetrofit(String baseUrl) {

        if (baseUrl == null || baseUrl.length() <= 0)
            throw new IllegalStateException("BaseUrl cannot be null");

        Retrofit.Builder builder = new Retrofit.Builder();
        GsonBuilder gsonBuilder = new GsonBuilder();

        // Gson double类型转换, 避免空字符串解析出错
        final TypeAdapter<Number> DOUBLE = new TypeAdapter<Number>() {
            @Override
            public Number read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                if (in.peek() == JsonToken.STRING) {
                    String tmp = in.nextString();
                    if (TextUtils.isEmpty(tmp)) tmp = "0";
                    return Double.parseDouble(tmp);
                }
                return in.nextDouble();
            }

            @Override
            public void write(JsonWriter out, Number value) throws IOException {
                out.value(value);
            }
        };

        // Gson long类型转换, 避免空字符串解析出错
        final TypeAdapter<Number> LONG = new TypeAdapter<Number>() {
            @Override
            public Number read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                if (in.peek() == JsonToken.STRING) {
                    String tmp = in.nextString();
                    if (TextUtils.isEmpty(tmp)) tmp = "0";
                    return Long.parseLong(tmp);
                }
                return in.nextLong();
            }

            @Override
            public void write(JsonWriter out, Number value) throws IOException {
                out.value(value);
            }
        };

        // Gson int类型转换, 避免空字符串解析出错
        final TypeAdapter<Number> INT = new TypeAdapter<Number>() {
            @Override
            public Number read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                if (in.peek() == JsonToken.STRING) {
                    String tmp = in.nextString();
                    if (TextUtils.isEmpty(tmp)) tmp = "0";
                    return Integer.parseInt(tmp);
                }
                return in.nextInt();
            }

            @Override
            public void write(JsonWriter out, Number value) throws IOException {
                out.value(value);
            }
        };

        gsonBuilder.registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, DOUBLE));
        gsonBuilder.registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, LONG));
        gsonBuilder.registerTypeAdapterFactory(TypeAdapters.newFactory(int.class, Integer.class, INT));

        builder.baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()));

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (Configuration.isShowNetworkParams()) {
            clientBuilder.addInterceptor(new HttpLoggingInterceptor());
        }

        builder.client(clientBuilder.build());

        return builder.build();
    }
}
