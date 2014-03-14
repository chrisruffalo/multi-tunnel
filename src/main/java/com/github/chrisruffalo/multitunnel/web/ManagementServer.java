package com.github.chrisruffalo.multitunnel.web;

import io.netty.channel.EventLoopGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.chrisruffalo.multitunnel.options.Options;
import com.github.chrisruffalo.multitunnel.tunnel.TunnelManager;
import com.github.chrisruffalo.multitunnel.web.rest.ManagementApplication;

public class ManagementServer {

	private final String managementInterface;
	
	private final int managementPort;
	
	private Server server;
	
	private Logger logger;
	
	public ManagementServer(TunnelManager manager, EventLoopGroup eventGroup, Options options) {
		this.managementInterface = options.getManagementInterface();
		this.managementPort = options.getManagementPort();
		
		this.logger = LoggerFactory.getLogger("management [" + this.managementInterface + ":" + this.managementPort + "]");
	}

	public void start() {
		InetSocketAddress addr = new InetSocketAddress(this.managementInterface, this.managementPort);
		final Server _server = new Server(addr);
		
		// create static resource handler
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(false);
		resourceHandler.setWelcomeFiles(new String[]{"index.html"});
		resourceHandler.setBaseResource(Resource.newClassPathResource("/web/"));
				
		// create context handler for java/servlet resources at "/services"
	    final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    contextHandler.setContextPath("/services");

	    // set prefixes
	    contextHandler.setInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,"/");
	    contextHandler.setInitParameter(ResteasyContextParameters.RESTEASY_SCAN,"true");

	    final ServletHolder restEasyServletHolder = new ServletHolder(new HttpServletDispatcher());
	    restEasyServletHolder.setInitOrder(1);

	    // set up bootstrap servlet application
	    restEasyServletHolder.setInitParameter("javax.ws.rs.Application", ManagementApplication.class.getName());
	    restEasyServletHolder.setInitParameter(ResteasyContextParameters.RESTEASY_SCAN,"true");

	    // set rest servlet handler to /* pattern
	    contextHandler.addServlet(restEasyServletHolder, "/*");

	    // create handler list
	    final HandlerList handlers = new HandlerList();
	    handlers.setHandlers(new Handler[] { 
	    	resourceHandler,
	    	contextHandler 
	    });
	    
	    // add handlers to server
	    _server.setHandler(handlers);
		
		// block until started
		final CountDownLatch latch = new CountDownLatch(1);
		
		// await server start
		try {
			_server.addLifeCycleListener(new DefaultJettyLifecycleListener(){
				@Override
				public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
					// just release latch
					latch.countDown();
					// show error
					logger.error("management server failed to start: {}", arg1.getMessage());
				}

				@Override
				public void lifeCycleStarted(LifeCycle arg0) {
					// save for later
					server = _server;
					// release latch
					latch.countDown();
					// log
					logger.info("started managment server interface");
				}				
			});
			_server.start();			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		try {
			latch.await();
		} catch (InterruptedException e) {
			if(this.server != null && this.server.isStarted()) {
				try {
					this.server.stop();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
}
