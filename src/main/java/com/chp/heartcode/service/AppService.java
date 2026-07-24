package com.chp.heartcode.service;

import com.chp.heartcode.model.dto.app.AppQueryRequest;
import com.chp.heartcode.model.entity.User;
import com.chp.heartcode.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.chp.heartcode.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author CHP
 */
public interface AppService extends IService<App> {

    /**
     * 查询 App 关联信息的方法
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造查询对象的方法
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 转换为 AppVO 的方法
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 聊天生成代码
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);
}
