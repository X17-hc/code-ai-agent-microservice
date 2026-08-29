package com.hechang.codeagent.innerservice;

import com.hechang.codeagent.exception.BusinessException;
import com.hechang.codeagent.exception.ErrorCode;
import com.hechang.codeagent.model.entity.User;
import com.hechang.codeagent.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.hechang.codeagent.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户相关的内部接口
 */
public interface InnerUserService {

    /**
     * 由于 HttpServletRequest 对象不好在网络中传递，因此采用静态方法，避免跨服务调用
     * 不能使用默认方法，否则 Dubbo 会尝试序列化，导致报错
     * @param request 请求
     * @return 登录用户
     */
    static User getLoginUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);
}
