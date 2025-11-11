package spring.ai.chat.infrastructure.tools;

import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import spring.ai.chat.infrastructure.dao.GoodsDao;
import spring.ai.chat.infrastructure.dao.po.Goods;

import java.util.List;

@Service
public class GoodsTool {

    @Resource
    private GoodsDao goodsDao;

    @Tool(description = "根据商品ID获取商品")
    public Goods getGoodByGoodsId(@ToolParam(description = "商品ID") String goodsId) {
        return goodsDao.getGoodsByID(goodsId);
    }

    @Tool(description = "获取商品列表")
    public List<Goods> getGoodsList() {
        return goodsDao.getGoodsList();
    }

}
