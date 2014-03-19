package com.github.chrisruffalo.multitunnel;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.github.chrisruffalo.multitunnel.model.configuration.MultiTunnelConfiguration;
import com.github.chrisruffalo.multitunnel.model.tunnel.TunnelConfiguration;
import com.github.chrisruffalo.multitunnel.options.Options;
import com.github.chrisruffalo.multitunnel.tunnel.TunnelManager;
import com.github.chrisruffalo.multitunnel.util.InterfaceHelper;
import com.github.chrisruffalo.multitunnel.util.MultiTunnelProperties;
import com.github.chrisruffalo.multitunnel.web.ManagementServer;

public class Main {

	public static void main(String[] args) {
		
		// create commander and add converter
		Options options = new Options();
		JCommander commander = new JCommander(options);
		commander.setAllowAbbreviatedOptions(true);
		commander.setProgramName(MultiTunnelProperties.INSTANCE.title() + " v" + MultiTunnelProperties.INSTANCE.version());
	
		// parse
		commander.parse(args);
		
		// do help stuff
		if(options.isHelp()) {
			// print usage
			commander.usage();
			// done
			return;
		}
		
		// start logging
		Logger logger = LoggerFactory.getLogger("main");
		
		// do example print out
		if(options.isCreateExample()) {
			File example = new File("./multi-tunnel.configuration.example");
			if(example.exists()) {
				example.delete();
				try {
					example.createNewFile();
				} catch (IOException e) {
					System.out.println("Could not create example file: " + e.getMessage());
					return;
				}
			}
			
			// output stream
			try (FileOutputStream output = new FileOutputStream(example)) {
				MultiTunnelConfiguration exampleConfiguration = new MultiTunnelConfiguration();
				exampleConfiguration.write(output);
				System.out.println("Example file written to: " + example.getAbsolutePath());
				
				output.flush();
			} catch (FileNotFoundException e) {
				logger.error("Could not create example file: " + e.getMessage(), e);
				return;
			} catch (IOException e) {
				logger.error("Error while creating example file: " + e.getMessage(), e);
				return;
			} 
			
			// quit after creating example
			return;
		}
		
		// look for configuration file
		File configFile = options.getConfigurationFile();
		InputStream input = null;
		if(configFile.exists() && configFile.isFile()) {
			try {
				input = new FileInputStream(configFile);
				logger.info("Using configuration file: {}", configFile.getAbsolutePath());
			} catch (FileNotFoundException e) {
				input = Thread.currentThread().getContextClassLoader().getResourceAsStream("default.multi-tunnel.configuration");
				logger.error("Could not load configuration file, using defaults");
			}
		} else {
			input = Thread.currentThread().getContextClassLoader().getResourceAsStream("default.multi-tunnel.configuration");
			logger.info("Using default configuration file");
		}
		
		// load multi-tunnel configuration file (if available) or load defaults from
		// classpath
		MultiTunnelConfiguration config = MultiTunnelConfiguration.read(input, logger);
		
		// set netty stuff and various environment things
		ResourceLeakDetector.setLevel(Level.DISABLED);		
		
		// otherwise execute
		List<TunnelConfiguration> configurations = options.getTunnels();
				
		// calculate threads
		int workers = config.getWorkers();
		if(workers < 1) {
			workers = 1;
		}
		EventLoopGroup eventGroup = new NioEventLoopGroup(workers);
		logger.info("Using {} workers", workers);
		
		// create tunnel manager
		TunnelManager manager = new TunnelManager(eventGroup);

		// init interface helper since it can be slow
		InterfaceHelper.INSTANCE.init();
		
		// only start if some instances exist
		if(configurations != null && !configurations.isEmpty()) {
			// filter null items
			Iterator<TunnelConfiguration> filter = configurations.iterator();
			while(filter.hasNext()) {
				TunnelConfiguration item = filter.next();
				if(item == null) {
					filter.remove();
				}
			}
						
			// start servers
			logger.info("Starting ({}) pre-configured tunnels...", configurations.size());
			for(TunnelConfiguration configuration : configurations) {
				// generate name for "auto" created tunnel
				String name = String.format("auto-%s:%d->%s:%d", 
												configuration.getSourceInterface(), 
												configuration.getSourcePort(), 
												configuration.getDestHost(), 
												configuration.getDestPort()
											);
				configuration.setName(name);
				
				manager.create(configuration);
			}
		} else {
			// empty configuration list
			configurations = new LinkedList<>();
		}
		
		ManagementServer server = new ManagementServer(manager, eventGroup, config);
		server.start();
			
		// and wait
		while(true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
