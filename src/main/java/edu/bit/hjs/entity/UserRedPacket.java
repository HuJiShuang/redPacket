package edu.bit.hjs.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 关于抢红包信息的类
 */
//使用lombok相关注解自动生成实体类中想要的方法
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)      //链式风格访问
public class UserRedPacket implements Serializable {
    private static final long serialVersionUID = -5529736592434968114L;            //实现Serializable接口,这样便可序列化对象

    private Long id;
    private Long redPacketId;
    private Long userId;
    private Double amount;
    private Timestamp grabTime;
    private String note;

}
