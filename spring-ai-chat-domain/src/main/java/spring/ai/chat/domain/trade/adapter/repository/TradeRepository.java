package spring.ai.chat.domain.trade.adapter.repository;

import spring.ai.chat.domain.trade.model.entity.GoodsDetailEntity;
import spring.ai.chat.domain.trade.model.entity.GoodsEntity;

import java.util.List;

public interface TradeRepository {

    /**
     * 获取商品集合
     * @return
     */
    List<GoodsEntity> getGoodsList();

    /**
     * 根据商品ID查询商品信息
     * @return
     */
    GoodsDetailEntity getGoodsByID(String goodsId);
}
