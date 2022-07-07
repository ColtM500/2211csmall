package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

@RestController
@RequestMapping("/seckill/spu")
@Api(tags = "秒杀Spu模块")
public class SeckillSpuController {
    @Autowired
    private ISeckillSpuService seckillSpuService;

    @GetMapping("/list")
    @ApiOperation("分页查询秒杀列表Spu信息")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name="page",required = true,dataType = "int"),
            @ApiImplicitParam(value = "每页条数",name="pageSize",required = true
                                                                    ,dataType = "int")
    })
    // 查询秒杀列表不需要登录
    public JsonResult<JsonPage<SeckillSpuVO>> listSeckillSpus(
                                        Integer page,Integer pageSize){
        JsonPage<SeckillSpuVO> list=seckillSpuService.listSeckillSpus(page,pageSize);
        return JsonResult.ok(list);
    }

    @GetMapping("/{spuId}/detail")
    @ApiOperation("根据SpuId查询Detail详情")
    @ApiImplicitParam(value = "spuId", name="spuId",required = true,
            dataType = "long",example = "1")
    public JsonResult<SeckillSpuDetailSimpleVO> getSeckillDetail(
            @PathVariable Long spuId){
        SeckillSpuDetailSimpleVO detailSimpleVO=seckillSpuService
                .getSeckillSpuDetail(spuId);
        return JsonResult.ok(detailSimpleVO);
    }


    @GetMapping("/{spuId}")
    @ApiOperation("根据SpuId查询秒杀Spu详情")
    @ApiImplicitParam(value = "spuId",name="spuId",required = true,
                        dataType = "long",example = "2")
    public JsonResult<SeckillSpuVO> getSeckillSpuVO(
            @PathVariable Long spuId){
        SeckillSpuVO seckillSpuVO=seckillSpuService.getSeckillSpu(spuId);
        return JsonResult.ok(seckillSpuVO);
    }






}
