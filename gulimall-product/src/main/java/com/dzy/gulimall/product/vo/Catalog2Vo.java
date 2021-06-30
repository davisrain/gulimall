package com.dzy.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catalog2Vo {

    private String catalog1Id;
    private String id;
    private String name;
    private List<Catalog3Vo> catalog3List;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catalog3Vo {
        private String catalog2Id;
        private String id;
        private String name;
    }
}
