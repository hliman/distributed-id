package com.geega.bsc.id.server;

import com.geega.bsc.id.common.exception.DistributedIdException;
import com.geega.bsc.id.common.sync.Sync;
import com.geega.bsc.id.common.utils.ResourcesUtil;
import com.geega.bsc.id.common.utils.SnowFlake;
import com.geega.bsc.id.server.config.ConfigConst;
import com.geega.bsc.id.server.config.ServerConfig;
import com.geega.bsc.id.server.network.ServerAcceptor;
import com.geega.bsc.id.server.network.ServerRequestCache;
import com.geega.bsc.id.server.network.ServerRequestHandler;
import com.geega.bsc.id.server.zk.ZkServer;
import lombok.extern.slf4j.Slf4j;
import java.util.Properties;

/**
 * @author Jun.An3
 * @date 2022/07/21
 */
@Slf4j
public class Server {

    public static void main(String[] args) throws Exception {

        //同步组件
        Sync sync = new Sync();

        //读取配置文件
        ServerConfig serverConfig = getServerConfig();

        //向zk注册，并定时心跳
        ZkServer zkServer = new ZkServer(serverConfig);

        //网络包处理缓冲器
        ServerRequestCache requestChannel = new ServerRequestCache();

        //连接接收器
        ServerAcceptor acceptor = new ServerAcceptor(requestChannel, serverConfig);
        acceptor.start();

        //请求处理器
        ServerRequestHandler requestHandler = new ServerRequestHandler(requestChannel, new SnowFlake(serverConfig.getIdDatacenter(), zkServer.getWorkId()));
        requestHandler.start();

        //hold住
        sync.waitShutdown();
    }

    private static ServerConfig getServerConfig() {
        try {
            ServerConfig serverConfig = new ServerConfig();

            Properties properties = ResourcesUtil.getProperties("/application.properties");

            assert properties.getProperty(ConfigConst.BIND_IP) != null;
            assert properties.getProperty(ConfigConst.BIND_PORT) != null;

            serverConfig.setIp(properties.getProperty(ConfigConst.BIND_IP));
            serverConfig.setPort(Integer.valueOf(properties.getProperty(ConfigConst.BIND_PORT)));
            serverConfig.setProcessor(Integer.valueOf(properties.getProperty(ConfigConst.NIO_PROCESSOR, "3")));
            serverConfig.setIdDatacenter(Long.valueOf(properties.getProperty(ConfigConst.ID_DATACENTER, "1")));
            serverConfig.setZkConnection(properties.getProperty(ConfigConst.ZK_CONNECTION, "127.0.0.1:2181"));
            serverConfig.setZkNamespace(properties.getProperty(ConfigConst.ZK_NAMESPACE, "default"));
            serverConfig.setZkConnectionTimeoutMs(Integer.valueOf(properties.getProperty(ConfigConst.ZK_CONNECTION_TIMEOUT_MS, "10000")));
            serverConfig.setZkSessionTimeoutMs(Integer.valueOf(properties.getProperty(ConfigConst.ZK_SESSION_TIMEOUT_MS, "10000")));

            return serverConfig;
        } catch (Exception e) {
            throw new DistributedIdException("读取配置文件异常", e);
        }
    }

}
