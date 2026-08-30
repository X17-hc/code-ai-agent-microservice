package com.hechang.codeagent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 已部署应用的公开访问配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.deploy")
public class DeploymentConfig {

    /**
     * 已部署应用静态资源入口，例如 http://localhost:8125/api/static/deployments。
     */
    private String publicBaseUrl = "http://localhost:8125/api/static/deployments";

    public String buildDeploymentUrl(String deployKey) {
        return publicBaseUrl.replaceAll("/+$", "") + "/" + deployKey + "/";
    }
}
