package com.zitao.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    /**
     * 工作单的id
     */
    private Long id;
    /**
     * 工作单详情
     */
    private StockDetailTo detailTo;
}
