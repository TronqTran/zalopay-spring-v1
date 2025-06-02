package com.vn.zalopay.controller;

import com.vn.zalopay.service.ZaloPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/zalo-pay")
@RequiredArgsConstructor
public class ZaloPayController {
    private final ZaloPayService zaloPayService;

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@RequestParam long amount, @RequestParam String user) throws Exception {
        return zaloPayService.createOrder(amount, user);
    }

    @GetMapping("/banks")
    public Map<String, List<String>> getBanks() throws Exception {
        return zaloPayService.getBankList();
    }

    @PostMapping("/callback")
    public String receiveCallback(@RequestBody String requestBody) {
        System.out.println("Received callback: " + requestBody);
        return zaloPayService.handleCallback(requestBody);
    }

    @GetMapping("/query")
    public Map<String, Object> query(@RequestParam("appTransId") String appTransId) throws Exception {
        return zaloPayService.queryOrderStatus(appTransId);
    }

    @PostMapping("/refund")
    public Map<String, Object> refund(
            @RequestParam String zpTransId,
            @RequestParam String amount,
            @RequestParam String description) throws Exception {

            return zaloPayService.refund(zpTransId, amount, description);

    }
    @GetMapping("/refund-status")
    public Map<String, Object> getRefundStatus(@RequestParam("m_refund_id") String mRefundId) throws Exception {
        return zaloPayService.queryRefundStatus(mRefundId);
    }
}
