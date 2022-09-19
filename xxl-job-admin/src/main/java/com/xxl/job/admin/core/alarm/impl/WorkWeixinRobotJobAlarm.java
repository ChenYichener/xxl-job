package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 企业微信机器人提醒
 *
 * @author ChenYichen
 */
@Component
public class WorkWeixinRobotJobAlarm implements JobAlarm {

    private static final Logger LOG = LoggerFactory.getLogger(WorkWeixinRobotJobAlarm.class);

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        if (info == null || info.getAlarmEmail() == null || info.getAlarmEmail().trim().length() <= 0) {
            LOG.info("跳过企业微信机器人提醒，当前");
            return Boolean.TRUE;
        }

        if (!info.getAlarmEmail().contains("robot=")) {
            LOG.info("跳过企业微信机器人提醒，当前alarm email = {}", info.getAlarmEmail());
            return Boolean.TRUE;
        }

        String webHookUrl = info.getAlarmEmail().substring(info.getAlarmEmail().indexOf("=") + 1);
        LOG.info("企业微信机器人提醒，当前webHookUrl = {}， 执行器名称:{}", webHookUrl, info.getExecutorHandler());

        String jsonData = getDataTemplate(buildContent(info, jobLog));
        HttpEntity<String> httpEntity = new HttpEntity<>(jsonData, getJsonHeader());

        try {
            ResponseEntity<String> responseEntity = new RestTemplate().postForEntity(webHookUrl, httpEntity, String.class);
            String responseContent = responseEntity.getBody();
            LOG.info("发送通知到企业微信群返回: {}", responseContent);

        } catch (Exception e) {
            LOG.error("发送通知到企业微信群发生异常:", e);
        }
        return Boolean.TRUE;
    }

    public String buildContent(XxlJobInfo info, XxlJobLog jobLog) {
        return "#### <font size=18 color=red>【定时器执行异常】</font> " +
                "\n\n > <font color=red>任务名称 : " + info.getJobDesc() + "</font>" +
                "\n > <font color=red>执行器名称 : " + info.getExecutorHandler() + "</font>" +
                "\n > <font color=red>负责人 : " + info.getAuthor() + "</font>" +
                "\n > 执行器ip : " + jobLog.getExecutorAddress() + "" +
                "\n > 任务参数 : " + jobLog.getExecutorParam() + "" +
                "\n > TriggerMsg : " + jobLog.getTriggerMsg().replace("<br>", "\n") + "" +
                "\n > HandleCode : " + jobLog.getHandleMsg() + "" +
                "\n > 报警时间 : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +
                "\n > <font  size=17 color=blue><@" + info.getAuthor() + ">, 请立即定位和解决异常问题，收到请回复。</font>";
    }

    public String getDataTemplate(String content) {
        Map<String, Object> dataMap = new HashMap<>(16);
        dataMap.put("msgtype", "markdown");

        List<String> mobileList = new ArrayList<>(10);
        // 通知人
        Map<String, Object> atMap = new HashMap<>(16);
        // 1.是否通知所有人
        atMap.put("isAtAll", Boolean.TRUE);
        // 2.通知具体人的手机号码列表
        atMap.put("atMobiles", mobileList);

        Map<String, Object> markdown = new HashMap<>(16);
        markdown.put("title", "XXL-JOB异常提醒：");
        markdown.put("content", content);
        dataMap.put("markdown", markdown);
        dataMap.put("at", atMap);
        return JacksonUtil.writeValueAsString(dataMap);
    }

    private HttpHeaders getJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "*/*");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        return headers;
    }

}

