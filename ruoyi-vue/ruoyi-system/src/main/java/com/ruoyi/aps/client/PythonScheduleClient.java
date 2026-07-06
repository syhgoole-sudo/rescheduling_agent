package com.ruoyi.aps.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.aps.client.dto.InitialScheduleRequest;
import com.ruoyi.aps.client.dto.InitialScheduleResponse;
import com.ruoyi.aps.client.dto.LocalRescheduleRequest;
import com.ruoyi.aps.client.dto.LocalRescheduleResponse;

@Component
public class PythonScheduleClient
{
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${aps.algorithm.base-url:http://localhost:8000}")
    private String baseUrl;

    public InitialScheduleResponse generateInitialSchedule(InitialScheduleRequest request)
    {
        String url = baseUrl + "/api/schedule/initial";
        String requestBody = JSON.toJSONString(request);
        if (requestBody == null || "null".equals(requestBody))
        {
            throw new IllegalStateException("Initial schedule request body is null.");
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.getBytes(StandardCharsets.UTF_8)))
                .build();
        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("Python schedule service returned HTTP "
                        + response.statusCode() + ", requestBodyLength=" + requestBody.length()
                        + ": " + response.body());
            }
            return JSON.parseObject(response.body(), InitialScheduleResponse.class);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot connect to Python schedule service: " + url, e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python schedule service call interrupted.", e);
        }
    }

    public LocalRescheduleResponse localReschedule(LocalRescheduleRequest request)
    {
        String url = baseUrl + "/api/schedule/reschedule/local";
        String requestBody = JSON.toJSONString(request);
        if (requestBody == null || "null".equals(requestBody))
        {
            throw new IllegalStateException("Local reschedule request body is null.");
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.getBytes(StandardCharsets.UTF_8)))
                .build();
        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("Python local reschedule service returned HTTP "
                        + response.statusCode() + ", requestBodyLength=" + requestBody.length()
                        + ": " + response.body());
            }
            return JSON.parseObject(response.body(), LocalRescheduleResponse.class);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot connect to Python local reschedule service: " + url, e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python local reschedule service call interrupted.", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> explainReschedule(Map<String, Object> request)
    {
        String url = baseUrl + "/api/agent/explain-reschedule";
        String requestBody = JSON.toJSONString(request);
        if (requestBody == null || "null".equals(requestBody))
        {
            throw new IllegalStateException("Agent explain request body is null.");
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.getBytes(StandardCharsets.UTF_8)))
                .build();
        try
        {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("Python agent service returned HTTP "
                        + response.statusCode() + ", requestBodyLength=" + requestBody.length()
                        + ": " + response.body());
            }
            return JSON.parseObject(response.body(), Map.class);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot connect to Python agent service: " + url, e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python agent service call interrupted.", e);
        }
    }
}
