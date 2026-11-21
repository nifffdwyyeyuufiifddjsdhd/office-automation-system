package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.vo.ImportResultVO;
import com.officeautomationsystemapi.vo.OrgNodeVO;
import com.officeautomationsystemapi.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<Users> {

    // 按用户名查用户，登录和注册校验都用它
    Users findUserByUserName(String username);

    boolean changePassword(Long userId, String oldPwd, String newPwd);

    // 登录，用户名或密码不对就抛异常
    Users login(String username, String rawPassword, String ip);

    void unlock(Long userId);

    // 退出登录，记录最后退出时间
    void logout(Long userId);

    // 用户列表按调用者的数据范围过滤，orgScope 为 null 不限
    Page<UserVO> pageUsers(Integer pageNum, Integer pageSize, String keyword, Integer status, Long orgId, List<Long> orgScope);
    UserVO getUserDetail(Long userId);
    UserVO getMyProfile(Long userId);                       // 个人中心完整资料
    void updateMyProfile(Long userId, UserVO vo);           // 本人只能改姓名性别手机邮箱头像
    Long createUserByAdmin(UserVO vo);

    // 入职办理：没给初始密码就生成一个，返回账号和明文密码
    Map<String, Object> onboard(UserVO vo);
    void updateUserByAdmin(UserVO vo);
    void deleteUserByAdmin(Long operatorId, Long userId);
    void resetPasswordByAdmin(Long userId, String newPassword);
    // force=true 时忽略待办提醒，直接停用
    void changeUserStatus(Long operatorId, Long userId, Integer status, boolean force);
    List<Roles> listEnabledRoles();
    List<OrgNodeVO> orgTree(List<Long> visibleOrgIds);

    // 批量导入用户，支持 .xlsx 和 .xls
    ImportResultVO importUsers(MultipartFile file);

    // 导出用户列表，手机号脱敏，最多一万条，写操作日志
    void exportUsers(Long operatorId, String ip, String userAgent,
                     String keyword, Integer status, Long orgId,
                     HttpServletResponse response);
}
