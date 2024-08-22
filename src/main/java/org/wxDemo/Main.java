package org.wxDemo;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final List<String> resultList = new ArrayList<>();
    private static JsonNode rootNode;

    //配置前 N 个月统计
    private static final Integer BeforeMonth = 10;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(11);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 页码范围
        int[][] pageRanges = {
                {1, 10},
                {11, 20},
                {21, 30},
                {31, 40},
                {41, 50},
                {51, 60},
                {61, 70},
                {71, 80},
                {81, 90},
                {91, 100}
        };
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        // 创建并启动所有任务
        for (int[] range : pageRanges) {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                RecentResult recentResult = new RecentResult(range[0], range[1]);
                return recentResult.call();
            }, executorService);
            futures.add(future);
        }
        // 收集结果并保持顺序
        ArrayList<String> countsList = new ArrayList<>();
        for (CompletableFuture<List<String>> future : futures) {
            countsList.addAll(future.get());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("执行总耗时" + (endTime - startTime));
        countsList.forEach(Main::parseJson);
        Thread thread = new Thread(Main::writeToExcel);
        thread.start();

        executorService.shutdown();
        getHosMonNumber();
        calculate(resultList);
        MaxCount();
    }

   public static String checkJson(String jsonStr) {
           ObjectMapper objectMapper = new ObjectMapper();
           try {
               rootNode = objectMapper.readTree(jsonStr);
               if (rootNode.isObject()) {
                   return "obj";
               } else if (rootNode.isArray()) {
                   return "array";
               }
               // 继续为字符串、数字、布尔值和null添加检查
           } catch (IOException e) {
               e.printStackTrace();
           }
           return "error";
   }

    /*
    解析返回的中奖数据
     */
    public static void parseJson(String jsonStr) {
        String s = checkJson(jsonStr);
        JsonNode jsonNode;
        if ("obj".equals(s)) {
             jsonNode = rootNode.get("value").get("list");
             jsonToNode(jsonNode);
        }
        if ("array".equals(s)){
            JSONArray jsonArray = JSONObject.parseArray(jsonStr);
            List<JsonNode> list = JSONObject.parseArray(jsonArray.toJSONString(), JsonNode.class);
            jsonToNode(list);
        }
    }

    /**
     * json 数据类型转换
     * @param t t
     * @param <T> T
     */
    public static<T extends Iterable<JsonNode>> void jsonToNode(T t){
        if(t instanceof JsonNode){
            for (JsonNode node : t) {
                JsonNode jsonNode = node.get("lotteryDrawResult");
                if (jsonNode.isNull()) {
                    break;
                } else {
                    String jsonString = jsonNode.toString();
                    resultList.add(jsonString);
                }
            }
        }
        if (t instanceof List){
            for (JsonNode node : t) {
                JsonNode jsonNode = node.get("lotteryDrawResult");
                if (jsonNode.isNull()) {
                    break;
                } else {
                    resultList.add(jsonNode.toString());
                }
            }
        }
    }

    /*
     * 计算连续大/小天数，判断是否打奖，并发送微信通知
     */
    public static void calculate(List<String> strings) {
        if (strings.isEmpty()) {
            logger.error("数据列表为空");
            return;
        }
        int bigMaxCount = 0;
        int littleMaxCount = 0;

        int bigCount = 0;
        int littleCount = 0;
        int maxIndex = 0;
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
        for (int i = 0; i < resultList.size(); i++) {
            int number = Integer.parseInt(resultList.get(i).replaceAll("^\"|\"$", "").split(" ")[0]);
            isBig = number >= 5;
            if (isBig) {
                bigNum++;
            } else {
                littleNum++;
            }
            //奇偶个数计数
            int jiOu = number % 2;
            if (jiOu == 0) {
                oushu++;
                oushuCount++;
                if (jishuCount > jishuMaxCount) {
                    jishuMaxCount = jishuCount;
                    jishuIndex = i;
                }
                jishuCount = 0;
            } else {
                jishu++;
                jishuCount++;
                if (oushuCount > oushuMaxCount) {
                    oushuMaxCount = oushuCount;
                    oushuIndex = i;
                }
                oushuCount = 0;
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
        System.out.println("最高连续大号为" + bigMaxCount + "个，在第" + (maxIndex - 9) + "个数据附近开始");
        System.out.println("最高连续小号为" + littleMaxCount + "个,在第" + (minIndex - 9) + "个数据附近开始");
        System.out.println("最高连续奇数为" + jishuMaxCount + "个，在第" + (jishuIndex - 9) + "个数据附近开始");
        System.out.println("最高连续偶数为" + oushuMaxCount + "个，在第" + (oushuIndex - 9) + "个数据附近开始");
        System.out.println("大号个数" + bigNum + ",小号个数" + littleNum);
        System.out.println("奇数个数" + jishu + ",偶数个数" + oushu);
    }

    public static void writeToExcel(){
        String userDir = System.getProperty("user.dir");
        String fileName = userDir+ File.separator+ System.currentTimeMillis()+ "simpleWrite.xlsx";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        // 将List<String>转换为List<DataEntity>
        List<DataEntity> data = new ArrayList<>();
        for (String line : resultList) {
            String s = line.replaceAll("^\"|\"$", "");
            String[] parts = s.split(" ");
            data.add(new DataEntity(parts[0], parts[1], parts[2], parts[3], parts[4]));
        }
        EasyExcel.write(fileName, DataEntity.class).sheet("Sheet1").doWrite(data);
    }

    public static void MaxCount(){
        int count0=0, count1=0, count2=0, count3=0, count4=0, count5=0, count6=0, count7=0, count8=0, count9=0;

        for (int i = 0; i < resultList.size(); i++) {
            int number = Integer.parseInt(resultList.get(i).replaceAll("^\"|\"$", "").split(" ")[0]);
            switch(number){
                case 0:
                    count0++;
                    break;
                case 1:
                    count1++;
                    break;
                case 2:
                    count2++;
                    break;
                case 3:
                    count3++;
                    break;
                case 4:
                    count4++;
                    break;
                case 5:
                    count5++;
                    break;
                case 6:
                    count6++;
                    break;
                case 7:
                    count7++;
                    break;
                case 8:
                    count8++;
                    break;
                case 9:
                    count9++;
                    break;
            }
        }
        List<Integer> countList = new ArrayList<>(10);
        countList.add(count0);
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

    /**
     * 获取历史月份出现数字的占比
     */
    public static  void getHosMonNumber(){
        // 获取当前年月的组合
        YearMonth currentYearMonth = YearMonth.now();
        int dayNumber = LocalDate.now().getDayOfMonth();
        int now = LocalDateTime.now().getHour();
        if (now < 21){
            dayNumber -= 1;
        }
        for(int i = 0; i < BeforeMonth ;i++) {
            YearMonth yearMonth1 = currentYearMonth.minusMonths(i);
            int dayNum = yearMonth1.lengthOfMonth();
            dayNumber += dayNum;
        }
        // 获取前 n 个月的数据
        List<String> beforeMonthRes = resultList.subList(0, Math.min(dayNumber, resultList.size()));
        ArrayList<Integer> resList = new ArrayList<>(beforeMonthRes.size());
        for (String string : beforeMonthRes) {
            int firstNumber = getFirstNumber(string);
            if (firstNumber == -1){
                throw  new RuntimeException("数据有误");
            }
            resList.add(firstNumber);
        }
        Map<Integer, Integer> countMap = new HashMap<>(10);
        for (int number : resList) {
            countMap.put(number, countMap.getOrDefault(number, 0) + 1);
        }
        int totalNumbers = resList.size();
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            int count = entry.getValue();
            double percentage = ((double) count / totalNumbers) * 100;
            System.out.printf("数字 %d 出现了 %d 次，占总数的 %.2f%%%n", entry.getKey(), count, percentage);
        }

    }

    /**
     * 获取字符串前一个数字
     * @param str
     * @return int
     */
    public static int getFirstNumber(String str) {
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        } else {
            return -1;
        }
    }


    private static void getMaxCount(List<Integer> countList) {
        int max = 0;
        for (int i = 0; i < countList.size(); i++) {
            int count = countList.get(i);
            int size = resultList.size();
            double percent = ((double)count/size)*100;
            // 格式化输出，保留两位小数
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedPercentage = df.format(percent);
            System.out.println("首位为" +i+"的数字出现的次数是："+countList.get(i)+"占比："+formattedPercentage+"%");
            max= Math.max(max,countList.get(i));
        }
    }
}
