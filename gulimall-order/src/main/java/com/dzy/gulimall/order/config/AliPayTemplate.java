package com.dzy.gulimall.order.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.dzy.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AliPayTemplate {
    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id = "2021000117697897";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCUOdiyLvSUDxFnzoKvWz4bJMSWF0VzyaQjJKQNiDrSW6BlIYnKG2M8evktnMAlmN8wP+v4t/RjnbUh1ALU70DaTIOj/9GJYGQre6JcsPfbn1ZGJV6n2bdnGZTZp0fAaVGUq/pmkf5Qu36T9SlHxtDbGJuu8vB6ImlNmLa4fYo7Pz+GU3cAuDgQxRUYyECo6h006kxpNnmhxMC+IJwVCcfmMbKOdqxGjuuPVi1vY1GIk497T9r7fo202YNWAGKFzgrdm2CNvVT6LOP6p3rAY+0exRH6fBOeGMC4Buo1Hl2N63ffHdW+8bhiZ3wNfKa5puCjrbf6py8wkAqo6nx5On0pAgMBAAECggEBAJP4/6Q6Df304OphlcMbcm/Ej+BKURJbXVmcDY5YzmgMAp6TEaxLFRFernzjfErMRxsy5Q6CDh6YZSflMAKE6phDRDOngfYD3Vwva5HfNUDx2z3htpIYXeg1+IwkXBPwz/l8ilh1LI5J7v4kA9bc2smu0lSfwIQc8ET/zIF3ClVDspr8UdZWhEIhOuZm4fAyP21T3gH7utjkKJXZ4EBvt1oc7jHtWne81Yn1+BYqFmRqxSPMPGuLEzjo7REFY63J8hz7qV+2ECH4NE2XSyk+/MuPLIoMRsiLzlcB9yv1UUZy6aLWMwFueg4e419w+6JgtokyrZyKIccLaIx4rGaLRoECgYEA2SYUjrwulEiIP1QCx05fay3KtYhKQfb6R65TlwtCO30OXIyMiX8irFhcpC4RC/eJ7UyZc44ewZFOYb0CjyGJeCcLSYhrJHMOcgTFaDx9vTNXLg95HDSPy8n0of3lP/A0ChJO/zckzV/5/2t+BiQyze/JI1doBaG9wSX1p0JFtTECgYEArr7xCq7NXIy0iKOyWbi/WzzqnpNo3uKssqzFnrobCNJAoWa/TtBs2TLySA/rrYab8xE3PRI6yFLnhK4Be0biNSkR9voN3g9UKvtP1FpoDC9jY0QBvQxn/L+l2QUV4xuU0LTxp8jmqa+BltsJAQHmv4LpIMzNb+4ocEXiITuzKXkCgYAImEtEAwrM77oE16xsexunwF4/pS9bQQ1S/QFt/xIWDLMe0qlVX+TdcXnpPbGLCbc530hLnJp3CutRTwvk4mzwhosZ0/55qNwvgyTVJV/Mt1WCCza9+Q6gAFRgfQ4v47ALngHmdCZOwrfAdl68m34vTIhnj7QcVPSPypLYlfTwYQKBgF8evOLNyCeqx9LhGIJSWId3n1b2cs1wogUYmvIN6A1jNZ8l6NkkBzwryUqEsAtjDJzBMEpkE+9bgX7toQzTxuqdEpRYBRhtS6mh5xB2rdCEMxujtzaK/EfOop6BcCqRpu38sNZeO4D+chIF5N7RhCOXRQN/uAxHnWeQAsmIT7wxAoGAZ9ZWSSJ5wMKFNsIDOZFAx1vUJobPcK7Ud4p11904XHoOS7gHYrHtkZh1ITWlptlgiOnovyXeaHemu8uAGSTGJXupOo2ReTFLfj03TNGZiMOFQgWujDwEBLvcZQKP+XO5ZEJ7aS8n8QiYM4Oh9kxjmdAEEJgSVjB/r05Oac148Rc=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgggbA6Vj7xk/2sLgNh/L24Ve6BcbSPtLs+Rt6wZcU/0s226NeRUwyIqttaQ4Uc7qzh3aTl/7IxWzY8MHfnPxpEcjD2kFQgU3pVrglTiQbrRn97hNA0uMFN3E1oDrCbjMnLYCSvDSuRduPISvx2hDvcfjJZfdXnHowiy35dUgUzsjpYiIhELOX1k3hRi06RRxOfuBRT/Stj9/qMgvKP/UU3uJsbgeM0XxRDV6PW91mUtzm7S5047eFbYjtn25IGOzBsmHYK3udO9N1vuYruQntPdG8vteHZmPCZ46uu7QzxnCejr7yFGbeq+LDDz3X93P9wQUzvkQf74DqqHtsrzTCQIDAQAB";

    // 服务器异步通知页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public String notify_url = "http://b1a8k.cdhttp.cn/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public String return_url = "http://member.gulimall.com/orderList.html";

    // 签名方式
    public String sign_type = "RSA2";

    // 字符编码格式
    public String charset = "utf-8";

    // 支付宝网关
    public String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String payOrder(PayVo payVo) throws Exception{
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, app_id, merchant_private_key, "json", charset, alipay_public_key, sign_type);

        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = payVo.getOut_trade_no();
        //付款金额，必填
        String total_amount = payVo.getTotalAmount();
        //订单名称，必填
        String subject = payVo.getSubject();
        //商品描述，可空
        String body = payVo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //若想给BizContent增加其他可选请求参数，以增加自定义超时时间参数timeout_express来举例说明
        //alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
        //		+ "\"total_amount\":\""+ total_amount +"\","
        //		+ "\"subject\":\""+ subject +"\","
        //		+ "\"body\":\""+ body +"\","
        //		+ "\"timeout_express\":\"10m\","
        //		+ "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        //请求参数可查阅【电脑网站支付的API文档-alipay.trade.page.pay-请求参数】章节

        //请求
        return alipayClient.pageExecute(alipayRequest).getBody();
    }
}
