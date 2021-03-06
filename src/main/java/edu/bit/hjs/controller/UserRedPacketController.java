package edu.bit.hjs.controller;

import edu.bit.hjs.service.UserRedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/userRedPacket")
public class UserRedPacketController {

    @Autowired
    private UserRedPacketService userRedPacketService = null;

    @RequestMapping(value = "/grapRedPacket")
    @ResponseBody
    public Map<String,Object> grapRedPacket(Long redPacketId, Long userId){

        //抢红包
        int result = userRedPacketService.grapRedPacket(redPacketId,userId);
        Map<String, Object> retMap = new HashMap<>();
        boolean flag = result > 0;
        retMap.put("success", flag);
        retMap.put("message",flag?"抢红包成功":"抢红包失败");
        System.out.println(retMap);
        return retMap;
    }

    @RequestMapping(value = "/grapRedPacketForVersion")
    @ResponseBody
    public Map<String,Object> grapRedPacketForVersion(Long redPacketId, Long userId){

        //抢红包
        int result = userRedPacketService.grapRedPacketForVersion(redPacketId,userId);
        Map<String, Object> retMap = new HashMap<>();
        boolean flag = result > 0;
        retMap.put("success", flag);
        retMap.put("message",flag?"抢红包成功":"抢红包失败");
        System.out.println(retMap);
        return retMap;
    }

    @RequestMapping(value = "/grapRedPacketByRedis")
    @ResponseBody
    public Map<String,Object> grapRedPacketByRedis(Long redPacketId, Long userId){

        //redis抢红包
        Long result = userRedPacketService.grapRedPacketByRedis(redPacketId,userId);
        Map<String, Object> retMap = new HashMap<>();
        boolean flag = result > 0;
        retMap.put("success", flag);
        retMap.put("message",flag?"抢红包成功":"抢红包失败");
        System.out.println(retMap);
        return retMap;
    }

}
