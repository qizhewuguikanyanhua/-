package com.tianjin;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UrlPool {
    // 使用线程安全的队列存储待抓取URL
    private static final ConcurrentLinkedQueue<String> taskQueue = new ConcurrentLinkedQueue<>();
    // 线程安全的已访问集合
    private static final ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
    // 控制并发量的信号量
    private static final Semaphore semaphore = new Semaphore(50); // 最大并发数设为50
    // 线程池配置
    private static final ExecutorService executor =
            Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        taskQueue.add("https://www.nipic.com/");
        visitedUrls.put("https://www.nipic.com/", true);

        // 启动10个消费者线程
        for (int i = 0; i < 10; i++) {
            executor.submit(new CrawlerTask());
        }

        // 优雅关闭
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static class CrawlerTask implements Runnable {
        // 使用连接池优化
        private static final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    semaphore.acquire(); // 控制并发量
                    String url = taskQueue.poll();
                    if (url == null) break;

                    processUrl(url);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            }
        }

        private void processUrl(String url) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15)) // 超时设置
                        .header("User-Agent", "Mozilla/5.0") // 伪装请求头
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 200) {
                    // 使用Jsoup替代正则解析
                    Document doc = Jsoup.parse(response.body());
                    Elements links = doc.select("a[href]");

                    links.parallelStream().forEach(link -> { // 并行处理链接
                        String newUrl = link.absUrl("href");
                        if (isValidUrl(newUrl)) {
                            synchronized (visitedUrls) {
                                if (!visitedUrls.containsKey(newUrl)) {
                                    visitedUrls.put(newUrl, true);
                                    taskQueue.add(newUrl);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error processing URL: " + url);
                e.printStackTrace();
            }
        }

        private boolean isValidUrl(String url) {
            // 实现域名过滤、文件类型过滤等规则
            return url.startsWith("https://www.nipic.com/")
                    && !url.matches(".*\\.(jpg|png|gif|css|js)$");
        }
    }
}