package the.flash;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

public class NettyServer {

    private static final int BEGIN_PORT = 8000;

    public static void main(String[] args) {
        // 这是一个管理线程的对象
        NioEventLoopGroup boosGroup = new NioEventLoopGroup();
        // 这是所有的业务线程的group
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        final AttributeKey<Object> clientKey = AttributeKey.newInstance("clientKey");
        serverBootstrap
                .group(boosGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        super.channelActive(ctx);
                    }
                })
                .attr(AttributeKey.newInstance("serverName"), "nettyServer")
                // 以给每一条连接指定自定义属性，然后后续我们可以通过channel.attr()取出该属性。
                .childAttr(clientKey, "clientValue")
                // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度，如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                .option(ChannelOption.SO_BACKLOG, 1024)
                // childOption()可以给每条连接设置一些TCP底层相关的属性
                // 表示是否开启TCP底层心跳机制，true为开启
                // 心跳机制就是客户端每隔几分钟就发送简单的信息给服务端，使其不至于长时间没有收到信息而断开连接
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 表示是否开启Nagle算法
                /**
                 * Nagle的算法通常会在TCP程序里添加两行代码，在未确认数据发送的时候让发送器把数据送到缓存里。
                 * 任何数据随后继续直到得到明显的数据确认或者直到攒到了一定数量的数据了再发包
                 *
                 * 不然每一次有数据就要发送，会造成 数据量小，但是报文的头部大，浪费资源的情况
                 */
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) {
                        // 业务逻辑的处理
                        System.out.println(ch.attr(clientKey).get());
                    }
                });


        bind(serverBootstrap, BEGIN_PORT);
    }

    private static void bind(final ServerBootstrap serverBootstrap, final int port) {
        serverBootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("端口[" + port + "]绑定成功!");
            } else {
                System.err.println("端口[" + port + "]绑定失败!");
                bind(serverBootstrap, port + 1);
            }
        });
    }
}
