package spring.ai.chat.infrastructure.adapter.repository;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.ai.chat.domain.trade.adapter.repository.TradeRepository;
import spring.ai.chat.domain.trade.model.entity.GoodsDetailEntity;
import spring.ai.chat.domain.trade.model.entity.GoodsEntity;
import spring.ai.chat.infrastructure.dao.GoodsDao;
import spring.ai.chat.infrastructure.dao.po.Goods;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TradeRepositoryImpl implements TradeRepository {

    @Resource
    private GoodsDao goodsDao;

    @Override
    public List<GoodsEntity> getGoodsList() {
        List<Goods> goodsList = goodsDao.getGoodsList();
        if (goodsList == null) {
            return null;
        }
        List<GoodsEntity> goodsEntityList = new ArrayList<>();
        for (Goods goods : goodsList) {
            goodsEntityList.add(GoodsEntity.builder()
                    .goodsId(goods.getGoodsId())
                    .goodsName(goods.getGoodsName())
                    .goodsPrice(goods.getGoodsPrice())
                    .build());
        }
        return goodsEntityList;
    }

    @Override
    public GoodsDetailEntity getGoodsByID(String goodsId) {
        Goods goods = goodsDao.getGoodsByID(goodsId);
        if (goods == null) {
            return null;
        }
        return GoodsDetailEntity.builder()
                .goodsId(goodsId)
                .goodsName(goods.getGoodsName())
                .goodsDesc(goods.getGoodsDesc())
                .goodsPrice(goods.getGoodsPrice())
                .build();
    }
}
