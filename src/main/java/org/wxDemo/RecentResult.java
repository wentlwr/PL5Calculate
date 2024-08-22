package org.wxDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class RecentResult {

    private final int startPage;
    private final int endPage;
    private static final String API_URL = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry?gameNo=350133&provinceId=0&pageSize=100&isVerify=1&termLimits=0&pageNo=";
    // 页码范围
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public RecentResult(int startPage1, int endPage1) {
        this.startPage = startPage1;
        this.endPage = endPage1;
    }


    public List<String> call() {
        System.out.println(Thread.currentThread().getName());
        int startPage = this.startPage;
        int endPage = this.endPage;
        List<String> counts = new ArrayList<>();
        for (; startPage <= endPage; startPage++) {
            String url = API_URL + startPage;
            String count = getCount(url);
            counts.add(count);
        }
        return counts;
    }

    /*
    发送请求，获取近期中奖数据
     */
    public static String getCount(String url) {
        StringBuilder content = new StringBuilder();
        try {
            // 创建URL对象
            URL getUrl = new URL(url);

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();

            // 设置请求方法为GET
            connection.setRequestMethod("GET");

            // 设置不使用缓存
            connection.setUseCaches(false);
            // 发送请求并获取响应码
            int responseCode = connection.getResponseCode();

            // 检查HTTP响应是否成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应流
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
            } else {
                logger.error("GET recent result response failed");
            }

            // 关闭连接
            connection.disconnect();
        } catch (Exception e) {
            logger.error("Get recent result error", e);
        }
        return content.toString();
    }
}
