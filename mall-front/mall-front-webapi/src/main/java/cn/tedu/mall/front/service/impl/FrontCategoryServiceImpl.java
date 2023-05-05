package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    //front模块需要dubbo调用product模块的方法，实现查询所有分类信息的功能
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    //方法要将查询到的分类信息保存到Redis，所以需要操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    //开发时，使用Redis定义一个常量，作为Key的名称，防止编码时拼写错误
    public static final String CATEGORY_TREE_KEY = "category_tree";

    //返回三级分类树对象
    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 方法开始，先检查redis中是否已经包含了这个key
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)) {
            //通过这个get（）取到键
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO = (FrontCategoryTreeVO<FrontCategoryEntity>)
                    redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            //取到了就别忘了返回
            return treeVO;
        }
        //代码运行到此处，表示Redis中没有三级分类信息，所以要从数据库查询分类对象
        //然后构建三级分类树
        //利用dubbo查询所有分类信息
        List<CategoryStandardVO> categoryList = dubboCategoryService.getCategoryList();
        //编写一个方法，将categoryList转换为三级分类树结构对象
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = initTree(categoryList);

        return null;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryList) {
        // 现在方法参数categoryList泛型是CategoryStandardVO类型,不包含children属性
        // 所以无法设置分类之间的父子关系,所以要使用FrontCategoryEntity类做返回值
        // FrontCategoryEntity类中包含children属性,能够设置当前分类对象的子分类对象

        // 下面要开始实施构建三级分类树的代码
        // 第一步:确定分类对象对应的父分类id,将所有相同父分类id的分类对象归纳在一起
        // 创建一个map,map中使用父分类id作为key,遍历当前参数集合,将相同父分类id的对象存在一个key下
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("准备构建三级分类树，分类元素总数:{}", categoryList.size());
        //遍历数据库中查询出来的所有分类对象
        for (CategoryStandardVO categoryStandardVO : categoryList) {
            // 遍历集合中的元素,将元素categoryStandardVO,转换为FrontCategoryEntity类型
            // 因为后面构建三级分类关系时,必须包含children属性
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
            // 因为后面会多次用到当前对象的父分类id,所以先取出来
            Long parentId = frontCategoryEntity.getParentId();
            //判断当前map中是否已经存在这个父分类id的key
            if (map.containsKey(parentId)) {
                // 如果已经存在,直接将当前对象添加到这个key对应的value中的list即可
                map.get(parentId).add(frontCategoryEntity);
            } else {
                // 如果当前map中还没有这个父分类id的key,就要创建新的key-value元素
                // 要先把value准备好,既实例化List,并将分类对象,添加到集合中
                List<FrontCategoryEntity> value = new ArrayList<>();
                value.add(frontCategoryEntity);
                //然后在map中新增元素
                map.put(parentId, value);
            }
        }
        // 第二步: 构建三级分类树,将子分类集合添加到对应的父分类对象的children属性中
        // 从一级分类开始,我们程序设计父分类id为0的就是一级分类
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        // 判断firstLevels集合是否为null(或元素个数为0)
        if (firstLevels == null || firstLevels.isEmpty()){
            // 如果没有一级分类对象,就抛出异常,终止程序
            throw new CoolSharkServiceException(
                    ResponseCode.INTERNAL_SERVER_ERROR, "没有一级分类!"
            );
        }
        // 遍历firstLevels集合
        for (FrontCategoryEntity oneLevel : firstLevels){
            // 一级分类对象的id,就是二级分类对象的父分类id
            Long secondLevelParentId = oneLevel.getId();
            // 根据二级分类的父分类id,获取当前分类对象包含的二级分类集合
            List<FrontCategoryEntity> secondLevels = map.get(secondLevelParentId);
            // 判断二级分类是否为null
            if (secondLevels == null || secondLevels.isEmpty()){
                // 二级分了集合如果为null,不用抛异常,只需在日志中输出警告即可
                log.warn("{}号分类对象的二级分类没有内容",secondLevelParentId);
                // 当前二级分类没有内容,无需循环之后的语句,直接运行下次循环:使用continue
                continue;
            }
            // 二级分类有元素,就遍历二级分类对象
            for (FrontCategoryEntity twoLevel : secondLevels){
                // 获取当前二级分类对象的id,作为三级分类的父id
                Long thirdParentId = twoLevel.getId();
                // 根据三级分类的父id获取三级分类集合
                List<FrontCategoryEntity> thirdLevels = map.get(thirdParentId);
                // 同样判断三级分类是否为null
                if (thirdLevels == null || thirdLevels.isEmpty()){
                    log.warn("{}号分类对象没有三级分类内容",thirdParentId);
                    continue;
                }
                // 将包含内容的三级分类集合,赋值到二级分类对象的children属性中
                twoLevel.setChildrens(thirdLevels);
            }
            // 内层循环结束,secondLevels中的每个元素,就都包含它的所有子分类对象了
            // 下面就要将二级分类集合赋值到一级分类对象中了
            oneLevel.setChildrens(secondLevels);
        }
        // 循环结束后,我们所有的分类对象都包含自己对应的子分类集合了
        // 最后需要实例化FrontCategoryTreeVO对象进行赋值和返回
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = new FrontCategoryTreeVO<>();
        treeVO.setCategories(firstLevels);
        // 最后别忘了返回treeVO !!!
        return treeVO;
    }
}
