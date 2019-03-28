package edu.bit.hjs.dao;

import edu.bit.hjs.entity.UserRedPacket;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserRedPacketDao {
    /**
     * 插入强红包信息
     * @param userRedPacketDao
     * @return 影响记录数
     */

    public int grapRedPacket(UserRedPacket userRedPacket);
}
