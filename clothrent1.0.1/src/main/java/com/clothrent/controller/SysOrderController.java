package com.clothrent.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clothrent.common.*;
import com.clothrent.dto.PageDTO;
import com.clothrent.entity.*;
import com.clothrent.service.*;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author liuqiming
 * @since 2024-02-06
 */
@Controller
@RequestMapping({"/order","home/order"})
public class SysOrderController {

    private static  final Logger logger= LoggerFactory.getLogger(SysOrderController.class);

    @Autowired
    SysOrderService orderService;

    @Autowired
    SysUserService userService;

    @Autowired
    SysFileService fileService;

    @Autowired
    SysCartService cartService;

    @Autowired
    UserAddressService userAddressService;

    @Autowired
    SysOrderItemService orderItemService;

    @Autowired
    SysGoodsService goodsService;

    @Autowired
    JavaMailSenderImpl mailSender;

    @Autowired
    OrderMailService orderMailService;

    @Autowired
    UserAccountService userAccountService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Scheduled(cron = "0 0/15 * * * 0-7")
    public void scheduleMail(){
        List<SysOrder> sysOrderList = orderService.list(new QueryWrapper<SysOrder>().eq("state", 4));
        for(SysOrder order:sysOrderList){
            try {

                //判断是否发送过
                List<OrderMail> orderMails = orderMailService.list(new QueryWrapper<OrderMail>().eq("order_id", order.getId()));
                if(orderMails.size()>0){
                    continue;
                }
                String rentDay = order.getRentDay();
                Date limitDay = DateUtils.parseDate(rentDay, "yyyy-MM-dd");
                Date checkDate = DateUtils.addHour(new Date(), 24);
                if(checkDate.compareTo(limitDay)>0){//满足发送邮件的时间要求
                    Long userId = order.getUserId();
                    SysUser sysUser = userService.getById(userId);
                    String email = sysUser.getEmail();
                    if(!RegexUtil.checkEmail(email)){
                        logger.error("当前用户邮箱不合法！{}{}",sysUser.getName(),email);
                        continue;
                    }

                    String title="租赁到期提醒";
                    String content="尊敬的"+sysUser.getName()+"您好！您有订单租赁即将到期请及时归还！订单号："+order.getCode();
                    SimpleMailMessage message = new SimpleMailMessage();

                    message.setSubject(title);
                    message.setText(content);

                    message.setFrom(fromEmail);
                    message.setTo(email);

                    mailSender.send(message);

                    //插入发送记录表
                    OrderMail orderMail=new OrderMail();
                    orderMail.setTitle(title);
                    orderMail.setContent(content);
                    orderMail.setOrderId(order.getId());
                    orderMail.setToMail(email);
                    orderMailService.save(orderMail);

                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }




    /**从商品列表或商品详情--确认订单--生成订单
     * @param userAddressId 用户收货地址ID
     * @param goodsId 商品ID
     * @param number 商品数量
     * @return
     */
    @RequestMapping("insertOrderFromGoods")
    @ResponseBody
    public ResponseBean insertOrderFromGoods(Long userAddressId, @RequestParam("goodsId") Long goodsId, Long number,String remark,String ifSend,
                                             String clothSize,String ifhz,String ifpic,String rentDay){
        if(userAddressId==null||goodsId==null||number==null){
            return  ResultUtil.error(CommonEnum.BAD_PARAM);
        }
        // 获取收货地址数据--也就是当前订单绑定的收货地址
        UserAddress userAddress = userAddressService.getById(userAddressId);
        SysOrder order=orderService.generateOrder(goodsId,number,userAddress,remark,clothSize,ifhz,ifpic,rentDay);
        if("1".equals(ifSend)){//无需发货，直接修改订单状态为待收货
            order.setState(3);
            orderService.updateById(order);
            UpdateWrapper<SysOrderItem> orderItemUpdateWrapper=new UpdateWrapper<>();
            orderItemUpdateWrapper.eq("order_id",order.getId()).set("state",3);
            orderItemService.update(orderItemUpdateWrapper);
        }

        return ResultUtil.successData(order);
    }

    /**
     * 从购物车--确认订单--生成订单
     *   var ifSendVal=$("#ifSend").is(':checked')?1:0;
     *                 var ifhz=$("#ifhz").is(':checked')?1:0;
     *                 var ifpic=$("#ifpic").is(':checked')?1:0;
     *                 let rentDay = $("#rentDay").val();
     * @param userAddressId
     * @param cartIdList
     * @param remark
     * @param ifSend
     * @return
     */
    @RequestMapping("insertOrderFromCart")
    @ResponseBody
    public ResponseBean insertOrderFromCart(Long userAddressId,@RequestParam("cartIdList") List<Long> cartIdList,String remark,String ifSend,String ifhz,String ifpic,String rentDay){
        QueryWrapper<SysCart> queryWrapper=new QueryWrapper<>();
        queryWrapper.in("id",cartIdList);
        // 获取购物车数据
        List<SysCart> cartList = cartService.list(queryWrapper);
        // 获取收货地址数据--也就是当前订单绑定的收货地址
        UserAddress userAddress = userAddressService.getById(userAddressId);
        SysOrder order=orderService.generateOrder(cartList,userAddress,remark,ifhz, ifpic, rentDay);
        if("1".equals(ifSend)){//无需发货，直接修改订单状态为待收货
            order.setState(3);
            orderService.updateById(order);
            UpdateWrapper<SysOrderItem> orderItemUpdateWrapper=new UpdateWrapper<>();
            orderItemUpdateWrapper.eq("order_id",order.getId()).set("state",3);
            orderItemService.update(orderItemUpdateWrapper);
        }
        return ResultUtil.successData(order);
    }

    // 从商品-立即购买来，这时没有购物车
    @RequestMapping("immediateOrderPage")
    public String immediateOrderPage(@RequestParam(required = false,defaultValue = "0")Long ifPrize,@RequestParam(required = false,defaultValue = "L") String clothSize,
                                      @RequestParam Long goodsId,@RequestParam(required = false,defaultValue = "0") String ifSend,
                                     @RequestParam(required = false,defaultValue = "1") int number,
                                     @RequestParam(required = false) String rentDay,
                                     Model model){
        model.addAttribute("goodsId",goodsId);
        model.addAttribute("goodsNumber",number);
        model.addAttribute("ifSend",ifSend);
        model.addAttribute("clothSize",clothSize);
        model.addAttribute("rentDay",rentDay);
        if(StringUtils.isEmpty(rentDay)){
            Date addDay = DateUtils.addDay(new Date(), 7);
            rentDay=DateUtils.format(addDay,"yyyy-MM-dd");
        }
        Date endDate = DateUtils.parseDate(rentDay, "yyyy-MM-dd");
        int rentDayNum = DateUtils.diffDateReDay(endDate, new Date())+1;
        model.addAttribute("rentDayNum",rentDayNum);
        model.addAttribute("ifPrize",ifPrize);//是否为抽奖所得
        return "home/order/immediateOrder";
    }


    // 跳到订单确认页面
    @RequestMapping("commitPage")
    public String commitPage(Long userId,HttpSession session,Model model,@RequestParam("cartIdList")List<Long> cartIdList,String ifSend){
        logger.debug("cartIdList:{},userId：{}",userId);
        model.addAttribute("ifSend","true".equals(ifSend)?1:0);
        if(userId==null&& ToolsUtils.getLoginUser(session)==null){
            return "home/login";
        }
        // 数据处理
        // 商品全部未选中
        UpdateWrapper<SysCart> updateWrapper=new UpdateWrapper<>();
        updateWrapper.eq("user_id",userId).set("field0","0");// 1表示选中 0 表示未选中
        cartService.update(updateWrapper);
        // 更新 cartIdList中的为已选中的
        updateWrapper=new UpdateWrapper<>();
        updateWrapper.eq("user_id",userId).in(cartIdList!=null&&!cartIdList.isEmpty(),"id",cartIdList).set("field0","1");// 1表示选中 0 表示未选中
        cartService.update(updateWrapper);

        return "home/order/commit";

    }

    /**
     * 跳转前台我的订单列表页面
     * @return
     */
    @RequestMapping("/homeListPage")
    public String homeListPage() {
        return "home/order/list";
    }

    /**
     * 跳转列表页面
     * @return
     */
    @RequestMapping("/listPage")
    public String orderListPage() {
        return "admin/order/list";
    }

    /**
     * 跳转订单驳回页面
     * @return
     */
    @RequestMapping("/cancelPage/{id}")
    public String cancelPage(@PathVariable Long id,Model model) {
        SysOrder sysOrder = orderService.getById(id);
        model.addAttribute("order",sysOrder);
        return "admin/order/cancel";
    }

    /**
     * 订单驳回更新驳回缘由
     * @return
     */
    @RequestMapping("/cancel")
    @ResponseBody
    public ResponseBean cancel(SysOrder order) {
        if(StringUtils.isEmpty(order.getRefuseDesc())){
            return ResultUtil.error(CommonEnum.BAD_PARAM);
        }
        SysOrder sysOrder = orderService.getById(order.getId());
        Integer oldState = sysOrder.getState();// 状态：0 待支付；1 已支付；2 已确认；3 已发货 ；4 已收货；5 申请归还；6 已归还；7 归还驳回；',
        if(oldState==5){
            sysOrder.setState(7);// 修改为归还驳回
        }
        sysOrder.setRefuseDesc(order.getRefuseDesc());

        sysOrder.setUpdateTime(LocalDateTime.now());
        orderService.updateById(sysOrder);
        return ResultUtil.success();
    }


    // 跳到申请归还退押金页面
    @RequestMapping("applyCancelPage/{id}")
    public String applyCancelPage(Model model,@PathVariable Long id){
        model.addAttribute("order",orderService.getById(id));
        return "home/order/applyCancel";
    }

    // 归还退押金
    @RequestMapping("/applyCancel")
    @ResponseBody
    public ResponseBean applyCancel(SysOrder order) {
        if(StringUtils.isEmpty(order.getField0())){
            return ResultUtil.error(CommonEnum.BAD_PARAM);
        }
        SysOrder sysOrder = orderService.getById(order.getId());
        sysOrder.setState(5);
        sysOrder.setField0(order.getField0());

        sysOrder.setUpdateTime(LocalDateTime.now());
        orderService.updateById(sysOrder);
        return ResultUtil.success();
    }

    /**
     * 跳转订单发货页面
     * @return
     */
    @RequestMapping("/sendPage/{id}")
    public String sendPage(@PathVariable Long id,Model model) {
        SysOrder sysOrder = orderService.getById(id);
        model.addAttribute("order",sysOrder);
        return "admin/order/send";
    }

    /**
     * 订单发货保存物流单号
     * @return
     */
    @RequestMapping("/send")
    @ResponseBody
    public ResponseBean send(SysOrder order) {
        if(StringUtils.isEmpty(order.getExpressNum())){
            return ResultUtil.error(CommonEnum.BAD_PARAM);
        }
        SysOrder sysOrder = orderService.getById(order.getId());
        sysOrder.setState(3);// 修改为已发货状态
        sysOrder.setExpressNum(order.getExpressNum());
        sysOrder.setUpdateTime(LocalDateTime.now());
        orderService.updateById(sysOrder);
        return ResultUtil.success();
    }

    /**
     * 订单修改状态
     * @return
     */
    @RequestMapping("/updateState")
    @ResponseBody
    public ResponseBean updateState(SysOrder order) {
        SysOrder sysOrder = orderService.getById(order.getId());
        Integer oldState = sysOrder.getState();// 状态：0 待支付；1 已支付；2 已确认；3 已发货 ；4 已收货；5 申请归还；6 已归还；7 归还驳回；',
        Integer newState = order.getState();
        sysOrder.setState(newState);
//        if(newState==2){
//            switch (oldState){
//                case 1:sysOrder.setState(2);break;
//                case 5:sysOrder.setState(6);break;
//            }
//        }

        sysOrder.setUpdateTime(LocalDateTime.now());
        orderService.updateById(sysOrder);
        if(sysOrder.getState()==6){//归还退押金
            LocalDateTime createTime = sysOrder.getCreateTime();
            String rentDay = sysOrder.getRentDay();
            List<SysOrderItem> itemList = orderItemService.list(new QueryWrapper<SysOrderItem>().eq("order_id", sysOrder.getId()));
            //租赁天数
            int rentNum = DateUtils.diffDateReDay(DateUtils.localFormat(createTime), DateUtils.parseDate(rentDay));
            BigDecimal price = sysOrder.getPrice();
            BigDecimal firstDayMoney = price.subtract(new BigDecimal(10*itemList.size()).multiply(new BigDecimal(rentNum - 1)));
            BigDecimal changeNum = firstDayMoney.multiply(new BigDecimal(70)).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            QueryWrapper<UserAccount> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("user_id",sysOrder.getUserId())
                    .orderBy(true,false,"id");
            List<UserAccount> accountList = userAccountService.list(queryWrapper);
            BigDecimal beforeNum=new BigDecimal(0);
            if(accountList.size()>0){
                UserAccount userAccount = accountList.get(0);
                beforeNum=userAccount.getAmount();
            }

            UserAccount userAccount=new UserAccount();
            userAccount.setBeforeNum(beforeNum);
            userAccount.setUserId(sysOrder.getUserId());
            userAccount.setOrderNum(sysOrder.getCode());
            userAccount.setOrderId(sysOrder.getId());
            userAccount.setUserName(sysOrder.getUserName());
            userAccount.setChangeNum(changeNum);
            userAccount.setAmount(beforeNum.add(changeNum));
            userAccount.setRemark("用户归还汉服退押金金额："+changeNum.doubleValue());
            userAccountService.save(userAccount);

            SysUser user = userService.getById(sysOrder.getUserId());
            user.setAccount(userAccount.getAmount().floatValue());
            userService.updateById(user);


        }
        return ResultUtil.success();
    }

    /**
     * 分页列表查询
     * @param queryParam 查询参数
     * @return
     */
    @ApiOperation(value = "查询订单列表")
    @RequestMapping(value = "/list")
    @ResponseBody
    public ResponseBean getList(PageDTO pageDTO, SysOrder queryParam , HttpSession session) {
        logger.debug("查询订单列表参数："+queryParam+",pageDTO:"+pageDTO);
        QueryWrapper<SysOrder> queryWrapper=new QueryWrapper<>();
        queryWrapper.le(!StringUtils.isEmpty(pageDTO.getEndTime()),"create_time",pageDTO.getEndTime());
        queryWrapper.ge(!StringUtils.isEmpty(pageDTO.getBeginTime()),"create_time",pageDTO.getBeginTime());
        queryWrapper.eq(queryParam.getState()!=null,"state",queryParam.getState());
        queryWrapper.eq(!StringUtils.isEmpty(queryParam.getCode()),"code",queryParam.getCode());
        queryWrapper.eq(!StringUtils.isEmpty(queryParam.getUserId()),"user_id",queryParam.getUserId());
        queryWrapper.like(!StringUtils.isEmpty(queryParam.getUserName()),"user_name",queryParam.getUserName());
        queryWrapper.orderBy(true,pageDTO.getIsAsc().equals("asc"), ToolsUtils.camelToUnderline(pageDTO.getOrderByColumn()));
        IPage<SysOrder> indexPage = new Page<>(pageDTO.getPageNum(),pageDTO.getPageSize());
        IPage<SysOrder> listPage = orderService.page(indexPage, queryWrapper);
        logger.debug("获取的订单列表："+listPage);
        List<SysOrder> records = listPage.getRecords();
        records.forEach(e->{
            QueryWrapper<SysOrderItem> itemQueryWrapper=new QueryWrapper<>();
            itemQueryWrapper.eq("order_id",e.getId());
            e.setChildList(orderItemService.list(itemQueryWrapper));
        });
        //获取分页信息
        Map pageInfo = ToolsUtils.wrapperPageInfo(listPage);
        return ResultUtil.successData(pageInfo);
    }

    /**
     * 订单删除
     * @param idList
     * @return
     */
    @RequestMapping("delete")
    @ResponseBody
    public ResponseBean delete(@RequestParam("idList") List<Long> idList){
        if(idList==null||idList.isEmpty()){
            return ResultUtil.error("ID不合法！");
        }
        orderService.removeByIds(idList);
        // 删除订单对应的订单明细
        QueryWrapper<SysOrderItem> queryWrapper=new QueryWrapper<>();
        queryWrapper.in("order_id",idList);
        orderItemService.remove(queryWrapper);
        return ResultUtil.success();
    }


}
