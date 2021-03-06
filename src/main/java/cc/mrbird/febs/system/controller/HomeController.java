package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.annotation.ControllerEndpoint;
import cc.mrbird.febs.common.entity.FebsResponse;
import cc.mrbird.febs.system.service.ITeacherCounselorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Prock.Liy
 * @Date 2021/3/13 20:54
 * @Descripttion  首页Api
 * @Version 1.0
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "home")
public class HomeController {

    @Resource
    private ITeacherCounselorService iTeacherCounselorService;

    @PostMapping("homePage")
    @ControllerEndpoint(operation = "请求出错", exceptionMessage = "请求资源出错")
    @ResponseBody
    public FebsResponse home() {
        return new FebsResponse().success().data(iTeacherCounselorService.homePage());
    }
}
