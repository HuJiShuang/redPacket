package edu.bit.hjs.serviceImpl;

import edu.bit.hjs.dao.RedPacketDao;
import edu.bit.hjs.dao.UserRedPacketDao;
import edu.bit.hjs.entity.RedPacket;
import edu.bit.hjs.entity.UserRedPacket;
import edu.bit.hjs.service.RedisRedPacketService;
import edu.bit.hjs.service.UserRedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;


@Service
public class UserRedPacketServiceImpl implements UserRedPacketService {

    @Autowired
    private UserRedPacketDao userRedPacketDao = null;

    @Autowired
    private RedPacketDao redPacketDao = null;

    //失败
    private static final int FAILED = 0;

    @Override
    //配置了事务注解@Transactional,让程序能够在事务中运行，以保证数据的一致性，
    // 这里采用的是读/写提交的隔离级别，之所以不采用更高的事务，主要是提高数据库的并发能力，
    //而对于传播行为则采用Propagation.REQUIRED，这样调用这个方法的时候，如果没有事务则会创建事务，
    //如果有事务则会沿用当前事务
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int grapRedPacket(Long redPacketId, Long userId) {

        //1.普通获取红包信息
        //RedPacket redPacket = redPacketDao.getRedPacket(redPacketId);
        //2.使用悲观锁获取红包信息
        //RedPacket redPacket = redPacketDao.getRedPacketForUpadte(redPacketId);
        //3.乐观锁获取红包信息（和普通获取一样）
        RedPacket redPacket = redPacketDao.getRedPacket(redPacketId);
        //当前小红包库存大于0
        if (redPacket.getStock() > 0){
            //再次传入线程保存的version旧值给SQL判断，是否有其他线程修改过数据
            redPacketDao.decreaseRedPacket(redPacketId);
            //生成强红包i信息
            UserRedPacket userRedPacket = new UserRedPacket();
            userRedPacket.setRedPacketId(redPacketId).setUserId(userId).setAmount(redPacket.getUnitAmount()).setNote("强红包"+redPacketId);
            //插入抢红包信息
            int result = userRedPacketDao.grapRedPacket(userRedPacket);
            return result;
        }
        //失败返回
        return FAILED;
    }


    @Override
    //配置了事务注解@Transactional,让程序能够在事务中运行，以保证数据的一致性，
    // 这里采用的是读/写提交的隔离级别，之所以不采用更高的事务，主要是提高数据库的并发能力，
    //而对于传播行为则采用Propagation.REQUIRED，这样调用这个方法的时候，如果没有事务则会创建事务，
    //如果有事务则会沿用当前事务
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int grapRedPacketForVersion(Long redPacketId, Long userId) {

        //使用乐观锁重入机制
        //记录开始时间
        long start = System.currentTimeMillis();
        //无限循环，等待成功或者时间满100毫秒退出
        while (true) {
            //获取循环当前时间
            long end = System.currentTimeMillis();
            if(end-start > 50)
                return FAILED;
            //3.乐观锁获取红包信息（和普通获取一样）
            RedPacket redPacket = redPacketDao.getRedPacket(redPacketId);
            //当前小红包库存大于0
            if (redPacket.getStock() > 0) {
                //再次传入线程保存的version旧值给SQL判断，是否有其他线程修改过数据
                int update = redPacketDao.decreaseRedPacketForVersion(redPacketId, redPacket.getVersion());
                if (update == 0) {
                    continue;
                }
                //生成强红包i信息
                UserRedPacket userRedPacket = new UserRedPacket();
                userRedPacket.setRedPacketId(redPacketId).setUserId(userId).setAmount(redPacket.getUnitAmount()).setNote("强红包" + redPacketId);
                //插入抢红包信息
                int result = userRedPacketDao.grapRedPacket(userRedPacket);
                return result;
            }
            //一旦发现没有库存则立即返回
            else
                //失败返回
                return FAILED;
        }
    }

    /**
     * 通过Redis实现抢红包
     *
     * @param redPacketId 红包编号
     * @param userId      抢红包用户编号
     * @return 0-没有库存,失败
     * 1--成功,且不是最后一个红包
     * 2--成功，且是最后一个红包
     */
    @Autowired
    private RedisTemplate redisTemplate = null;

    @Autowired
    private RedisRedPacketService redisRedPacketService = null;

    //Lua脚本
    String script = "local listKey = 'red_packet_list_'..KEYS[1] \n"
            + "local redPacket = 'red_packet_'..KEYS[1] \n"
            + "local stock = tonumber(redis.call('hget', redPacket, 'stock')) \n"
            + "if stock <= 0 then return 0 end \n"
            + "stock = stock - 1 \n"
            + "redis.call('hset', redPacket, 'stock', tostring(stock)) \n"
            + "redis.call('rpush', listKey, ARGV[1]) \n"
            + "if stock == 0 then return 2 end \n"
            + "return 1 \n";
    //在缓存Lua脚本后,使用该变量保存Redis返回的32位的SHA1编码,使用它去执行缓存的Lua脚本
    String sha1 = null;

    @Override
    public Long grapRedPacketByRedis(Long redPacketId, Long userId) {
        //当前抢红包用户和日期信息
        String args = userId + "-" + System.currentTimeMillis();
        Long result = null;
        //获取底层Redis操作对象
        Jedis jedis = (Jedis) redisTemplate.getConnectionFactory().getConnection().getNativeConnection();
        try {
            //如果脚本没有加载过，那么进行加载，这样就会返回一个sha1编码
            if (sha1 == null) {
                sha1 = jedis.scriptLoad(script);
            }
            //执行脚本返回结果
            Object res = jedis.evalsha(sha1, 1, redPacketId + "", args);
            result = (Long) res;
            //返回2时为最后一个红包,此时将红包信息通过异步保存到数据库中
            if (result == 2) {
                //获取单个小红包数额
                String unitAmountStr = jedis.hget("red_packet_" + redPacketId, "unit_amount");
                //触发保存数据库操作
                Double unitAmount = Double.parseDouble(unitAmountStr);
                System.err.println("thred_name =" + Thread.currentThread().getName());
                redisRedPacketService.saveUserRedPacketByRedis(redPacketId, unitAmount);
                System.out.println("======================================");
            }
        }finally {
            //确保jedis顺利关闭
            if (jedis != null && jedis.isConnected())
                jedis.close();
        }
        return result;
    }
}
