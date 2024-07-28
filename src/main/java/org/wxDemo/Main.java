package org.wxDemo;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String API_URL = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry?gameNo=350133&provinceId=0&pageSize=100&isVerify=1&termLimits=0&pageNo=";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<String> resultList = new ArrayList<>();
    private static final List<List<String>> JsonList = new ArrayList<>();
    private static final List<String> NumList = new ArrayList<>();
    ;

    public static void main(String[] args) {

        for (int i = 1; i <= 70; i++) {
            String recentWinningData = getRecentResult(API_URL + i);
            parseJson(recentWinningData);
        }
        JsonList.add(resultList);
        calculate(JsonList);
        writeToExcel();
        MaxCount();
    }


    /*
    发送请求，获取近期中奖数据
     */
    public static String getRecentResult(String url) {
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

    /*
    解析返回的中奖数据
     */
    public static List<String> parseJson(String jsonStr) {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            logger.error("Parse json error", e);
            throw new RuntimeException(e);
        }
        JsonNode listNode = rootNode.get("value").get("list");
        for (JsonNode node : listNode) {
            JsonNode jsonNode = node.get("lotteryDrawResult");
            if (jsonNode.isNull()) {
                break;
            } else {
                resultList.add(jsonNode.toString());
            }
        }
        return resultList;
    }

    /*
     * 计算连续大/小天数，判断是否打奖，并发送微信通知
     */
    public static void calculate(List<List<String>> list) {
        if (list.isEmpty()) {
            logger.error("数据列表为空");
            return;
        }
        for (List<String> strings : list) {
            for (String s : strings) {
                s.replaceAll("^\"|\"$", "");
                NumList.add(s);
            }
        }
        int bigMaxCount = 0;
        int littleMaxCount = 0;

        int bigCount = 0;
        int littleCount = 0;
        int maxIndex=0;
        int minIndex = 0;
        int bigNum = 0;
        int littleNum = 0;
        int jishuCount = 0;
        int jishuMaxCount = 0;
        int jishu = 0;
        int jishuIndex = 0;
        int oushu = 0;
        int oushuCount = 0;
        int oushuMaxCount = 0;
        int oushuIndex = 0;
        boolean isBig;
        for (int i = 0; i < NumList.size(); i++) {
            int number = Integer.parseInt(NumList.get(i).replaceAll("^\"|\"$", "").split(" ")[0]);
            isBig = number >= 5;
            if(isBig){
                bigNum++;
            }else{
                littleNum++;
            }
            //奇偶个数计数
            int jiOu = number % 2;
            if(jiOu ==0){
                oushu++;
                oushuCount++;
                if(jishuCount > jishuMaxCount){
                    jishuMaxCount = jishuCount;
                    jishuIndex = i;
                }
                jishuCount = 0;
            }else{
                jishu++;
                jishuCount++;
                if(oushuCount > oushuMaxCount){
                    oushuMaxCount = oushuCount;
                    oushuIndex = i;
                }
                oushuCount=0;
            }
            //大小号个数计数
            if (isBig) {
                bigCount++;
                if (littleCount > littleMaxCount) {
                    littleMaxCount = littleCount;
                    minIndex = i;
                }
                littleCount = 0;
            } else {
                littleCount++;
                if (bigCount > bigMaxCount) {
                    bigMaxCount = bigCount;
                    maxIndex = i;
                }
                bigCount = 0;
            }
        }
        System.out.println("最高连续大号为" + bigMaxCount + "个，在第"+(maxIndex-9)+"个数据附近开始");
        System.out.println("最高连续小号为" + littleMaxCount + "个,在第"+(minIndex-9)+"个数据附近开始");
        System.out.println("最高连续奇数为" + jishuMaxCount + "个，在第"+(jishuIndex-9)+"个数据附近开始");
        System.out.println("最高连续偶数为" + oushuMaxCount + "个，在第"+(oushuIndex-9)+"个数据附近开始");
        System.out.println("大号个数" + bigNum + ",小号个数"+littleNum);
        System.out.println("奇数个数" + jishu + ",偶数个数"+oushu);
    }

    public static void writeToExcel(){
        String userDir = System.getProperty("user.dir");
        String fileName = userDir+ File.separator+ System.currentTimeMillis()+ "simpleWrite.xlsx";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        // 将List<String>转换为List<DataEntity>
        List<DataEntity> data = new ArrayList<>();
        for (String line : NumList) {
            String s = line.replaceAll("^\"|\"$", "");
            String[] parts = s.split(" ");
            data.add(new DataEntity(parts[0], parts[1], parts[2], parts[3], parts[4]));
        }
        EasyExcel.write(fileName, DataEntity.class).sheet("Sheet1").doWrite(data);
    }

    public static void MaxCount(){
        int count0=0, count1=0, count2=0, count3=0, count4=0, count5=0, count6=0, count7=0, count8=0, count9=0;

        for (int i = 0; i < NumList.size(); i++) {
            int number = Integer.parseInt(NumList.get(i).replaceAll("^\"|\"$", "").split(" ")[0]);
            switch(number){
                case 0:
                    count0++;
                case 1:
                    count1++;
                case 2:
                    count2++;
                case 3:
                    count3++;
                case 4:
                    count4++;
                case 5:
                    count5++;
                case 6:
                    count6++;
                case 7:
                    count7++;
                case 8:
                    count8++;
                case 9:
                    count9++;
            }
        }
        List<Integer> countList = new ArrayList<>(10);
        countList.add(count0, count1);
        countList.add(count1);
        countList.add(count2);
        countList.add(count3);
        countList.add(count4);
        countList.add(count5);
        countList.add(count6);
        countList.add(count7);
        countList.add(count8);
        countList.add(count9);
       getMaxCount(countList);
    }

    private static void getMaxCount(List<Integer> countList) {
        int max = 0;
        for (int i = 0; i < countList.size(); i++) {
            int count = countList.get(i);
            int size = NumList.size();
            double percent = ((double)count/size)*100;
            // 格式化输出，保留两位小数
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedPercentage = df.format(percent);
            System.out.println("首位为" +i+"的数字出现的次数是："+countList.get(i)+"占比："+formattedPercentage+"%");
            max= Math.max(max,countList.get(i));
        }
    }
}
