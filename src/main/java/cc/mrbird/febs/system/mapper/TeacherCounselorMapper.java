package cc.mrbird.febs.system.mapper;

import cc.mrbird.febs.system.entity.TeacherCounselor;
import cc.mrbird.febs.system.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Prock.Liy
 */
public interface TeacherCounselorMapper extends BaseMapper<TeacherCounselor> {

    /**
     * 查找师咨列表
     *
     * @return List<TeacherCounselor>
     */
    List<TeacherCounselor> findTeacherCounselorList();
}
