package com.dzy.gulimall.cart.controller;


import com.dzy.gulimall.cart.Interceptor.CartInterceptor;
import com.dzy.gulimall.cart.service.CartService;
import com.dzy.gulimall.cart.to.UserInfoTo;
import com.dzy.gulimall.cart.vo.Cart;
import com.dzy.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     *  浏览器有一个cookie：user-key，标识用户身份，一个月后过期
     *  如果第一次使用购物车功能，都会给一个临时的用户身份；
     *  浏览器以后保存，每次访问都会带上这个cookie
     *
     *  登录：session里面有用户信息
     *  没有登录：按照cookie里面的user-key来识别临时用户身份
     *  第一次进入购物车，如果没有临时用户，帮忙创建一个临时用户（即向cookie中添加一个名为user-key的值）
     */
    @GetMapping("/cartlist.html")
    public String cartListPage(Model model) {
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     *  redirectAttributes
     *         addFlashAttribute() 是将数据添加到session中，并且只能使用一次
     *         addAttribute() 是将数据拼接在重定向的路径之后
     */
    @GetMapping("/addtocart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num, Model model,
                            RedirectAttributes redirectAttributes) {
        CartItem cartItem = cartService.addToCart(skuId, num);
        redirectAttributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addtocartsuccess";
    }

    @GetMapping("/addtocartsuccess")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId, Model model) {
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItem);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkCartItem(@RequestParam("skuId") Long skuId,
                                @RequestParam("checked") Integer checked) {
        cartService.checkCartItem(skuId, checked);
        return "redirect:http://cart.gulimall.com/cartlist.html";
    }

    @GetMapping("/changeItemNum")
    public String changeCartItemNum(@RequestParam("skuId") Long skuId,
                                    @RequestParam("num") Integer num) {
        cartService.changeCartItemNum(skuId, num);
        return "redirect:http://cart.gulimall.com/cartlist.html";
    }

    @GetMapping("/deleteItem")
    public String deleteCartItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteCartItem(skuId);
        return "redirect:http://cart.gulimall.com/cartlist.html";

    }

    @ResponseBody
    @GetMapping("/currentCartItems")
    public List<CartItem> getCurrentCartItems() {
       return cartService.getCurrentCartItems();
    }
}
