package com.reggie.reg.dto;

import com.reggie.reg.dto.AuditLogVO;
import com.reggie.reg.dto.ResourceDetailVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核日志详情 - 视图对象（用于详情页完整展示）
 * 比 AuditLogVO 包含更深层的关联信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogDetailVO extends AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 继承 AuditLogVO 的所有字段 ==========
    // 审核记录基础信息 + 内容摘要 + 提交者/审核人简化信息

    // ========== 扩展字段：完整的内容详情（三选一）==========

    /**
     * 课程资源完整详情（当 targetType = "RESOURCE" 时有值）
     */
    private ResourceDetailVO resourceDetail;

    /**
     * 问答互动完整详情（当 targetType = "QA" 时有值）
     */
    private QaDetailVO qaDetail;

    /**
     * 作业提交完整详情（当 targetType = "SUBMISSION" 时有值）
     */
    private SubmissionDetailVO submissionDetail;

    // ========== 扩展字段：完整的内容提交者信息 ==========

    /**
     * 提交者完整信息（用于详情页显示更多用户信息）
     */
    private UserDetailVO submitterDetail;

    /**
     * 审核人完整信息
     */
    private UserDetailVO auditorDetail;

    // ========== 扩展字段：审核操作历史 ==========

    /**
     * 审核操作记录（如果有多次审核）
     * 示例：首次提交→拒绝→修改→重新提交→通过
     */
    private List<AuditOperationVO> operationHistory;

    // ========== 扩展字段：辅助展示信息 ==========

    /**
     * 内容完整预览（用于富文本/代码高亮等）
     */
    private String fullContent;

    /**
     * 文件预览信息（如果是文件类型）
     */
    private FilePreviewVO filePreview;

    /**
     * 关联的课程完整信息（用于跳转课程详情）
     */
    private CourseSimpleVO courseInfo;
}