package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ServiceLocalServiceImpl implements ISearchService {

    @DubboReference // Dubbo消费模块
    private IForFrontSpuService dubboSpuService;
    @Autowired
    private SpuForElasticRepository spuRepository;
    // 利用Dubbo从product模块中分页查询所有spu数据,并新增到ES的操作
    @Override
    public void loadSpuByPage() {
        // 先要查询一次,才能知道分页信息,制定循环条件,是典型的先运行后判断
        // 所以推荐大家使用do-while循环
        int i=1;     //循环次数,也是页码
        int pages=0; // 总页数初始值为0即可
        do{
            // Dubbo查询当前页spu信息
            JsonPage<Spu> spus=dubboSpuService.getSpuByPage(i,2);
            // 实例化一个SpuForElastic泛型的集合,以备spus集合中的数据转换后赋值添加
            List<SpuForElastic> esSpus=new ArrayList<>();
            // 遍历从数据库中查询出的spus
            for(Spu spu:spus.getList()){
                // 实例化一个ES的实体类
                SpuForElastic esSpu=new SpuForElastic();
                // 将spu对象的同名属性赋值给esSpu
                BeanUtils.copyProperties(spu,esSpu);
                // 将赋好值的esSpu对象,新增到esSpus集合中
                esSpus.add(esSpu);
            }
            // 利用SpringData框架提供的批量新增方法执行新增操作
            spuRepository.saveAll(esSpus);
            i++;
            pages=spus.getTotalPage();
            log.info("成功新增第{}页数据",i);
        }while (i<=pages);// 循环条件是当前页码不超过总页数

    }

    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {
        return null;
    }
}