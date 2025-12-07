package com.github.shy526.caimogu;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.shy526.App;
import com.github.shy526.factory.OkHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;



@Slf4j
public class CaiMoGuHelp {
    public static Set<String> readResources(String fileName){
        Set<String> ids=new HashSet<>();
        ClassLoader classLoader = App.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            return ids;
        }
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(classLoader.getResourceAsStream(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ids.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    /**
     * 获取参考 踩蘑菇中所有游戏Id
     * @return
     */
    public static Set<String> ScanGameIds(){
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();

        YearMonth current = YearMonth.now();;

        YearMonth target = YearMonth.of(2021, 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String urlFormat="https://www.caimogu.cc/game/find.html?act=fetch&date=%s&sort=1&sort_desc=1&page=%s";
        Set<String> ids=new HashSet<>();
        while(!target.isAfter(current)) {
            String dateStr = formatter.format(target);
            parseGameId(urlFormat, dateStr, client, ids);
            target = target.plusMonths(1);
        }
        parseGameId(urlFormat, "2020%E5%89%8D", client, ids);
        return ids;
    }


    private static void parseGameId(String urlFormat, String dateStr, OkHttpClient client, Set<String> ids) {
        for ( int page = 1; page < Integer.MAX_VALUE; page++ ) {
            Request request = buildCaimoguGameRequest(urlFormat, dateStr, page);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    continue;
                }
                JSONObject bodyJson = JSON.parseObject(response.body().string());
                JSONArray data = bodyJson.getJSONArray("data");
                if (data==null|| data.isEmpty()) {
                    break;
                }
                for (Object item : data) {
                    JSONObject jsonObject = (JSONObject) item;
                    String id = jsonObject.getString("id");
                    String name = jsonObject.getString("name");
                    ids.add(id);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static Request buildCaimoguGameRequest(String urlFormat,String dateStr ,int page) {
        return new Request.Builder()
                .url(String.format(urlFormat, dateStr, page))
                .addHeader("Host","www.caimogu.cc")
                .addHeader("X-Requested-With","XMLHttpRequest")
                .build();
    }


    /**
     * 踩蘑菇评分
     * @param id 游戏Id
     * @param caiMoGuToken toKen
     * @return true 成功评分 没有评分
     */
    public static boolean actSore(String id,String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        FormBody formBody = new FormBody.Builder()
                .add("id", id.toString())
                .add("type", "2")
                .add("score", "10")
                .add("content", "神中神非常好玩")
                .build();
        Request request = new Request.Builder()
                .url("https://www.caimogu.cc/game/act/score") // 测试API，可替换为实际接口
                .post(formBody)
                .addHeader("Host","www.caimogu.cc")
                .addHeader("Cookie","CAIMOGU="+caiMoGuToken+";")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject jsonObject = JSON.parseObject(response.body().string());
                Integer status = jsonObject.getInteger("status");
                String info = jsonObject.getString("info");
                log.error(jsonObject.toJSONString());
                return  status ==1&&info.isEmpty();
            }
        } catch (Exception ignored) {

        }
        return false;
    }
}
