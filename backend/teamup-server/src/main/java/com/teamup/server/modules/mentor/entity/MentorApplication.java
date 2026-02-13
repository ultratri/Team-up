package com.teamup.server.modules.mentor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 鐎电厧绗€閻㈠疇顕€圭偘缍?
 */
@Data
@TableName("mentor_applications")
public class MentorApplication {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 閻㈠疇顕禍铏规暏閹寸īD
     */
    private Long applicantId;
    
    /**
     * 閻喎鐤勬慨鎾虫倳
     */
    private String realName;
    
    /**
     * 瀹搞儱褰?
     */
    private String userCode;
    
    /**
     * 闂勩垻閮?
     */
    private String department;
    
    /**
     * 娑撴挷绗?
     */
    private String major;
    
    /**
     * 闁喚顔?
     */
    private String email;
    
    /**
     * 閻絻鐦?
     */
    private String phone;
    
    /**
     * 娑擃亙姹夌粻鈧禒?
     */
    private String bio;
    
    /**
     * 妞ゅ湱娲扮紒蹇涚崣
     */
    private String projectExperience;
    
    /**
     * 閹稿洤顕辩紒蹇涚崣
     */
    private String guidanceExperience;
    
    /**
     * 閻㈠疇顕悶鍡欐暠
     */
    private String applicationReason;
    
    /**
     * 閻㈠疇顕悩鑸碘偓渚婄窗PENDING-瀵板懎顓搁弽? APPROVED-瀹告煡鈧俺绻? REJECTED-瀹稿弶瀚嗙紒?
     */
    private String status;
    
    /**
     * 鐎光剝鐗虫禍绡扗
     */
    private Long reviewerId;
    
    /**
     * 鐎光剝鐗抽幇蹇氼潌
     */
    private String reviewComment;
    
    /**
     * 鐎光剝鐗抽弮鍫曟？
     */
    private LocalDateTime reviewedAt;
    
    /**
     * 閻㈠疇顕弮鍫曟？
     */
    private LocalDateTime createdAt;
    
    /**
     * 閺囧瓨鏌婇弮鍫曟？
     */
    private LocalDateTime updatedAt;
}
