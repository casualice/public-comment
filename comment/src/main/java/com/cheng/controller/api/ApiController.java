package com.cheng.controller.api;

import com.cheng.constant.ApiCodeEnum;
import com.cheng.dto.*;
import com.cheng.service.BusinessService;
import com.cheng.service.impl.AdServiceImpl;
import com.cheng.service.impl.MemberServiceImpl;
import com.cheng.util.CommonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cheng on 2017/7/22.
 * 大众点评api控制层
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private AdServiceImpl adService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private MemberServiceImpl memberService;

    @Value("${ad.number}")
    private int adNumber;

    @Value("${businessSearch.number}")
    private int businessSearchNumber;

    @Value("${businessHome.number}")
    private int businessHomeNumber;

    /**
     * 首页 —— 广告（超值特惠）
     */
    @RequestMapping(value = "/homead", method = RequestMethod.GET)
    public List<AdDto> homead() throws IOException {
        AdDto adDto = new AdDto();
        adDto.getPage().setPageSize(adNumber);
        return adService.searchByPage(adDto).getList();
    }

    /**
     * 首页 —— 推荐列表（猜你喜欢）
     */
    @RequestMapping(value = "/homelist/{city}/{page.pageNum}", method = RequestMethod.GET)
    public BusinessListDto homelist(BusinessDto businessDto) {
        businessDto.getPage().setPageSize(businessHomeNumber);
        return businessService.searchByPageForApi(businessDto);
    }

    /**
     * 搜索结果页 - 三个参数
     */
    @RequestMapping(value = "/search/{page.pageNum}/{city}/{category}/{keyword}", method = RequestMethod.GET)
    public BusinessListDto searchByKeyword(BusinessDto businessDto) throws IOException {
        businessDto.getPage().setPageNum(businessSearchNumber);
        return businessService.searchByPageForApi(businessDto);
    }

    /**
     * 搜索结果页 - 两个参数
     */
    @RequestMapping(value = "/search/{page.pageNum}/{city}/{category}", method = RequestMethod.GET)
    public BusinessListDto search(BusinessDto businessDto) {
        businessDto.getPage().setPageNum(businessSearchNumber);
        return businessService.searchByPageForApi(businessDto);
    }

    /**
     * 详情页 - 商户信息
     */
    @RequestMapping(value = "/detail/info/{id}", method = RequestMethod.GET)
    public BusinessDto detail(@PathVariable("id") long id) {
        return businessService.getById(id);
    }

    /**
     * 详情页 - 用户评论
     */
    @RequestMapping(value = "/detail/comment/{page}/{id}", method = RequestMethod.GET)
    public CommentListDto detail() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String info = "";
        return mapper.readValue(info, new TypeReference<CommentListDto>() {
        });
    }

    /**
     * 订单列表
     */
    @RequestMapping(value = "/orderlist/{username}", method = RequestMethod.GET)
    public List<OrdersDto> orderList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String info = "";
        return mapper.readValue(info, new TypeReference<List<OrdersDto>>() {
        });
    }

    /**
     * 提交评论页
     */
    @RequestMapping(value = "/submitcomment", method = RequestMethod.GET)
    public Map<String, Object> submitComment() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("msg", "ok");
        return result;
    }

    /**
     * 根据手机号下发短信验证码
     */
    @RequestMapping(value = "/sms", method = RequestMethod.POST)
    public ApiCodeDto sms(@RequestParam("username") Long username) {
        ApiCodeDto dto;
        String code = null;
        //1.验证用户手机号是否存在(是否注册过)
        if (memberService.exists(username)) {
            //2.生成6位随机数
            code = String.valueOf(CommonUtil.random(6));
            //3.保存手机号与对应的md5(6位随机数)(一般保存1分钟，1分钟后失效)
            if (memberService.saveCode(username, code)) {
                //4.调用短信通道，将明文6位随机数字发送到对应的手机上
                if (memberService.sendCode(username, code)) {
                    dto = new ApiCodeDto(ApiCodeEnum.SUCCESS, code);
                } else {
                    dto = new ApiCodeDto(ApiCodeEnum.SEND_FAIL);
                }
            } else {
                dto = new ApiCodeDto(ApiCodeEnum.SEND_FAIL);
            }
        } else {
            dto = new ApiCodeDto(ApiCodeEnum.USER_NOT_EXISTS);
        }
        return dto;
    }

    /**
     * 会员登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ApiCodeDto login(@RequestParam("username") Long username, @RequestParam("code") String code) {
        ApiCodeDto dto = null;
        //1.用手机号取出保存的md5(6位随机数)，能取到，并且提交上来的code值相同为校验通过
        String saveCode = memberService.getCode(username);
        if (saveCode != null) {
            if (saveCode.equals(code)) {
                //2.如果校验通过，生成一个32位的token
                String token = CommonUtil.getUUID();
                //3.保存手机号与对应的token(一般这个手机号中途没有与服务器交互的情况下，保持10分钟)
                memberService.saveToken(token, username);
                //4.将这个token返回给前端
                dto = new ApiCodeDto(ApiCodeEnum.SUCCESS, code);
                dto.setToken(token);
            } else {
                dto = new ApiCodeDto(ApiCodeEnum.CODE_ERROR, code);
            }
        } else {
            dto = new ApiCodeDto(ApiCodeEnum.CODE_INVALID, code);
        }
        return dto;
    }
}