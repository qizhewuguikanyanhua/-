package com.tianjin;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ImageCrawl {

    private static String url = "https://www.nipic.com/topic/show_29204_1.html";

    public static void main(String[] args) {
        apacheHttpClient();
        jsoup();
    }

    private static void jsoup() {
        try {
            Document document = Jsoup.connect(url).get();
            Elements select = document.select("li.new-search-works-item");
            for (Element element : select) {
                Elements imgElement = element.select("a > img");
                String imgUrl = imgElement.attr("src");
                if (imgUrl.startsWith("//")) {
                    imgUrl = "https:" + imgUrl;
                }
                Connection.Response response = Jsoup.connect(imgUrl).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36").ignoreContentType(true).execute();
                String flieName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(10);
                FileUtils.copyInputStreamToFile(new ByteArrayInputStream(response.bodyAsBytes()), new File("D:\\新建文件夹\\" + flieName + ".png"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void apacheHttpClient() {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");
        try (CloseableHttpResponse httpResponse = (CloseableHttpResponse) httpClient.execute(httpGet)) {
            HttpEntity entity = httpResponse.getEntity();
            String info = EntityUtils.toString(entity);
            System.out.println(info);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }


}
