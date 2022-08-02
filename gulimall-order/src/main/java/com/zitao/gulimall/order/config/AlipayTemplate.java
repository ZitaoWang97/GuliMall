package com.zitao.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.zitao.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 在支付宝创建的应用的id
    private String app_id = "2021000121638994";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCA47GdsvPK+0a3lIJqHc09z+jSP1TpTdy0ztWeq0NQgFQWMfdU1nk6BDtvs6gosPa9VCDNYBDJvp/4KTbc92thvSJxXYiY/D+5JGT1kkmeAsLsmWrN/O0KeCt69Ts42C4/5QUboFEb+zau9jPH/5F1N06XVyIwXf3gL0pPLTL7BksdobdsxW9fCcKBBsWGsOVV+hvY1vKSAsOok3cU8YcCUpbLc+IG/90pGIELhtfWGqD5/ljNfwXE/9WUtl1/s5GkchAejjXBzyPZTYB8Hyn5kz2LMvPWiKHaKfJ1h9OQP/+n8ieTcZGKb+AqIvzqO4CaNjqR4vMZEHUKTdUiSIiFAgMBAAECggEAZ9qQOJiENm9IKQ5CE01WHlNTr22WdrLlfs8Wc9Zd1BTDLGswdqymW6gjWDsz6S3GcwHBuJqKjHTMI0LaUSwP215xzVTnhxXiJsV7kCBCr05cUBbCZ55ARepUZqwI/yG6CoP4HK2ODXTbOBlr2eBFDWT2L53nD3/829JfFvM9m+PujWeTmPK6H/QfsAscUu4W/XZsH6OPAVO5YAg0jT0TPJu38A0N3J8kjUCuFpwLdzoL0eShQ+LcQZv6j14F9hQkLruj1GtEt0sI1gkFYogm+LzHtXECWXpv/Kh6PlsW9hNMHMDAKBXZxTvQ8XOS/3DfjaINW54sLWKue66tUTbUVQKBgQC3hZMLStRT3xmTRfbCDQsbZsCerITG4v8w+Uv11uiIIzrRdBivoq2dY9YLQarvvVGtNxAbEGYpPhb2HXF+KC0cU2SNOy2DbYOVB3jAUprUTIP4mVfdZgESDkwk1tSZdhaaCpsB1L7ovNe0maTPc6Y3ZBHE053AyTDBp2W2XIl/fwKBgQCzyq2IBLom5Mve/QkPW2ee1t30OeRB2ndrIKIzRvT8emiCT61jOI4Q122eaXp5bdBKpk1MANwRnQn2pYd3NHSXu2fVY3PPnMRJtV680b++FuEqB0jeOLycALSlP3Zu+NJkTPRENaT+eAqs45ZVC7fK/tcUJyaaUJ3BlD4QEIr5+wKBgBH5mExr2N4aJPQizyd0hZj6eHVSKnMceqg+Uq1SjlX/NVychWp2gZC/3ZAer3Jp1Z0knoQ4F/mEJlWWzq3vm3OBy4B1wmB0EoY/RBl6PcX7dJWd11hNDF/LXLPjAtHBGjeeQE5umwBKtsnunpDfH/Ge8IqZu4LNceQV1EWHTMRbAoGAHaOn/+hnl8nYIiasu69h4bmiVYTFsGWHkk2K79EeyhCT8geYmebU5Mne9GKHIkWMNQI9c+4gWXb+EX8wWeJ35huq/m6qxOsfINeZip85cOjsrbUNujS4Qy+KmENiDuFOLeLR0fzV+m1ntSC/w71uyzQ/2L6rin0HxMXfhV50yskCgYEApck5lO5Bad3y55oRQ942kbFt1zjhCL14arcTzoozQBelKLcHtk9jMdHHCVDdV3dwO24vGwKBaSzVJrEgGpLfa953h95W+Emsvs7yGN3yuG6FyDQ0VgYYjeuytTooqYg1ER0d7zl3yEgn+xEdTzAL/jzBSFOPU+i1roGfQ7T4u6M=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuUmP2yNmZ23Kmgne1vQksADFGa82XXuX5JKNbfqd/l3OjsRjM6vbUQSHlGMKOgKRQmEbVQD2SwaZKEIRaVYe1gxBnu7WlRtNNPEd/SaKsY5/HHiVEs+8eJeP9B2oCNNKWChZWa8p4LgksK0gzIKXe6bm6lB+0bahVTUYVZYgifTPg0YAQWswMIw8HW27hEeQlL9UvM4QmRe3jT7irstlkkUf6vbK3jcxnHbD9P6v7SMWMXe2Dv3RU7n4r1+/lO130rsmp6phVdNmrzrH43QhmLbRd6Rd+AyqJB+ZkM81/1fwcKpUu59eRLMUH5dfRL+QmLyT4P03Oqa2bi91WcYtMQIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private String notify_url = "http://order.gulimall.com/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 同步通知，支付成功，一般跳转到成功页
    private String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"1m\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应：" + result);

        return result;

    }
}
