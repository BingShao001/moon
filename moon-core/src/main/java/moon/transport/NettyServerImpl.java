package moon.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import moon.common.URL;
import moon.common.URLParam;
import moon.core.DefaultRequest;
import moon.core.DefaultResponse;
import moon.exception.RpcFrameworkException;
import moon.rpc.MessageRouter;
import moon.rpc.RpcContext;
import moon.util.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ${DESCRIPTION}
 * netty4
 * @author Ricky Fung
 */
public class NettyServerImpl extends AbstractServer {
    //只处理连接的group线程
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //处理work业务的group线程 默认实际 cpu核数 * 2  最佳线程数=CPU核数*[1+(I/O耗时/CPU耗时)]
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    //引导类
    private ServerBootstrap serverBootstrap = new ServerBootstrap();
    //业务处理线程池
    private ThreadPoolExecutor pool;

    private MessageRouter router;

    private volatile boolean initializing = false;
    private static Map<String,ThreadPoolExecutor> executors = new ConcurrentHashMap<>();
    public NettyServerImpl(URL url, MessageRouter router) {
        super(url);

        this.localAddress = new InetSocketAddress(url.getPort());
        this.router = router;
        //业务线程资源全局唯一，默认200
        ThreadPoolExecutor threadPoolExecutor = executors.get(url.getHost());
        if(null == threadPoolExecutor){
            pool = new ThreadPoolExecutor(url.getIntParameter(URLParam.minWorkerThread.getName(), URLParam.minWorkerThread.getIntValue()),
                    url.getIntParameter(URLParam.maxWorkerThread.getName(), URLParam.maxWorkerThread.getIntValue()),
                    120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    //自定义线程名
                    new DefaultThreadFactory(String.format("%s-%s", Constants.FRAMEWORK_NAME, "biz")));
            executors.put(url.getHost(),pool);
            return;
        }
        pool = threadPoolExecutor;
    }

    @Override
    public synchronized boolean open() {
        if (initializing) {
            logger.warn("NettyServer ServerChannel is initializing: url=" + url);
            return true;
        }
        initializing = true;

        if (state.isAvailable()) {
            logger.warn("NettyServer ServerChannel has initialized: url=" + url);
            return true;
        }
        // 最大响应包限制
        final int maxContentLength = url.getIntParameter(URLParam.maxContentLength.getName(),
                URLParam.maxContentLength.getIntValue());

        this.serverBootstrap.group(bossGroup, workerGroup)
                //用到NIO，绑定通道NioServerSocketChannel
                .channel(NioServerSocketChannel.class)
                // 设置一个线程队列等待连接的个数
                .option(ChannelOption.SO_BACKLOG, 128)
                //通讯的TCP优化参数
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_RCVBUF, url.getIntParameter(URLParam.bufferSize.getName(), URLParam.bufferSize.getIntValue()))
                .childOption(ChannelOption.SO_SNDBUF, url.getIntParameter(URLParam.bufferSize.getName(), URLParam.bufferSize.getIntValue()))
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws IOException {

                        ch.pipeline().addLast(
                                new NettyDecoder(codec, url, maxContentLength, Constants.HEADER_SIZE, 4), //
                                new NettyEncoder(codec, url), //
                                new NettyServerHandler());
                    }
                });

        try {
            ChannelFuture channelFuture = this.serverBootstrap.bind(this.localAddress).sync();

            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {

                    if (f.isSuccess()) {
                        logger.info("Rpc Server bind port:{} success", url.getPort());
                    } else {
                        logger.error("Rpc Server bind port:{} failure", url.getPort());
                    }
                }
            });
        } catch (InterruptedException e) {
            logger.error(String.format("NettyServer bind to address:%s failure", this.localAddress), e);
            throw new RpcFrameworkException(String.format("NettyClient connect to address:%s failure", this.localAddress), e);
        }
        state = ChannelState.AVAILABLE;
        return true;
    }

    @Override
    public boolean isAvailable() {
        return state.isAvailable();
    }

    @Override
    public boolean isClosed() {
        return state.isClosed();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {

        if (state.isClosed()) {
            logger.info("NettyServer close fail: already close, url={}", url.getUri());
            return;
        }

        try {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
            this.pool.shutdown();

            state = ChannelState.CLOSED;
        } catch (Exception e) {
            logger.error("NettyServer close Error: url=" + url.getUri(), e);
        }
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<DefaultRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext context, DefaultRequest request) throws Exception {

            logger.info("Rpc server receive request id:{}", request.getRequestId());
            //处理请求
            processRpcRequest(context, request);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("NettyServerHandler exceptionCaught: remote=" + ctx.channel().remoteAddress()
                    + " local=" + ctx.channel().localAddress(), cause);
            ctx.channel().close();
        }
    }

    /**
     * 处理客户端请求
     **/
    private void processRpcRequest(final ChannelHandlerContext context, final DefaultRequest request) {
        final long processStartTime = System.currentTimeMillis();
        try {
            //上面声明的业务线程池
            this.pool.execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        //转成上下文
                        RpcContext.init(request);
                        processRpcRequest(context, request, processStartTime);
                    } finally {
                        RpcContext.destroy();
                    }

                }
            });
        } catch (RejectedExecutionException e) {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setException(new RpcFrameworkException("process thread pool is full, reject"));
            response.setProcessTime(System.currentTimeMillis() - processStartTime);
            context.channel().write(response);
        }

    }

    private void processRpcRequest(ChannelHandlerContext context, DefaultRequest request, long processStartTime) {
        //反射调用实现方法
        DefaultResponse response = (DefaultResponse) this.router.handle(request);
        response.setProcessTime(System.currentTimeMillis() - processStartTime);
        //非单向调用
        if (request.getType() != Constants.REQUEST_ONEWAY) {
            context.writeAndFlush(response);
        }
        logger.info("Rpc server process request:{} end...", request.getRequestId());
    }
}
