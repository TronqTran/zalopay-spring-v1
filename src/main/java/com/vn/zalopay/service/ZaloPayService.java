package com.vn.zalopay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.zalopay.config.ZaloPayConfig;
import com.vn.zalopay.crypto.HMACUtil;
import com.vn.zalopay.util.ZaloPayUtil;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloPayService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ZaloPayConfig zaloPayConfig;

    public Map<String, Object> createOrder(long amount, String user) throws Exception {
        // Generate order ID
        int randomId = new Random().nextInt(1000000);
        String appTransId = ZaloPayUtil.getCurrentTimeString("yyMMdd") + "_" + randomId;
        
        // Initialize base data
        Map<String, Object> embedData = new HashMap<>();
        List<Object> items = new ArrayList<>();
        
        // Build order data
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", zaloPayConfig.getAppid());
        order.put("app_trans_id", appTransId);
        order.put("app_time", System.currentTimeMillis());
        order.put("app_user", user);
        order.put("amount", amount);
        order.put("description", "Payment for order #" + randomId);
        order.put("bank_code", "");
        order.put("callback_url", zaloPayConfig.getEndpoints().getCallback());
        order.put("item", objectMapper.writeValueAsString(items));
        order.put("embed_data", objectMapper.writeValueAsString(embedData));

        // Generate MAC
        String data = String.join("|",
                order.get("app_id").toString(),
                order.get("app_trans_id").toString(),
                order.get("app_user").toString(),
                order.get("amount").toString(),
                order.get("app_time").toString(),
                order.get("embed_data").toString(),
                order.get("item").toString()
        );
        order.put("mac", HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data));

        // Send HTTP Request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        order.forEach((key, value) -> formParams.add(key, value.toString()));
        
        log.info("Creating order - AppTransId: {} Description: {}", appTransId, order.get("description"));
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formParams, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                zaloPayConfig.getEndpoints().getCreate(),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                }
        );
        
        return response.getBody();
    }

    public Map<String, List<String>> getBankList() throws Exception {
        String appid = zaloPayConfig.getAppid();
        String key1 = zaloPayConfig.getKey1();
        String endpoint = zaloPayConfig.getEndpoints().getBanks();

        String reqtime = String.valueOf(System.currentTimeMillis());
        String mac = ZaloPayUtil.generateMac(key1, appid, reqtime);

        // Tạo URL với query params
        String url = UriComponentsBuilder.fromHttpUrl(endpoint)
                .queryParam("appid", appid)
                .queryParam("reqtime", reqtime)
                .queryParam("mac", mac)
                .toUriString();

        // Gọi GET API
        String response = restTemplate.getForObject(url, String.class);
        JsonNode result = new ObjectMapper().readTree(response);

        JsonNode banksObj = result.get("banks");

        Map<String, List<String>> bankMap = new HashMap<>();
        banksObj.fields().forEachRemaining(entry -> {
            List<String> bankList = new ArrayList<>();
            entry.getValue().forEach(bank -> bankList.add(bank.asText()));
            bankMap.put(entry.getKey(), bankList);
        });

        return bankMap;
    }

    public String handleCallback(String jsonStr) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Parse callback JSON string to Map
            Map<String, Object> callbackMap = objectMapper.readValue(jsonStr, new TypeReference<>() {
            });
            String dataStr = (String) callbackMap.get("data");
            String requestMac = (String) callbackMap.get("mac");

            // Calculate MAC for verification
            String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey2(), dataStr);

            assert mac != null;
            if (!mac.equalsIgnoreCase(requestMac)) {
                // MAC verification failed
                response.put("return_code", -1);
                response.put("return_message", "MAC verification failed");
            } else {
                // Parse data JSON string to Map
                Map<String, Object> dataMap = objectMapper.readValue(dataStr, new TypeReference<Map<String, Object>>() {});
                String appTransId = (String) dataMap.get("app_trans_id");
            
                // Process successful callback
                log.info("Processing successful callback for transaction: {}", appTransId);
                // TODO: update transaction status in database
            
                response.put("return_code", 1);
                response.put("return_message", "success");
            }
        } catch (Exception e) {
            // Handle any parsing or processing errors
            log.error("Error processing callback: {}", e.getMessage(), e);
            response.put("return_code", 0);
            response.put("return_message", e.getMessage());
        }

        try {
            // Convert response Map to JSON string
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            log.error("Error serializing response: {}", ex.getMessage(), ex);
            return "{\"return_code\":0,\"return_message\":\"json error\"}";
        }
    }

    public Map<String, Object> queryOrderStatus(String appTransId) throws Exception {
        // Generate MAC string for validation
        String data = String.join("|",
                zaloPayConfig.getAppid(),
                appTransId,
                zaloPayConfig.getKey1()
        );
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data);

        // Prepare request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("app_id", zaloPayConfig.getAppid());
        params.add("app_trans_id", appTransId);
        params.add("mac", mac);

        // Setup request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create HTTP request entity
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // Send request and get response
        log.info("Querying order status for transaction: {}", appTransId);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                zaloPayConfig.getEndpoints().getQuery(),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Return response body
        return response.getBody();
    }

    public Map<String, Object> refund(String zpTransId, String amount, String description) throws Exception {
        // Generate refund ID
        long timestamp = System.currentTimeMillis();
        String uid = timestamp + String.format("%03d", 111 + new Random().nextInt(888));
        String mRefundId = ZaloPayUtil.getCurrentTimeString("yyMMdd") + "_" + zaloPayConfig.getAppid() + "_" + uid;

        // Generate MAC string
        String data = String.join("|",
                zaloPayConfig.getAppid(),
                zpTransId,
                amount,
                description,
                String.valueOf(timestamp)
        );
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data);

        // Prepare request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("app_id", zaloPayConfig.getAppid());
        params.add("zp_trans_id", zpTransId);
        params.add("m_refund_id", mRefundId);
        params.add("timestamp", String.valueOf(timestamp));
        params.add("amount", amount);
        params.add("description", description);
        params.add("mac", mac);

        // Setup request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create HTTP request entity
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // Send request and get response
        log.info("Processing refund request - RefundId: {} Amount: {} Description: {}",
                mRefundId, amount, description);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                zaloPayConfig.getEndpoints().getRefund(),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                }
        );

        return response.getBody();
    }

    public Map<String, Object> queryRefundStatus(String mRefundId) throws Exception {
        // Generate MAC string
        String timestamp = String.valueOf(System.currentTimeMillis());
        String data = String.join("|",
                zaloPayConfig.getAppid(),
                mRefundId,
                timestamp
        );
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data);

        // Prepare request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("app_id", zaloPayConfig.getAppid());
        params.add("m_refund_id", mRefundId);
        params.add("timestamp", timestamp);
        params.add("mac", mac);

        // Setup request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create HTTP request entity
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // Send request and get response
        log.info("Querying refund status for refund ID: {}", mRefundId);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                zaloPayConfig.getEndpoints().getRefundQuery(),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        return response.getBody();
    }
}