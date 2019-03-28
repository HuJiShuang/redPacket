package edu.bit.hjs.service;

import edu.bit.hjs.entity.RedPacket;

import java.util.Map;

public interface RedPacketService {

    /**
     * 获取红包
     * @param id 编号
     * @return 红包信息
     */
    RedPacket getRedPacket(Long id);

    /**
     * 扣减红包
     * @param id 编号
     * @return 影响条数
     */
    int decreaseRedPacket(Long id);

    /**
     * 使用数据库自带的悲观锁来解决超发现象
     * 获取红包信息
     * @param  id 红包id
     * @return 红包具体信息
     */
    RedPacket getRedPacketForUpadte (Long id);

    /**
     * 使用乐观锁扣减抢红包数
     * @param id  编号
     * @param version 版本号
     * @return 更新记录条数
     */
    int decreaseRedPacketForVersion(Long id , Integer version);
}
